package com.temple.vazhipadu.order.service;

import com.temple.vazhipadu.common.dto.PagedResponse;
import com.temple.vazhipadu.common.error.BadRequestException;
import com.temple.vazhipadu.common.error.ConflictException;
import com.temple.vazhipadu.common.error.ResourceNotFoundException;
import com.temple.vazhipadu.offering.entity.DailyOfferingQuota;
import com.temple.vazhipadu.offering.entity.Offering;
import com.temple.vazhipadu.offering.repository.DailyOfferingQuotaRepository;
import com.temple.vazhipadu.offering.repository.OfferingRepository;
import com.temple.vazhipadu.order.dto.CreateOrderRequest;
import com.temple.vazhipadu.order.dto.OrderItemRequest;
import com.temple.vazhipadu.order.dto.OrderResponse;
import com.temple.vazhipadu.order.entity.Order;
import com.temple.vazhipadu.order.entity.OrderItem;
import com.temple.vazhipadu.order.entity.OrderStatus;
import com.temple.vazhipadu.order.repository.OrderRepository;
import com.temple.vazhipadu.order.repository.OrderSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OfferingRepository offeringRepository;
    private final DailyOfferingQuotaRepository dailyOfferingQuotaRepository;

    public OrderService(OrderRepository orderRepository,
                        OfferingRepository offeringRepository,
                        DailyOfferingQuotaRepository dailyOfferingQuotaRepository) {
        this.orderRepository = orderRepository;
        this.offeringRepository = offeringRepository;
        this.dailyOfferingQuotaRepository = dailyOfferingQuotaRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one offering item");
        }

        // Validate uniqueness of offerings in the same order request
        Set<Long> seenOfferings = new HashSet<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getOfferingId() != null && !seenOfferings.add(itemReq.getOfferingId())) {
                throw new BadRequestException("Duplicate offering id " + itemReq.getOfferingId() + " in order. Please consolidate quantities.");
            }
        }

        // Deterministic lock acquisition order by offeringId ASC to eliminate deadlocks
        List<OrderItemRequest> sortedItems = new ArrayList<>(request.getItems());
        sortedItems.sort(Comparator.comparing(OrderItemRequest::getOfferingId));

        BigDecimal calculatedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Order order = new Order(
                request.getTempleId(),
                request.getDevoteeName().trim(),
                request.getPhoneNumber().trim(),
                request.getNakshatra().trim(),
                request.getBookingDate(),
                BigDecimal.ZERO
        );

        for (OrderItemRequest itemReq : sortedItems) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for offering id: " + itemReq.getOfferingId());
            }

            // Acquire pessimistic write lock on the offering row
            Offering offering = offeringRepository.findByIdForUpdate(itemReq.getOfferingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + itemReq.getOfferingId()));

            if (!offering.getTempleId().equals(request.getTempleId())) {
                throw new BadRequestException("Offering '" + offering.getName() + "' does not belong to temple id: " + request.getTempleId());
            }

            if (!offering.isActive()) {
                throw new BadRequestException("Offering '" + offering.getName() + "' is currently inactive and cannot be booked");
            }

            // Enforce daily limits atomically if configured
            if (offering.getDailyLimit() != null) {
                DailyOfferingQuota quota = dailyOfferingQuotaRepository
                        .findByOfferingIdAndBookingDate(offering.getId(), request.getBookingDate())
                        .orElseGet(() -> new DailyOfferingQuota(offering.getId(), request.getBookingDate(), 0));

                if (quota.getBookedCount() + itemReq.getQuantity() > offering.getDailyLimit()) {
                    String errorMsg = (quota.getBookedCount() >= offering.getDailyLimit())
                            ? String.format("Offering '%s' has already reached or exceeded its daily limit of %d for date %s (currently booked: %d)",
                            offering.getName(), offering.getDailyLimit(), request.getBookingDate(), quota.getBookedCount())
                            : String.format("Offering '%s' has exceeded its daily limit of %d for date %s (already booked: %d, requested: %d)",
                            offering.getName(), offering.getDailyLimit(), request.getBookingDate(), quota.getBookedCount(), itemReq.getQuantity());
                    throw new ConflictException(errorMsg);
                }

                quota.setBookedCount(quota.getBookedCount() + itemReq.getQuantity());
                dailyOfferingQuotaRepository.save(quota);
            }

            BigDecimal unitPrice = offering.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            calculatedTotal = calculatedTotal.add(itemSubtotal);

            OrderItem orderItem = new OrderItem(
                    order,
                    offering.getId(),
                    offering.getName(),
                    unitPrice,
                    itemReq.getQuantity(),
                    itemSubtotal
            );
            order.addItem(orderItem);
        }

        order.setTotalAmount(calculatedTotal);
        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> searchOrders(
            Long templeId,
            LocalDate bookingDate,
            OrderStatus status,
            String phoneNumber,
            int page,
            int size) {

        if (templeId == null) {
            throw new BadRequestException("templeId is required for order search");
        }
        if (page < 0) {
            throw new BadRequestException("Page number cannot be negative");
        }
        if (size <= 0) {
            size = 20;
        } else if (size > 100) {
            size = 100; // Cap to maximum 100 items per page to prevent OOM
        }

        // Stable ordering: primary by createdAt DESC, tie-breaker by id DESC
        Sort stableSort = Sort.by(
                Sort.Direction.DESC, "createdAt"
        ).and(Sort.by(
                Sort.Direction.DESC, "id"
        ));

        Pageable pageable = PageRequest.of(page, size, stableSort);
        Specification<Order> spec = OrderSpecification.searchOrders(templeId, bookingDate, status, phoneNumber);

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(orderPage.map(OrderResponse::fromEntity));
    }
}
