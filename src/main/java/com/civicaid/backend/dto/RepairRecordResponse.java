package com.civicaid.backend.dto;

import com.civicaid.backend.entity.RepairOutcomeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RepairRecordResponse(
        UUID id,
        UUID lifecycleRecordId,
        LocalDate repairDate,
        UUID providerServiceLocationId,
        String issueDescription,
        RepairOutcomeStatus repairOutcomeStatus,
        String workSummary,
        BigDecimal costInr,
        Boolean warrantyUsed,
        Instant createdAt,
        Instant updatedAt) {
}