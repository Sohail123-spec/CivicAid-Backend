package com.civicaid.backend.dto;

import com.civicaid.backend.entity.AcquisitionMethod;
import com.civicaid.backend.entity.ConditionAtAcquisition;
import com.civicaid.backend.entity.LifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LifecycleRecordResponse(

        UUID id,

        String itemName,

        UUID atProductId,

        UUID atCategoryId,

        LocalDate acquiredOn,

        AcquisitionMethod acquisitionMethod,

        ConditionAtAcquisition conditionAtAcquisition,

        LifecycleStatus lifecycleStatus,

        LocalDate warrantyExpiresOn,

        UUID preferredProviderServiceLocationId,

        LocalDate replacementDueOn,

        LocalDate reassessmentDueOn,

        UUID replacesLifecycleRecordId,

        Instant createdAt,

        Instant updatedAt

) {
}