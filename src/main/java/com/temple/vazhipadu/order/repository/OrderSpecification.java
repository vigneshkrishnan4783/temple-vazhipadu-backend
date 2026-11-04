package com.temple.vazhipadu.order.repository;

import com.temple.vazhipadu.order.entity.Order;
import com.temple.vazhipadu.order.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> searchOrders(
            Long templeId,
            LocalDate bookingDate,
            OrderStatus status,
            String phoneNumber) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (templeId != null) {
                predicates.add(cb.equal(root.get("templeId"), templeId));
            }

            if (bookingDate != null) {
                predicates.add(cb.equal(root.get("bookingDate"), bookingDate));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (phoneNumber != null && !phoneNumber.isBlank()) {
                predicates.add(cb.equal(root.get("phoneNumber"), phoneNumber.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
