package com.civicaid.backend.dto;

import com.civicaid.backend.entity.MaintenanceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceRecordCreateRequest(
        @NotNull
        UUID lifecycleRecordId,

        @NotNull
        MaintenanceType maintenanceType,

        @NotNull
        @PastOrPresent
        LocalDate maintenanceDate,

        UUID providerServiceLocationId,

        @Size(max = 1000)
        String details,

        LocalDate nextMaintenanceDueOn) {
}