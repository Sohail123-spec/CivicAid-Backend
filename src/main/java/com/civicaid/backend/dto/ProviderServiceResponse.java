package com.civicaid.backend.dto;

import com.civicaid.backend.entity.ProviderServiceType;
import com.civicaid.backend.entity.VerificationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProviderServiceResponse(
        UUID id,
        UUID providerId,
        String providerName,
        String code,
        String name,
        ProviderServiceType serviceType,
        String description,
        String sourceName,
        String sourceUrl,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}