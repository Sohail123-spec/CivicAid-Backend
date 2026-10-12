package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceCreateRequest;
import com.civicaid.backend.dto.ProviderServiceResponse;
import com.civicaid.backend.dto.ProviderServiceUpdateRequest;
import com.civicaid.backend.dto.ProviderServiceVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.service.ProviderServiceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/provider-services")
public class AdminProviderServiceController {

    private final ProviderServiceService providerServiceService;

    public AdminProviderServiceController(
            ProviderServiceService providerServiceService) {

        this.providerServiceService = providerServiceService;
    }

    @PostMapping("/providers/{providerId}")
    public ResponseEntity<ProviderServiceResponse> createService(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderServiceCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerServiceService.createService(
                        providerId,
                        request,
                        authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceResponse>> getAdminServices() {

        return ResponseEntity.ok(
                providerServiceService.getAdminServices());
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<ProviderServiceResponse>> getServicesByProvider(
            @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerServiceService.getAdminServicesByProvider(
                        providerId));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ProviderServiceResponse> getAdminService(
            @PathVariable UUID serviceId) {

        return ResponseEntity.ok(
                providerServiceService.getAdminService(serviceId));
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ProviderServiceResponse> updateService(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ProviderServiceUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceService.updateService(
                        serviceId,
                        request,
                        authentication.getName()));
    }

    @PatchMapping("/{serviceId}/archive")
    public ResponseEntity<ProviderServiceResponse> archiveService(
            @PathVariable UUID serviceId,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceService.setServiceActive(
                        serviceId,
                        false,
                        authentication.getName()));
    }

    @PatchMapping("/{serviceId}/activate")
    public ResponseEntity<ProviderServiceResponse> activateService(
            @PathVariable UUID serviceId,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceService.setServiceActive(
                        serviceId,
                        true,
                        authentication.getName()));
    }

    @PostMapping("/{serviceId}/verifications")
    public ResponseEntity<ProviderServiceResponse> verifyService(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ProviderServiceVerificationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceService.verifyService(
                        serviceId,
                        request,
                        authentication.getName()));
    }

    @GetMapping("/{serviceId}/verification-records")
    public ResponseEntity<List<VerificationRecordResponse>>
            getVerificationHistory(
                    @PathVariable UUID serviceId) {

        return ResponseEntity.ok(
                providerServiceService.getVerificationHistory(
                        serviceId));
    }
}