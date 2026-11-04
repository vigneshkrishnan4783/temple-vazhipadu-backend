package com.temple.vazhipadu.offering.dto;

import com.temple.vazhipadu.offering.entity.Offering;

import java.math.BigDecimal;
import java.time.Instant;

public class OfferingResponse {

    private Long id;
    private Long templeId;
    private String name;
    private BigDecimal price;
    private boolean active;
    private Integer dailyLimit;
    private Instant createdAt;

    public OfferingResponse() {
    }

    public OfferingResponse(Long id, Long templeId, String name, BigDecimal price, boolean active, Integer dailyLimit, Instant createdAt) {
        this.id = id;
        this.templeId = templeId;
        this.name = name;
        this.price = price;
        this.active = active;
        this.dailyLimit = dailyLimit;
        this.createdAt = createdAt;
    }

    public static OfferingResponse fromEntity(Offering offering) {
        return new OfferingResponse(
                offering.getId(),
                offering.getTempleId(),
                offering.getName(),
                offering.getPrice(),
                offering.isActive(),
                offering.getDailyLimit(),
                offering.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTempleId() {
        return templeId;
    }

    public void setTempleId(Long templeId) {
        this.templeId = templeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
