package com.temple.vazhipadu.offering.repository;

import com.temple.vazhipadu.offering.entity.DailyOfferingQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyOfferingQuotaRepository extends JpaRepository<DailyOfferingQuota, Long> {
    Optional<DailyOfferingQuota> findByOfferingIdAndBookingDate(Long offeringId, LocalDate bookingDate);
}
