package com.civicaid.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ATProductCreateRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
        @Size(max = 64)
        String code,

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 1000)
        String description,

        @Size(max = 1000)
        String considerations,

        @Min(0)
        int displayOrder,

        @NotBlank
        @Size(max = 200)
        String sourceName,

        @Pattern(regexp = "^https://.*$", message = "Source URL must use HTTPS")
        @Size(max = 2048)
        String sourceUrl
) {
}