package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProviderServiceLocationResponse(

        UUID id,

        UUID providerId,
        String providerName,

        UUID providerServiceId,
        String serviceCode,
        String serviceName,

        UUID providerLocationId,
        String addressLine1,
        String addressLine2,
        String locality,
        String city,
        String stateCode,
        String postalCode,
        String countryCode,

        BigDecimal latitude,
        BigDecimal longitude,

        String publicPhone,
        String publicEmail,
        String accessibilityNotes,

        String sourceName,
        String sourceUrl,

        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,

        boolean active,

        Instant createdAt,
        Instant updatedAt

) {
}