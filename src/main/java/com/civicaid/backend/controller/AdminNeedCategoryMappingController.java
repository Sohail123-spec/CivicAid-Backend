package com.civicaid.backend.controller;

import com.civicaid.backend.dto.NeedCategoryMappingCreateRequest;
import com.civicaid.backend.dto.NeedCategoryMappingResponse;
import com.civicaid.backend.dto.NeedCategoryMappingUpdateRequest;
import com.civicaid.backend.service.NeedCategoryMappingService;
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
@RequestMapping("/api/admin/need-category-mappings")
public class AdminNeedCategoryMappingController {

    private final NeedCategoryMappingService needCategoryMappingService;

    public AdminNeedCategoryMappingController(
            NeedCategoryMappingService needCategoryMappingService) {
        this.needCategoryMappingService = needCategoryMappingService;
    }

    @PostMapping
    public ResponseEntity<NeedCategoryMappingResponse> createMapping(
            @Valid @RequestBody NeedCategoryMappingCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(needCategoryMappingService.createMapping(
                        request,
                        authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<NeedCategoryMappingResponse>> getAdminMappings() {
        return ResponseEntity.ok(
                needCategoryMappingService.getAdminMappings());
    }

    @GetMapping("/needs/{functionalNeedId}")
    public ResponseEntity<List<NeedCategoryMappingResponse>> getAdminMappingsByFunctionalNeed(
            @PathVariable UUID functionalNeedId) {

        return ResponseEntity.ok(
                needCategoryMappingService.getAdminMappingsByFunctionalNeed(functionalNeedId));
    }

    @GetMapping("/categories/{atCategoryId}")
    public ResponseEntity<List<NeedCategoryMappingResponse>> getAdminMappingsByCategory(
            @PathVariable UUID atCategoryId) {

        return ResponseEntity.ok(
                needCategoryMappingService.getAdminMappingsByCategory(atCategoryId));
    }

    @GetMapping("/{mappingId}")
    public ResponseEntity<NeedCategoryMappingResponse> getAdminMapping(
            @PathVariable UUID mappingId) {

        return ResponseEntity.ok(
                needCategoryMappingService.getAdminMapping(mappingId));
    }

    @PutMapping("/{mappingId}")
    public ResponseEntity<NeedCategoryMappingResponse> updateMapping(
            @PathVariable UUID mappingId,
            @Valid @RequestBody NeedCategoryMappingUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                needCategoryMappingService.updateMapping(
                        mappingId,
                        request,
                        authentication.getName()));
    }

    @PatchMapping("/{mappingId}/archive")
    public ResponseEntity<NeedCategoryMappingResponse> archiveMapping(
            @PathVariable UUID mappingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                needCategoryMappingService.setMappingActive(
                        mappingId,
                        false,
                        authentication.getName()));
    }

    @PatchMapping("/{mappingId}/activate")
    public ResponseEntity<NeedCategoryMappingResponse> activateMapping(
            @PathVariable UUID mappingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                needCategoryMappingService.setMappingActive(
                        mappingId,
                        true,
                        authentication.getName()));
    }
}