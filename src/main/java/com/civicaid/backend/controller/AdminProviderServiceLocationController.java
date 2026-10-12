package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceLocationCreateRequest;
import com.civicaid.backend.dto.ProviderServiceLocationResponse;
import com.civicaid.backend.dto.ProviderServiceLocationUpdateRequest;
import com.civicaid.backend.dto.ProviderServiceLocationVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.service.ProviderServiceLocationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/provider-service-locations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderServiceLocationController {

    private final ProviderServiceLocationService providerServiceLocationService;

    public AdminProviderServiceLocationController(
            ProviderServiceLocationService providerServiceLocationService) {

        this.providerServiceLocationService =
                providerServiceLocationService;
    }

    @PostMapping("/services/{providerServiceId}")
    public ResponseEntity<ProviderServiceLocationResponse> createServiceLocation(
            @PathVariable UUID providerServiceId,
            @Valid @RequestBody ProviderServiceLocationCreateRequest request,
            org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerServiceLocationService.createServiceLocation(
                        providerServiceId,
                        request,
                        authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceLocationResponse>>
            getServiceLocations() {

        return ResponseEntity.ok(
                providerServiceLocationService.getServiceLocationsForAdmin());
    }

    @GetMapping("/services/{providerServiceId}")
    public ResponseEntity<List<ProviderServiceLocationResponse>>
            getServiceLocationsByService(
                    @PathVariable UUID providerServiceId) {

        return ResponseEntity.ok(
                providerServiceLocationService
                        .getServiceLocationsByServiceForAdmin(
                                providerServiceId));
    }

    @GetMapping("/locations/{providerLocationId}")
    public ResponseEntity<List<ProviderServiceLocationResponse>>
            getServiceLocationsByLocation(
                    @PathVariable UUID providerLocationId) {

        return ResponseEntity.ok(
                providerServiceLocationService
                        .getServiceLocationsByLocationForAdmin(
                                providerLocationId));
    }

    @GetMapping("/{serviceLocationId}")
    public ResponseEntity<ProviderServiceLocationResponse>
            getServiceLocation(
                    @PathVariable UUID serviceLocationId) {

        return ResponseEntity.ok(
                providerServiceLocationService
                        .getServiceLocationForAdmin(serviceLocationId));
    }

    @PutMapping("/{serviceLocationId}")
    public ResponseEntity<ProviderServiceLocationResponse>
            updateServiceLocation(
                    @PathVariable UUID serviceLocationId,
                    @Valid @RequestBody ProviderServiceLocationUpdateRequest request,
                    org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceLocationService.updateServiceLocation(
                        serviceLocationId,
                        request,
                        authentication.getName()));
    }

    @PatchMapping("/{serviceLocationId}/archive")
    public ResponseEntity<ProviderServiceLocationResponse>
            archiveServiceLocation(
                    @PathVariable UUID serviceLocationId,
                    org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceLocationService.setServiceLocationActive(
                        serviceLocationId,
                        false,
                        authentication.getName()));
    }

    @PatchMapping("/{serviceLocationId}/activate")
    public ResponseEntity<ProviderServiceLocationResponse>
            activateServiceLocation(
                    @PathVariable UUID serviceLocationId,
                    org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceLocationService.setServiceLocationActive(
                        serviceLocationId,
                        true,
                        authentication.getName()));
    }

    @PostMapping("/{serviceLocationId}/verifications")
    public ResponseEntity<ProviderServiceLocationResponse>
            verifyServiceLocation(
                    @PathVariable UUID serviceLocationId,
                    @Valid @RequestBody ProviderServiceLocationVerificationRequest request,
                    org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceLocationService.verifyServiceLocation(
                        serviceLocationId,
                        request,
                        authentication.getName()));
    }

    @GetMapping("/{serviceLocationId}/verification-records")
    public ResponseEntity<List<VerificationRecordResponse>>
            getVerificationHistory(
                    @PathVariable UUID serviceLocationId) {

        return ResponseEntity.ok(
                providerServiceLocationService
                        .getVerificationHistory(serviceLocationId));
    }
}