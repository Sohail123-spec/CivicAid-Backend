
package com.civicaid.backend.dto;

import com.civicaid.backend.entity.ApplicationMethod;
import com.civicaid.backend.entity.JurisdictionLevel;
import com.civicaid.backend.entity.VerificationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SchemeResponse(
        UUID id,
        String code,
        String name,
        String authorityName,
        String description,
        String benefitsSummary,
        String eligibilitySummary,
        String requiredDocumentsSummary,
        ApplicationMethod applicationMethod,
        String applicationInstructions,
        String officialSourceUrl,
        String officialApplicationUrl,
        JurisdictionLevel jurisdictionLevel,
        String applicableStateCode,
        String sourceVersion,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}