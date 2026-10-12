package com.civicaid.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ProviderServiceProductResponse(
        UUID id,
        UUID providerId,
        String providerName,
        UUID providerServiceId,
        String serviceCode,
        String serviceName,
        UUID atProductId,
        String productCode,
        String productName,
        UUID atCategoryId,
        String categoryName,
        String rationale,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}