package com.temple.vazhipadu.offering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_offering_quotas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quota_offering_date",
                columnNames = {"offering_id", "booking_date"}
        )
)
public class DailyOfferingQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booked_count", nullable = false)
    private int bookedCount = 0;

    public DailyOfferingQuota() {
    }

    public DailyOfferingQuota(Long offeringId, LocalDate bookingDate, int bookedCount) {
        this.offeringId = offeringId;
        this.bookingDate = bookingDate;
        this.bookedCount = bookedCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public int getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(int bookedCount) {
        this.bookedCount = bookedCount;
    }
}
