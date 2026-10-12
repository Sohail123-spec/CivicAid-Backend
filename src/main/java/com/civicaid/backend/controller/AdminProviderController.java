package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderCreateRequest;
import com.civicaid.backend.dto.ProviderResponse;
import com.civicaid.backend.dto.ProviderUpdateRequest;
import com.civicaid.backend.dto.ProviderVerificationRecordResponse;
import com.civicaid.backend.dto.ProviderVerificationRequest;
import com.civicaid.backend.service.ProviderService;
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
@RequestMapping("/api/admin/providers")
public class AdminProviderController {

    private final ProviderService providerService;

    public AdminProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> createProvider(
            @Valid @RequestBody ProviderCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createProvider(
                        request,
                        authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> getAllProviders() {

        return ResponseEntity.ok(
                providerService.getAllProvidersForAdmin());
    }

    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderResponse> getProvider(
            @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerService.getProviderForAdmin(providerId));
    }

    @PutMapping("/{providerId}")
    public ResponseEntity<ProviderResponse> updateProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerService.updateProvider(
                        providerId,
                        request,
                        authentication.getName()));
    }

    @PostMapping("/{providerId}/verifications")
    public ResponseEntity<ProviderVerificationRecordResponse>
            verifyProvider(
                    @PathVariable UUID providerId,
                    @Valid @RequestBody ProviderVerificationRequest request,
                    Authentication authentication) {

        return ResponseEntity.ok(
                providerService.verifyProvider(
                        providerId,
                        request,
                        authentication.getName()));
    }

    @GetMapping("/{providerId}/verification-records")
    public ResponseEntity<List<ProviderVerificationRecordResponse>>
            getVerificationHistory(
                    @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerService.getVerificationHistory(providerId));
    }
}