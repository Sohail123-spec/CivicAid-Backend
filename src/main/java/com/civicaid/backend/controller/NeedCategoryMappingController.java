package com.civicaid.backend.controller;

import com.civicaid.backend.dto.NeedCategoryMappingResponse;
import com.civicaid.backend.service.NeedCategoryMappingService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/need-category-mappings")
public class NeedCategoryMappingController {

    private final NeedCategoryMappingService needCategoryMappingService;

    public NeedCategoryMappingController(
            NeedCategoryMappingService needCategoryMappingService) {
        this.needCategoryMappingService = needCategoryMappingService;
    }

    @GetMapping
    public ResponseEntity<List<NeedCategoryMappingResponse>> getPublicMappings() {
        return ResponseEntity.ok(
                needCategoryMappingService.getPublicMappings());
    }

    @GetMapping("/needs/{functionalNeedId}")
    public ResponseEntity<List<NeedCategoryMappingResponse>> getPublicMappingsByFunctionalNeed(
            @PathVariable UUID functionalNeedId) {

        return ResponseEntity.ok(
                needCategoryMappingService.getPublicMappingsByFunctionalNeed(functionalNeedId));
    }

    @GetMapping("/categories/{atCategoryId}")
    public ResponseEntity<List<NeedCategoryMappingResponse>> getPublicMappingsByCategory(
            @PathVariable UUID atCategoryId) {

        return ResponseEntity.ok(
                needCategoryMappingService.getPublicMappingsByCategory(atCategoryId));
    }
}