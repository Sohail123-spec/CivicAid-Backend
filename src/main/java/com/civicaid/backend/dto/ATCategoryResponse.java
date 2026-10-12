package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ATCategoryResponse(
        UUID id,
        String code,
        String name,
        String description,
        int displayOrder,
        String sourceName,
        String sourceUrl,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        boolean active
) {
}
