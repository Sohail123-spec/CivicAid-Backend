
package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ATProductVerificationRequest(

        @NotNull
        VerificationResult result,

        @Size(max = 1000)
        String reviewNotes,

        @NotBlank
        @Size(max = 200)
        String sourceName,

        @Pattern(
                regexp = "^https://.*$",
                message = "Source URL must use HTTPS"
        )
        @Size(max = 2048)
        String sourceUrl,

        @Size(max = 200)
        String sourceVersion

) {
}