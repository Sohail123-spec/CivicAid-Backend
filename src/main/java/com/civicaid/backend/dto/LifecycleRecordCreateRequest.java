package com.civicaid.backend.dto;

import com.civicaid.backend.entity.AcquisitionMethod;
import com.civicaid.backend.entity.ConditionAtAcquisition;
import com.civicaid.backend.entity.LifecycleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record LifecycleRecordCreateRequest(

        @NotBlank
        @Size(max = 200)
        String itemName,

        UUID atProductId,

        UUID atCategoryId,

        @NotNull
        @PastOrPresent
        LocalDate acquiredOn,

        @NotNull
        AcquisitionMethod acquisitionMethod,

        @NotNull
        ConditionAtAcquisition conditionAtAcquisition,

        @NotNull
        LifecycleStatus lifecycleStatus,

        LocalDate warrantyExpiresOn,

        UUID preferredProviderServiceLocationId,

        LocalDate replacementDueOn,

        LocalDate reassessmentDueOn,

        UUID replacesLifecycleRecordId) {
}
