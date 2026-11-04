package com.temple.vazhipadu.offering.repository;

import com.temple.vazhipadu.offering.entity.Offering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferingRepository extends JpaRepository<Offering, Long> {
    List<Offering> findByTempleId(Long templeId);
    List<Offering> findByTempleIdAndActiveTrue(Long templeId);
}
