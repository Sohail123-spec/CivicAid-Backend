package com.civicaid.backend.controller;

import com.civicaid.backend.dto.MaintenanceRecordCreateRequest;
import com.civicaid.backend.dto.MaintenanceRecordResponse;
import com.civicaid.backend.dto.MaintenanceRecordUpdateRequest;
import com.civicaid.backend.security.AuthenticatedUser;
import com.civicaid.backend.service.MaintenanceRecordService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/maintenance-records")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(
            MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping("/lifecycle/{lifecycleRecordId}")
    public List<MaintenanceRecordResponse> getMyMaintenanceRecords(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID lifecycleRecordId) {

        return maintenanceRecordService.getMyMaintenanceRecords(
                lifecycleRecordId,
                authenticatedUser.getUsername());
    }

    @GetMapping("/{maintenanceRecordId}")
    public MaintenanceRecordResponse getMyMaintenanceRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID maintenanceRecordId) {

        return maintenanceRecordService.getMyMaintenanceRecord(
                maintenanceRecordId,
                authenticatedUser.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecordResponse createMaintenanceRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody MaintenanceRecordCreateRequest request) {

        return maintenanceRecordService.createMaintenanceRecord(
                request,
                authenticatedUser.getUsername());
    }

    @PatchMapping("/{maintenanceRecordId}")
    public MaintenanceRecordResponse updateMaintenanceRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID maintenanceRecordId,
            @Valid @RequestBody MaintenanceRecordUpdateRequest request) {

        return maintenanceRecordService.updateMaintenanceRecord(
                maintenanceRecordId,
                request,
                authenticatedUser.getUsername());
    }

    @DeleteMapping("/{maintenanceRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyMaintenanceRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID maintenanceRecordId) {

        maintenanceRecordService.deleteMyMaintenanceRecord(
                maintenanceRecordId,
                authenticatedUser.getUsername());
    }
}