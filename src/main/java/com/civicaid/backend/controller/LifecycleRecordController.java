package com.civicaid.backend.controller;

import com.civicaid.backend.dto.LifecycleRecordCreateRequest;
import com.civicaid.backend.dto.LifecycleRecordResponse;
import com.civicaid.backend.security.AuthenticatedUser;
import com.civicaid.backend.service.LifecycleRecordService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/lifecycle-records")
public class LifecycleRecordController {

    private final LifecycleRecordService lifecycleRecordService;

    public LifecycleRecordController(
            LifecycleRecordService lifecycleRecordService) {

        this.lifecycleRecordService = lifecycleRecordService;
    }

    @GetMapping
    public List<LifecycleRecordResponse> getMyLifecycleRecords(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        return lifecycleRecordService.getMyLifecycleRecords(
                authenticatedUser.getUsername());
    }

    @GetMapping("/{lifecycleRecordId}")
    public LifecycleRecordResponse getMyLifecycleRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID lifecycleRecordId) {

        return lifecycleRecordService.getMyLifecycleRecord(
                lifecycleRecordId,
                authenticatedUser.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LifecycleRecordResponse createLifecycleRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody LifecycleRecordCreateRequest request) {

        return lifecycleRecordService.createLifecycleRecord(
                request,
                authenticatedUser.getUsername());
    }

    @DeleteMapping("/{lifecycleRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyLifecycleRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID lifecycleRecordId) {

        lifecycleRecordService.deleteMyLifecycleRecord(
                lifecycleRecordId,
                authenticatedUser.getUsername());
    }
}