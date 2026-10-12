package com.civicaid.backend.dto;

import java.util.UUID;

public record FunctionalNeedResponse(
        UUID id,
        String code,
        String label,
        String description,
        UUID parentNeedId,
        boolean selectable,
        int displayOrder
) {
}