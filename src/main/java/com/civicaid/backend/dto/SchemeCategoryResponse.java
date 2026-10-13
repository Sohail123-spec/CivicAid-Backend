package com.civicaid.backend.dto;

import java.util.UUID;

public record SchemeCategoryResponse(
        UUID id,
        UUID schemeId,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        String categoryDescription,
        String rationale,
        boolean active
) {
}
