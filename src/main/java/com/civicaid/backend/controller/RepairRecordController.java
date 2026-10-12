package com.civicaid.backend.controller;

import com.civicaid.backend.dto.RepairRecordCreateRequest;
import com.civicaid.backend.dto.RepairRecordResponse;
import com.civicaid.backend.dto.RepairRecordUpdateRequest;
import com.civicaid.backend.security.AuthenticatedUser;
import com.civicaid.backend.service.RepairRecordService;
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
@RequestMapping("/api/users/me/repair-records")
public class RepairRecordController {

    private final RepairRecordService repairRecordService;

    public RepairRecordController(
            RepairRecordService repairRecordService) {

        this.repairRecordService = repairRecordService;
    }

    @GetMapping("/lifecycle/{lifecycleRecordId}")
    public List<RepairRecordResponse> getMyRepairRecords(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID lifecycleRecordId) {

        return repairRecordService.getMyRepairRecords(
                lifecycleRecordId,
                authenticatedUser.getUsername());
    }

    @GetMapping("/{repairRecordId}")
    public RepairRecordResponse getMyRepairRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID repairRecordId) {

        return repairRecordService.getMyRepairRecord(
                repairRecordId,
                authenticatedUser.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepairRecordResponse createRepairRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RepairRecordCreateRequest request) {

        return repairRecordService.createRepairRecord(
                request,
                authenticatedUser.getUsername());
    }

    @PatchMapping("/{repairRecordId}")
    public RepairRecordResponse updateRepairRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID repairRecordId,
            @Valid @RequestBody RepairRecordUpdateRequest request) {

        return repairRecordService.updateRepairRecord(
                repairRecordId,
                request,
                authenticatedUser.getUsername());
    }

    @DeleteMapping("/{repairRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyRepairRecord(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID repairRecordId) {

        repairRecordService.deleteMyRepairRecord(
                repairRecordId,
                authenticatedUser.getUsername());
    }
}