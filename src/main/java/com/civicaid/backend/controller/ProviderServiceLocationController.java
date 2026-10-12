package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceLocationResponse;
import com.civicaid.backend.service.ProviderServiceLocationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/{providerId}/service-locations")
public class ProviderServiceLocationController {

    private final ProviderServiceLocationService
            providerServiceLocationService;

    public ProviderServiceLocationController(
            ProviderServiceLocationService providerServiceLocationService) {

        this.providerServiceLocationService =
                providerServiceLocationService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceLocationResponse>>
            getPublicServiceLocationsByProvider(
                    @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerServiceLocationService
                        .getPublicServiceLocationsByProvider(providerId));
    }
}