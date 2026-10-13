package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SchemeCategoryCreateRequest(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotBlank(message = "Rationale is required")
        @Size(max = 500, message = "Rationale must not exceed 500 characters")
        String rationale
) {
}
