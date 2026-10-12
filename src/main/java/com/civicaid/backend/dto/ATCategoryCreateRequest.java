package com.civicaid.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ATCategoryCreateRequest(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must use uppercase letters, numbers, and underscores")
        @Size(max = 64, message = "Code must be 64 characters or fewer")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be 150 characters or fewer")
        String name,

        @Size(max = 750, message = "Description must be 750 characters or fewer")
        String description,

        @Min(value = 0, message = "Display order must be zero or greater")
        int displayOrder,

        @NotBlank(message = "Source name is required")
        @Size(max = 200, message = "Source name must be 200 characters or fewer")
        String sourceName,

        @Pattern(regexp = "^https://\\S+$", message = "Source URL must use HTTPS")
        @Size(max = 2048, message = "Source URL must be 2048 characters or fewer")
        String sourceUrl,

        @NotNull(message = "Active state is required")
        Boolean active
) {
}
