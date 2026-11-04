package com.temple.vazhipadu.report.dto;

import java.math.BigDecimal;

public class OfferingCollectionSummary {

    private Long offeringId;
    private String offeringName;
    private int quantity;
    private BigDecimal totalAmount;

    public OfferingCollectionSummary() {
    }

    public OfferingCollectionSummary(Long offeringId, String offeringName, int quantity, BigDecimal totalAmount) {
        this.offeringId = offeringId;
        this.offeringName = offeringName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public String getOfferingName() {
        return offeringName;
    }

    public void setOfferingName(String offeringName) {
        this.offeringName = offeringName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
