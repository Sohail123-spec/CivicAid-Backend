package com.civicaid.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record NeedCategoryMappingResponse(
        UUID id,
        UUID functionalNeedId,
        String functionalNeedLabel,
        UUID atCategoryId,
        String categoryCode,
        String categoryName,
        String rationale,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}