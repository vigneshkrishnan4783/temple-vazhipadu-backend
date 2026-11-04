package com.temple.vazhipadu.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "Temple ID is required")
    private Long templeId;

    @NotBlank(message = "Devotee name is required")
    private String devoteeName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+ -]{7,15}$", message = "Phone number must be valid")
    private String phoneNumber;

    @NotBlank(message = "Nakshatra (birth star) is required")
    private String nakshatra;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotEmpty(message = "Order must contain at least one offering item")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(Long templeId, String devoteeName, String phoneNumber, String nakshatra, LocalDate bookingDate, List<OrderItemRequest> items) {
        this.templeId = templeId;
        this.devoteeName = devoteeName;
        this.phoneNumber = phoneNumber;
        this.nakshatra = nakshatra;
        this.bookingDate = bookingDate;
        this.items = items;
    }

    public Long getTempleId() {
        return templeId;
    }

    public void setTempleId(Long templeId) {
        this.templeId = templeId;
    }

    public String getDevoteeName() {
        return devoteeName;
    }

    public void setDevoteeName(String devoteeName) {
        this.devoteeName = devoteeName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNakshatra() {
        return nakshatra;
    }

    public void setNakshatra(String nakshatra) {
        this.nakshatra = nakshatra;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
