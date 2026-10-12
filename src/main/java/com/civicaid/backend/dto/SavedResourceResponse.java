package com.civicaid.backend.dto;

import com.civicaid.backend.entity.SavedResourceType;
import java.time.Instant;
import java.util.UUID;

public record SavedResourceResponse(
        UUID id,
        SavedResourceType resourceType,
        UUID resourceId,
        Instant createdAt
) {
}