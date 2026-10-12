package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NeedCategoryMappingUpdateRequest(
        @NotBlank @Size(max = 500) String rationale
) {
}