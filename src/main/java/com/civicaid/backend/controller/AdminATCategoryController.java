package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ATCategoryCreateRequest;
import com.civicaid.backend.dto.ATCategoryResponse;
import com.civicaid.backend.dto.ATCategoryUpdateRequest;
import com.civicaid.backend.dto.ATCategoryVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.service.ATCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/at-categories")
public class AdminATCategoryController {

    private final ATCategoryService atCategoryService;

    public AdminATCategoryController(ATCategoryService atCategoryService) {
        this.atCategoryService = atCategoryService;
    }

    @PostMapping
    public ResponseEntity<ATCategoryResponse> createCategory(
            @Valid @RequestBody ATCategoryCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atCategoryService.createCategory(request, authentication.getName()));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ATCategoryResponse> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ATCategoryUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                atCategoryService.updateCategory(categoryId, request, authentication.getName()));
    }

    @PostMapping("/{categoryId}/verifications")
    public ResponseEntity<VerificationRecordResponse> verifyCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ATCategoryVerificationRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atCategoryService.verifyCategory(categoryId, request, authentication.getName()));
    }

    @GetMapping("/{categoryId}/verification-records")
    public ResponseEntity<List<VerificationRecordResponse>> getVerificationHistory(
            @PathVariable UUID categoryId) {

        return ResponseEntity.ok(atCategoryService.getVerificationHistory(categoryId));
    }
}
