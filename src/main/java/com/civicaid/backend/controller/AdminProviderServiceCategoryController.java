package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceCategoryCreateRequest;
import com.civicaid.backend.dto.ProviderServiceCategoryResponse;
import com.civicaid.backend.dto.ProviderServiceCategoryUpdateRequest;
import com.civicaid.backend.service.ProviderServiceCategoryService;
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
@RequestMapping("/api/admin/provider-service-categories")
public class AdminProviderServiceCategoryController {

    private final ProviderServiceCategoryService providerServiceCategoryService;

    public AdminProviderServiceCategoryController(
            ProviderServiceCategoryService providerServiceCategoryService) {

        this.providerServiceCategoryService =
                providerServiceCategoryService;
    }

    @PostMapping("/services/{providerServiceId}")
    public ResponseEntity<ProviderServiceCategoryResponse> createMapping(
            @PathVariable UUID providerServiceId,
            @Valid @RequestBody ProviderServiceCategoryCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerServiceCategoryService.createMapping(
                        providerServiceId,
                        request,
                        authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceCategoryResponse>>
            getAdminMappings() {

        return ResponseEntity.ok(
                providerServiceCategoryService.getAdminMappings());
    }

    @GetMapping("/services/{providerServiceId}")
    public ResponseEntity<List<ProviderServiceCategoryResponse>>
            getMappingsByService(
                    @PathVariable UUID providerServiceId) {

        return ResponseEntity.ok(
                providerServiceCategoryService.getAdminMappingsByService(
                        providerServiceId));
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<List<ProviderServiceCategoryResponse>>
            getMappingsByCategory(
                    @PathVariable UUID categoryId) {

        return ResponseEntity.ok(
                providerServiceCategoryService.getAdminMappingsByCategory(
                        categoryId));
    }

    @GetMapping("/{mappingId}")
    public ResponseEntity<ProviderServiceCategoryResponse> getAdminMapping(
            @PathVariable UUID mappingId) {

        return ResponseEntity.ok(
                providerServiceCategoryService.getAdminMapping(mappingId));
    }

    @PutMapping("/{mappingId}")
    public ResponseEntity<ProviderServiceCategoryResponse> updateMapping(
            @PathVariable UUID mappingId,
            @Valid @RequestBody ProviderServiceCategoryUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceCategoryService.updateMapping(
                        mappingId,
                        request,
                        authentication.getName()));
    }

    @PatchMapping("/{mappingId}/archive")
    public ResponseEntity<ProviderServiceCategoryResponse> archiveMapping(
            @PathVariable UUID mappingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceCategoryService.setMappingActive(
                        mappingId,
                        false,
                        authentication.getName()));
    }

    @PatchMapping("/{mappingId}/activate")
    public ResponseEntity<ProviderServiceCategoryResponse> activateMapping(
            @PathVariable UUID mappingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                providerServiceCategoryService.setMappingActive(
                        mappingId,
                        true,
                        authentication.getName()));
    }
}