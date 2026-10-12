package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderLocationResponse;
import com.civicaid.backend.service.ProviderLocationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/{providerId}/locations")
public class ProviderLocationController {

    private final ProviderLocationService providerLocationService;

    public ProviderLocationController(
            ProviderLocationService providerLocationService) {
        this.providerLocationService = providerLocationService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderLocationResponse>>
            getPublicLocationsForProvider(
                    @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerLocationService
                        .getPublicLocationsForProvider(providerId));
    }
}