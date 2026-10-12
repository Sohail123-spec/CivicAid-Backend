
package com.civicaid.backend.dto;

import com.civicaid.backend.entity.ProviderType;
import com.civicaid.backend.entity.VerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        String code,
        String name,
        ProviderType providerType,
        String description,
        String websiteUrl,
        String sourceName,
        String sourceUrl,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
