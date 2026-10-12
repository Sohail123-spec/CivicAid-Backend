package com.civicaid.backend.dto;

import com.civicaid.backend.entity.MaintenanceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceRecordResponse(
        UUID id,
        UUID lifecycleRecordId,
        MaintenanceType maintenanceType,
        LocalDate maintenanceDate,
        UUID providerServiceLocationId,
        String details,
        LocalDate nextMaintenanceDueOn,
        Instant createdAt,
        Instant updatedAt) {
}