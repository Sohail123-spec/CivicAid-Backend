package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SchemeProductCreateRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotBlank(message = "Rationale is required")
        @Size(max = 500, message = "Rationale must not exceed 500 characters")
        String rationale
) {
}
