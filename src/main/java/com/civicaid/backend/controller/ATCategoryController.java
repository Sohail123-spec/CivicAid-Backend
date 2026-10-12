package com.civicaid.backend.controller;

import com.civicaid.backend.dto.ATCategoryResponse;
import com.civicaid.backend.service.ATCategoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/at-categories")
public class ATCategoryController {

    private final ATCategoryService atCategoryService;

    public ATCategoryController(ATCategoryService atCategoryService) {
        this.atCategoryService = atCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<ATCategoryResponse>> getPublicCategories() {
        return ResponseEntity.ok(atCategoryService.getPublicCategories());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ATCategoryResponse> getPublicCategory(
            @PathVariable UUID categoryId) {

        return ResponseEntity.ok(atCategoryService.getPublicCategory(categoryId));
    }
}
