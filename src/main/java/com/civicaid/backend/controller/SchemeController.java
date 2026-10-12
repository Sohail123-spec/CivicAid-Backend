
package com.civicaid.backend.controller;

import com.civicaid.backend.dto.SchemeResponse;
import com.civicaid.backend.service.SchemeService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schemes")
public class SchemeController {

    private final SchemeService schemeService;

    public SchemeController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @GetMapping
    public ResponseEntity<List<SchemeResponse>> getPublicSchemes(
            @RequestParam(required = false) String stateCode) {

        return ResponseEntity.ok(
                schemeService.getPublicSchemes(stateCode));
    }

    @GetMapping("/{schemeId}")
    public ResponseEntity<SchemeResponse> getPublicScheme(
            @PathVariable UUID schemeId,
            @RequestParam(required = false) String stateCode) {

        return ResponseEntity.ok(
                schemeService.getPublicScheme(schemeId, stateCode));
    }
}