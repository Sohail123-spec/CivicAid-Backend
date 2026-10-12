package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ATProductResponse(
        UUID id,
        UUID categoryId,
        String code,
        String name,
        String description,
        String considerations,
        int displayOrder,
        String sourceName,
        String sourceUrl,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        boolean active) {
}