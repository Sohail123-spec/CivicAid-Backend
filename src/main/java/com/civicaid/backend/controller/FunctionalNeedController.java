package com.civicaid.backend.controller;

import com.civicaid.backend.dto.FunctionalNeedResponse;
import com.civicaid.backend.service.FunctionalNeedService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/needs")
public class FunctionalNeedController {

    private final FunctionalNeedService functionalNeedService;

    public FunctionalNeedController(FunctionalNeedService functionalNeedService) {
        this.functionalNeedService = functionalNeedService;
    }

    @GetMapping
    public ResponseEntity<List<FunctionalNeedResponse>> getAvailableNeeds() {
        return ResponseEntity.ok(functionalNeedService.getAvailableNeeds());
    }
}