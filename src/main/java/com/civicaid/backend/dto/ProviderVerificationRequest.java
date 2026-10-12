package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderVerificationRequest(

        @NotNull(message = "Verification result is required")
        VerificationResult result,

        @NotBlank(message = "Source name is required")
        @Size(max = 200)
        String sourceName,

        @Pattern(regexp = "^https://\\S+$")
        @Size(max = 2048)
        String sourceUrl,

        @Size(max = 100)
        String sourceVersion,

        @Size(max = 1000)
        String reviewNotes

) {
}