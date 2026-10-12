
package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ATProductResponse;
import com.civicaid.backend.service.ATProductService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ATProductController {

    private final ATProductService atProductService;

    public ATProductController(ATProductService atProductService) {
        this.atProductService = atProductService;
    }

    @GetMapping
    public ResponseEntity<List<ATProductResponse>> getPublicProducts() {
        return ResponseEntity.ok(atProductService.getPublicProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ATProductResponse> getPublicProduct(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(
                atProductService.getPublicProduct(productId));
    }
}