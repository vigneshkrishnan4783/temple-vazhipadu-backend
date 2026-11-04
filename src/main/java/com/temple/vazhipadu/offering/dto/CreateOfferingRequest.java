package com.temple.vazhipadu.offering.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateOfferingRequest {

    @NotNull(message = "Temple ID is required")
    private Long templeId;

    @NotBlank(message = "Offering name is required")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    private Boolean active = true;

    @Min(value = 1, message = "Daily limit must be at least 1 if set")
    private Integer dailyLimit;

    public CreateOfferingRequest() {
    }

    public CreateOfferingRequest(Long templeId, String name, BigDecimal price, Boolean active, Integer dailyLimit) {
        this.templeId = templeId;
        this.name = name;
        this.price = price;
        this.active = active;
        this.dailyLimit = dailyLimit;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
}
