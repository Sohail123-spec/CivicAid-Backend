
package com.civicaid.backend.controller;

import com.civicaid.backend.dto.SchemeCreateRequest;
import com.civicaid.backend.dto.SchemeResponse;
import com.civicaid.backend.dto.SchemeUpdateRequest;
import com.civicaid.backend.service.SchemeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
import com.civicaid.backend.dto.SchemeCategoryCreateRequest;
import com.civicaid.backend.dto.SchemeCategoryResponse;
import com.civicaid.backend.dto.SchemeProductCreateRequest;
import com.civicaid.backend.dto.SchemeProductResponse;
import com.civicaid.backend.dto.SchemeVerificationRequest;
import com.civicaid.backend.dto.SchemeVerificationResponse;

@RestController
@RequestMapping("/api/admin/schemes")
public class AdminSchemeController {

    private final SchemeService schemeService;

    public AdminSchemeController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @PostMapping
    public ResponseEntity<SchemeResponse> createScheme(
            @Valid @RequestBody SchemeCreateRequest request,
            Authentication authentication) {

        SchemeResponse created = schemeService.createScheme(
                request, authentication.getName());

        return ResponseEntity
                .created(URI.create("/api/admin/schemes/" + created.id()))
                .body(created);
    }

    @GetMapping
    public ResponseEntity<List<SchemeResponse>> getAllSchemesForAdmin() {
        return ResponseEntity.ok(schemeService.getAllSchemesForAdmin());
    }

    @GetMapping("/{schemeId}")
    public ResponseEntity<SchemeResponse> getSchemeForAdmin(
            @PathVariable UUID schemeId) {

        return ResponseEntity.ok(
                schemeService.getSchemeForAdmin(schemeId));
    }

    @PutMapping("/{schemeId}")
    public ResponseEntity<SchemeResponse> updateScheme(
            @PathVariable UUID schemeId,
            @Valid @RequestBody SchemeUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                schemeService.updateScheme(
                        schemeId, request, authentication.getName()));
    }

    @PatchMapping("/{schemeId}/archive")
    public ResponseEntity<SchemeResponse> archiveScheme(
            @PathVariable UUID schemeId,
            Authentication authentication) {

        return ResponseEntity.ok(
                schemeService.setSchemeActive(
                        schemeId, false, authentication.getName()));
    }

    @PatchMapping("/{schemeId}/activate")
    public ResponseEntity<SchemeResponse> activateScheme(
            @PathVariable UUID schemeId,
            Authentication authentication) {

        return ResponseEntity.ok(
                schemeService.setSchemeActive(
                        schemeId, true, authentication.getName()));
    }
    
    @PostMapping("/{schemeId}/verifications")
    public ResponseEntity<SchemeVerificationResponse> verifyScheme(
            @PathVariable UUID schemeId,
            @Valid @RequestBody SchemeVerificationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                schemeService.verifyScheme(
                        schemeId, request, authentication.getName()));
    }

    @GetMapping("/{schemeId}/verification-records")
    public ResponseEntity<List<SchemeVerificationResponse>> getSchemeVerificationHistory(
            @PathVariable UUID schemeId) {

        return ResponseEntity.ok(
                schemeService.getVerificationHistory(schemeId));
    }

    @GetMapping("/{schemeId}/categories")
    public ResponseEntity<List<SchemeCategoryResponse>> getSchemeCategories(
            @PathVariable UUID schemeId) {
        return ResponseEntity.ok(schemeService.getAdminSchemeCategories(schemeId));
    }

    @PostMapping("/{schemeId}/categories")
    public ResponseEntity<SchemeCategoryResponse> addSchemeCategory(
            @PathVariable UUID schemeId,
            @Valid @RequestBody SchemeCategoryCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(schemeService.addSchemeCategory(
                schemeId, request, authentication.getName()));
    }

    @GetMapping("/{schemeId}/products")
    public ResponseEntity<List<SchemeProductResponse>> getSchemeProducts(
            @PathVariable UUID schemeId) {
        return ResponseEntity.ok(schemeService.getAdminSchemeProducts(schemeId));
    }

    @PostMapping("/{schemeId}/products")
    public ResponseEntity<SchemeProductResponse> addSchemeProduct(
            @PathVariable UUID schemeId,
            @Valid @RequestBody SchemeProductCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(schemeService.addSchemeProduct(
                schemeId, request, authentication.getName()));
    }

}