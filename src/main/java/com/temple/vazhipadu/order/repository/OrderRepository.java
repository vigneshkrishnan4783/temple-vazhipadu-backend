package com.temple.vazhipadu.order.repository;

import com.temple.vazhipadu.order.entity.Order;
import com.temple.vazhipadu.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findByTempleIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long templeId, OrderStatus status, Instant start, Instant end
    );
}
