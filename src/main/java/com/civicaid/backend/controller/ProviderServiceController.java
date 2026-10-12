package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceResponse;
import com.civicaid.backend.service.ProviderServiceService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/{providerId}/services")
public class ProviderServiceController {

    private final ProviderServiceService providerServiceService;

    public ProviderServiceController(
            ProviderServiceService providerServiceService) {

        this.providerServiceService = providerServiceService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceResponse>>
            getPublicServicesByProvider(
                    @PathVariable UUID providerId) {

        return ResponseEntity.ok(
                providerServiceService.getPublicServicesByProvider(
                        providerId));
    }
}