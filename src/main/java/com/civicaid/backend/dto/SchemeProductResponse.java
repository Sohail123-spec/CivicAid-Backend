package com.civicaid.backend.dto;

import java.util.UUID;

public record SchemeProductResponse(
        UUID id,
        UUID schemeId,
        UUID productId,
        String productCode,
        String productName,
        String productDescription,
        UUID categoryId,
        String categoryName,
        String rationale,
        boolean active
) {
}
