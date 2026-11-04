package com.temple.vazhipadu.report.service;

import com.temple.vazhipadu.common.error.BadRequestException;
import com.temple.vazhipadu.common.util.TimeUtils;
import com.temple.vazhipadu.order.entity.Order;
import com.temple.vazhipadu.order.entity.OrderItem;
import com.temple.vazhipadu.order.entity.OrderStatus;
import com.temple.vazhipadu.order.repository.OrderRepository;
import com.temple.vazhipadu.report.dto.DailyCollectionReportResponse;
import com.temple.vazhipadu.report.dto.OfferingCollectionSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public DailyCollectionReportResponse generateDailyCollectionReport(Long templeId, LocalDate reportDate) {
        if (templeId == null) {
            throw new BadRequestException("templeId query parameter is required for report generation");
        }
        if (reportDate == null) {
            throw new BadRequestException("date query parameter is required for report generation");
        }

        // Days are counted in Indian Standard Time (IST)
        Instant startOfIstDay = TimeUtils.getStartOfDayInIst(reportDate);
        Instant endOfIstDay = TimeUtils.getEndOfDayInIst(reportDate);

        // Fetch only CONFIRMED orders created within that IST day
        List<Order> confirmedOrders = orderRepository.findByTempleIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                templeId,
                OrderStatus.CONFIRMED,
                startOfIstDay,
                endOfIstDay
        );

        Map<Long, OfferingCollectionSummary> offeringMap = new LinkedHashMap<>();
        BigDecimal calculatedGrandTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Order order : confirmedOrders) {
            for (OrderItem item : order.getItems()) {
                OfferingCollectionSummary summary = offeringMap.computeIfAbsent(
                        item.getOfferingId(),
                        id -> new OfferingCollectionSummary(
                                item.getOfferingId(),
                                item.getOfferingName(),
                                0,
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        )
                );
                summary.setQuantity(summary.getQuantity() + item.getQuantity());
                summary.setTotalAmount(summary.getTotalAmount().add(item.getSubtotal()));
            }
        }

        List<OfferingCollectionSummary> offeringSummaries = offeringMap.values().stream()
                .sorted(Comparator.comparing(OfferingCollectionSummary::getOfferingId))
                .toList();

        for (OfferingCollectionSummary summary : offeringSummaries) {
            calculatedGrandTotal = calculatedGrandTotal.add(summary.getTotalAmount());
        }
        calculatedGrandTotal = calculatedGrandTotal.setScale(2, RoundingMode.HALF_UP);

        return new DailyCollectionReportResponse(
                templeId,
                reportDate,
                confirmedOrders.size(),
                calculatedGrandTotal,
                offeringSummaries
        );
    }
}
