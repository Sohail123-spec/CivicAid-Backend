
package com.civicaid.backend.dto;

import com.civicaid.backend.entity.ApplicationMethod;
import com.civicaid.backend.entity.JurisdictionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SchemeCreateRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
        @Size(max = 64)
        String code,

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 200)
        String authorityName,

        @NotBlank
        @Size(max = 10000)
        String description,

        @NotBlank
        @Size(max = 5000)
        String benefitsSummary,

        @NotBlank
        @Size(max = 5000)
        String eligibilitySummary,

        @Size(max = 5000)
        String requiredDocumentsSummary,

        @NotNull
        ApplicationMethod applicationMethod,

        @NotBlank
        @Size(max = 10000)
        String applicationInstructions,

        @NotBlank
        @Pattern(
                regexp = "^https://\\S+$",
                message = "Official source URL must use HTTPS"
        )
        @Size(max = 2048)
        String officialSourceUrl,

        @Pattern(
                regexp = "^https://\\S+$",
                message = "Official application URL must use HTTPS"
        )
        @Size(max = 2048)
        String officialApplicationUrl,

        @NotNull
        JurisdictionLevel jurisdictionLevel,

        @Pattern(
                regexp = "^[A-Z]{2}$",
                message = "State code must contain two uppercase letters"
        )
        String applicableStateCode,

        @Size(max = 100)
        String sourceVersion,

        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {
}