package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceProductCreateRequest;
import com.civicaid.backend.dto.ProviderServiceProductResponse;
import com.civicaid.backend.dto.ProviderServiceProductUpdateRequest;
import com.civicaid.backend.service.ProviderServiceProductService;
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
@RequestMapping("/api/admin/provider-service-products")
public class AdminProviderServiceProductController {

    private final ProviderServiceProductService providerServiceProductService;

    public AdminProviderServiceProductController(
            ProviderServiceProductService providerServiceProductService) {
        this.providerServiceProductService = providerServiceProductService;
    }

    @PostMapping("/services/{providerServiceId}")
    public ResponseEntity<ProviderServiceProductResponse> createMapping(
            @PathVariable UUID providerServiceId,
            @Valid @RequestBody ProviderServiceProductCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerServiceProductService.createMapping(
                        providerServiceId,
                        request,
                        authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceProductResponse>> getAdminMappings() {
        return ResponseEntity.ok(
                providerServiceProductService.getAdminMappings());
    }

    @GetMapping("/services/{providerServiceId}")
    public ResponseEntity<List<ProviderServiceProductResponse>> getMappingsByService(
            @PathVariable UUID providerServiceId) {

        return ResponseEntity.ok(
                providerServiceProductService.getAdminMappingsByService(
                        providerServiceId));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ProviderServiceProductResponse>> getMappingsByProduct(
            @PathVariable UUID productId) {

        return ResponseEntity.ok(
                providerServiceProductService.getAdminMappingsByProduct(
                        productId));
    }

    @GetMapping("/{mappingId}")
    public ResponseEntity<ProviderServiceProductResponse> getAdminMapping(
            @PathVariable UUID mappingId) {

        return ResponseEntity.ok(
                providerServiceProductService.getAdminMapping(mappingId));
    }

    @PutMapping("/{mappingId}")
    public ResponseEntity<ProviderServiceProductResponse> updateMapping(
            @PathVariable UUID mappingId,
            @Valid @RequestBody ProviderServiceProductUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceProductService.updateMapping(
                        mappingId,
                        request,
                        authentication.getName()));
    }

    @PatchMapping("/{mappingId}/archive")
    public ResponseEntity<ProviderServiceProductResponse> archiveMapping(
            @PathVariable UUID mappingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceProductService.setMappingActive(
                        mappingId,
                        false,
                        authentication.getName()));
    }

    @PatchMapping("/{mappingId}/activate")
    public ResponseEntity<ProviderServiceProductResponse> activateMapping(
            @PathVariable UUID mappingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceProductService.setMappingActive(
                        mappingId,
                        true,
                        authentication.getName()));
    }
}