package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderLocationCreateRequest;
import com.civicaid.backend.dto.ProviderLocationResponse;
import com.civicaid.backend.dto.ProviderLocationUpdateRequest;
import com.civicaid.backend.dto.ProviderLocationVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.service.ProviderLocationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminProviderLocationController {

    private final ProviderLocationService providerLocationService;

    public AdminProviderLocationController(
            ProviderLocationService providerLocationService) {
        this.providerLocationService = providerLocationService;
    }

    @PostMapping("/providers/{providerId}/locations")
    public ResponseEntity<ProviderLocationResponse> createLocation(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderLocationCreateRequest request,
            Authentication authentication) {

        ProviderLocationResponse response =
                providerLocationService.createLocation(
                        providerId,
                        request,
                        authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/providers/{providerId}/locations")
    public ResponseEntity<List<ProviderLocationResponse>> getLocationsForProvider(
            @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerLocationService.getLocationsForProvider(providerId));
    }

    @GetMapping("/provider-locations")
    public ResponseEntity<List<ProviderLocationResponse>> getAllLocations() {
        return ResponseEntity.ok(
                providerLocationService.getAllLocationsForAdmin());
    }

    @GetMapping("/provider-locations/{locationId}")
    public ResponseEntity<ProviderLocationResponse> getLocation(
            @PathVariable UUID locationId) {

        return ResponseEntity.ok(
                providerLocationService.getLocationForAdmin(locationId));
    }

    @PutMapping("/provider-locations/{locationId}")
    public ResponseEntity<ProviderLocationResponse> updateLocation(
            @PathVariable UUID locationId,
            @Valid @RequestBody ProviderLocationUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerLocationService.updateLocation(
                        locationId,
                        request,
                        authentication.getName()));
    }

    @PostMapping("/provider-locations/{locationId}/verifications")
    public ResponseEntity<VerificationRecordResponse> verifyLocation(
            @PathVariable UUID locationId,
            @Valid @RequestBody ProviderLocationVerificationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerLocationService.verifyLocation(
                        locationId,
                        request,
                        authentication.getName()));
    }

    @GetMapping("/provider-locations/{locationId}/verification-records")
    public ResponseEntity<List<VerificationRecordResponse>> getVerificationHistory(
            @PathVariable UUID locationId) {

        return ResponseEntity.ok(
                providerLocationService.getVerificationHistory(locationId));
    }
}