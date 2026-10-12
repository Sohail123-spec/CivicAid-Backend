package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record NeedCategoryMappingCreateRequest(
        @NotNull UUID functionalNeedId,

        @NotNull UUID atCategoryId,

        @NotBlank @Size(max = 500) String rationale
) {
}