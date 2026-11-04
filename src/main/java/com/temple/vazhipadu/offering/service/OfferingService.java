package com.temple.vazhipadu.offering.service;

import com.temple.vazhipadu.common.error.BadRequestException;
import com.temple.vazhipadu.common.error.ResourceNotFoundException;
import com.temple.vazhipadu.offering.dto.CreateOfferingRequest;
import com.temple.vazhipadu.offering.dto.OfferingResponse;
import com.temple.vazhipadu.offering.dto.UpdateOfferingRequest;
import com.temple.vazhipadu.offering.entity.Offering;
import com.temple.vazhipadu.offering.repository.OfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class OfferingService {

    private final OfferingRepository offeringRepository;

    public OfferingService(OfferingRepository offeringRepository) {
        this.offeringRepository = offeringRepository;
    }

    public OfferingResponse createOffering(CreateOfferingRequest request) {
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BadRequestException("Offering price must be greater than zero");
        }
        Offering offering = new Offering(
                request.getTempleId(),
                request.getName().trim(),
                request.getPrice().setScale(2, RoundingMode.HALF_UP),
                request.getActive() == null ? true : request.getActive(),
                request.getDailyLimit()
        );
        Offering saved = offeringRepository.save(offering);
        return OfferingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public OfferingResponse getOfferingById(Long id) {
        Offering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + id));
        return OfferingResponse.fromEntity(offering);
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> listOfferings(Long templeId, Boolean activeOnly) {
        List<Offering> offerings;
        if (activeOnly != null && activeOnly) {
            offerings = offeringRepository.findByTempleIdAndActiveTrue(templeId);
        } else {
            offerings = offeringRepository.findByTempleId(templeId);
        }
        return offerings.stream().map(OfferingResponse::fromEntity).toList();
    }

    public OfferingResponse updateOffering(Long id, UpdateOfferingRequest request) {
        Offering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            offering.setName(request.getName().trim());
        }
        if (request.getPrice() != null) {
            if (request.getPrice().signum() <= 0) {
                throw new BadRequestException("Offering price must be greater than zero");
            }
            offering.setPrice(request.getPrice().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getActive() != null) {
            offering.setActive(request.getActive());
        }
        if (request.getDailyLimit() != null) {
            offering.setDailyLimit(request.getDailyLimit());
        }

        Offering updated = offeringRepository.save(offering);
        return OfferingResponse.fromEntity(updated);
    }
}
