package com.temple.vazhipadu.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DailyCollectionReportResponse {

    private Long templeId;
    private LocalDate reportDate;
    private int totalOrders;
    private BigDecimal totalAmount;
    private List<OfferingCollectionSummary> offerings;

    public DailyCollectionReportResponse() {
    }

    public DailyCollectionReportResponse(Long templeId, LocalDate reportDate, int totalOrders, BigDecimal totalAmount, List<OfferingCollectionSummary> offerings) {
        this.templeId = templeId;
        this.reportDate = reportDate;
        this.totalOrders = totalOrders;
        this.totalAmount = totalAmount;
        this.offerings = offerings;
    }

    public Long getTempleId() {
        return templeId;
    }

    public void setTempleId(Long templeId) {
        this.templeId = templeId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OfferingCollectionSummary> getOfferings() {
        return offerings;
    }

    public void setOfferings(List<OfferingCollectionSummary> offerings) {
        this.offerings = offerings;
    }
}
