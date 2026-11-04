package com.temple.vazhipadu.offering.controller;

import com.temple.vazhipadu.offering.dto.CreateOfferingRequest;
import com.temple.vazhipadu.offering.dto.OfferingResponse;
import com.temple.vazhipadu.offering.dto.UpdateOfferingRequest;
import com.temple.vazhipadu.offering.service.OfferingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offerings")
public class OfferingController {

    private final OfferingService offeringService;

    public OfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @PostMapping
    public ResponseEntity<OfferingResponse> createOffering(@Valid @RequestBody CreateOfferingRequest request) {
        OfferingResponse response = offeringService.createOffering(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferingResponse> getOfferingById(@PathVariable Long id) {
        OfferingResponse response = offeringService.getOfferingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OfferingResponse>> listOfferings(
            @RequestParam Long templeId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<OfferingResponse> responses = offeringService.listOfferings(templeId, activeOnly);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferingRequest request) {
        OfferingResponse response = offeringService.updateOffering(id, request);
        return ResponseEntity.ok(response);
    }
}
