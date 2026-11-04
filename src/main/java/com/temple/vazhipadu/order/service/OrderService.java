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
}
