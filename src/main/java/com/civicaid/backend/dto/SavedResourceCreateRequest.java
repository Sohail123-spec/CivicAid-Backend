package com.civicaid.backend.dto;

import com.civicaid.backend.entity.SavedResourceType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SavedResourceCreateRequest(

        @NotNull
        SavedResourceType resourceType,

        @NotNull
        UUID resourceId
) {
}