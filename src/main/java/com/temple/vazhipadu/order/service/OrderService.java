package com.temple.vazhipadu.order.service;

import com.temple.vazhipadu.common.error.BadRequestException;
import com.temple.vazhipadu.common.error.ResourceNotFoundException;
import com.temple.vazhipadu.offering.entity.Offering;
import com.temple.vazhipadu.offering.repository.OfferingRepository;
import com.temple.vazhipadu.order.dto.CreateOrderRequest;
import com.temple.vazhipadu.order.dto.OrderItemRequest;
import com.temple.vazhipadu.order.dto.OrderResponse;
import com.temple.vazhipadu.order.entity.Order;
import com.temple.vazhipadu.order.entity.OrderItem;
import com.temple.vazhipadu.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OfferingRepository offeringRepository;

    public OrderService(OrderRepository orderRepository, OfferingRepository offeringRepository) {
        this.orderRepository = orderRepository;
        this.offeringRepository = offeringRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one offering item");
        }

        // Validate uniqueness of offerings in the same order request
        java.util.Set<Long> seenOfferings = new java.util.HashSet<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getOfferingId() != null && !seenOfferings.add(itemReq.getOfferingId())) {
                throw new BadRequestException("Duplicate offering id " + itemReq.getOfferingId() + " in order. Please consolidate quantities.");
            }
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Order order = new Order(
                request.getTempleId(),
                request.getDevoteeName().trim(),
                request.getPhoneNumber().trim(),
                request.getNakshatra().trim(),
                request.getBookingDate(),
                BigDecimal.ZERO
        );

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for offering id: " + itemReq.getOfferingId());
            }

            Offering offering = offeringRepository.findById(itemReq.getOfferingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + itemReq.getOfferingId()));

            if (!offering.getTempleId().equals(request.getTempleId())) {
                throw new BadRequestException("Offering '" + offering.getName() + "' does not belong to temple id: " + request.getTempleId());
            }

            if (!offering.isActive()) {
                throw new BadRequestException("Offering '" + offering.getName() + "' is currently inactive and cannot be booked");
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
    public com.temple.vazhipadu.common.dto.PagedResponse<OrderResponse> searchOrders(
            Long templeId,
            java.time.LocalDate bookingDate,
            com.temple.vazhipadu.order.entity.OrderStatus status,
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
        org.springframework.data.domain.Sort stableSort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
        ).and(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "id"
        ));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, stableSort);
        org.springframework.data.jpa.domain.Specification<Order> spec =
                com.temple.vazhipadu.order.repository.OrderSpecification.searchOrders(templeId, bookingDate, status, phoneNumber);

        org.springframework.data.domain.Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return com.temple.vazhipadu.common.dto.PagedResponse.fromPage(orderPage.map(OrderResponse::fromEntity));
    }
}
