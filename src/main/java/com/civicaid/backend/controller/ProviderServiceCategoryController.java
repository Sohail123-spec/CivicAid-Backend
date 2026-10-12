package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ProviderServiceCategoryResponse;
import com.civicaid.backend.service.ProviderServiceCategoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider-service-categories")
public class ProviderServiceCategoryController {

    private final ProviderServiceCategoryService
            providerServiceCategoryService;

    public ProviderServiceCategoryController(
            ProviderServiceCategoryService providerServiceCategoryService) {

        this.providerServiceCategoryService =
                providerServiceCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderServiceCategoryResponse>>
            getPublicMappings() {

        return ResponseEntity.ok(
                providerServiceCategoryService.getPublicMappings());
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<List<ProviderServiceCategoryResponse>>
            getPublicMappingsByCategory(
                    @PathVariable UUID categoryId) {

        return ResponseEntity.ok(
                providerServiceCategoryService
                        .getPublicMappingsByCategory(categoryId));
    }
}