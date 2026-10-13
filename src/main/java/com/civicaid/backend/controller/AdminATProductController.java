
package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ATProductCreateRequest;
import com.civicaid.backend.dto.ATProductResponse;
import com.civicaid.backend.dto.ATProductUpdateRequest;
import com.civicaid.backend.dto.ATProductVerificationRecordResponse;
import com.civicaid.backend.dto.ATProductVerificationRequest;
import com.civicaid.backend.service.ATProductService;
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
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/admin")
public class AdminATProductController {

    private final ATProductService atProductService;

    public AdminATProductController(ATProductService atProductService) {
        this.atProductService = atProductService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ATProductResponse>> getAllProductsForAdmin() {
        return ResponseEntity.ok(atProductService.getAllProductsForAdmin());
    }

    @PostMapping("/at-categories/{categoryId}/products")
    public ResponseEntity<ATProductResponse> createProduct(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ATProductCreateRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atProductService.createProduct(
                        categoryId, request, authentication.getName()));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<ATProductResponse> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ATProductUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                atProductService.updateProduct(
                        productId, request, authentication.getName()));
    }

    @PostMapping("/products/{productId}/verifications")
    public ResponseEntity<ATProductVerificationRecordResponse> verifyProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ATProductVerificationRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atProductService.verifyProduct(
                        productId, request, authentication.getName()));
    }

    @GetMapping("/products/{productId}/verification-records")
    public ResponseEntity<List<ATProductVerificationRecordResponse>>
            getVerificationHistory(@PathVariable UUID productId) {

        return ResponseEntity.ok(
                atProductService.getVerificationHistory(productId));
    }
    
    @PatchMapping("/products/{productId}/archive")
    public ResponseEntity<ATProductResponse> archiveProduct(
            @PathVariable UUID productId,
            Authentication authentication) {

        return ResponseEntity.ok(
                atProductService.setProductActive(
                        productId, false, authentication.getName()));
    }

    @PatchMapping("/products/{productId}/activate")
    public ResponseEntity<ATProductResponse> activateProduct(
            @PathVariable UUID productId,
            Authentication authentication) {

        return ResponseEntity.ok(
                atProductService.setProductActive(
                        productId, true, authentication.getName()));
    }
}