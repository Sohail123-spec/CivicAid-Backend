package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderResponse;
import com.civicaid.backend.service.ProviderService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> getPublicProviders() {
        return ResponseEntity.ok(
                providerService.getPublicProviders());
    }

    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderResponse> getPublicProvider(
            @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerService.getPublicProvider(providerId));
    }
}