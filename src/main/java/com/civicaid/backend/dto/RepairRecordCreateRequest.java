package com.civicaid.backend.dto;

import com.civicaid.backend.entity.RepairOutcomeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RepairRecordCreateRequest(
        @NotNull
        UUID lifecycleRecordId,

        @NotNull
        @PastOrPresent
        LocalDate repairDate,

        UUID providerServiceLocationId,

        @NotBlank
        @Size(max = 1000)
        String issueDescription,

        @NotNull
        RepairOutcomeStatus repairOutcomeStatus,

        @Size(max = 1000)
        String workSummary,

        @DecimalMin("0.00")
        BigDecimal costInr,

        Boolean warrantyUsed) {
}