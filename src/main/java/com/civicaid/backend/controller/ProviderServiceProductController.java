package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceProductResponse;
import com.civicaid.backend.service.ProviderServiceProductService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider-service-products")
public class ProviderServiceProductController {

    private final ProviderServiceProductService providerServiceProductService;

    public ProviderServiceProductController(
            ProviderServiceProductService providerServiceProductService) {
        this.providerServiceProductService = providerServiceProductService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceProductResponse>> getPublicMappings() {
        return ResponseEntity.ok(
                providerServiceProductService.getPublicMappings());
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ProviderServiceProductResponse>> getPublicMappingsByProduct(
            @PathVariable UUID productId) {

        return ResponseEntity.ok(
                providerServiceProductService.getPublicMappingsByProduct(productId));
    }
}