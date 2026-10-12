package com.civicaid.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ProviderServiceCategoryResponse(
        UUID id,
        UUID providerId,
        String providerName,
        UUID providerServiceId,
        String serviceCode,
        String serviceName,
        UUID atCategoryId,
        String categoryName,
        String rationale,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}