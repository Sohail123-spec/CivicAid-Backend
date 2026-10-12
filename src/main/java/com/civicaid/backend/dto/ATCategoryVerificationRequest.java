package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ATCategoryVerificationRequest(
        @NotNull(message = "Verification result is required")
        VerificationResult result,

        @NotBlank(message = "Source name is required")
        @Size(max = 200, message = "Source name must be 200 characters or fewer")
        String sourceName,

        @Pattern(regexp = "^https://\\S+$", message = "Source URL must use HTTPS")
        @Size(max = 2048, message = "Source URL must be 2048 characters or fewer")
        String sourceUrl,

        @Size(max = 100, message = "Source version must be 100 characters or fewer")
        String sourceVersion,

        @Size(max = 1000, message = "Review notes must be 1000 characters or fewer")
        String reviewNotes
) {
}
