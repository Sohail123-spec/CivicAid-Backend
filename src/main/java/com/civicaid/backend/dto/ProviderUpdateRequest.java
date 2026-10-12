package com.civicaid.backend.dto;

import com.civicaid.backend.entity.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderUpdateRequest(

        @NotBlank(message = "Provider name is required")
        @Size(max = 200, message = "Provider name must be 200 characters or fewer")
        String name,

        @NotNull(message = "Provider type is required")
        ProviderType providerType,

        @Size(max = 1000, message = "Provider description must be 1000 characters or fewer")
        String description,

        @Pattern(
                regexp = "^https://\\S+$",
                message = "Website URL must use HTTPS"
        )
        @Size(max = 2048, message = "Website URL must be 2048 characters or fewer")
        String websiteUrl,

        @NotBlank(message = "Source name is required")
        @Size(max = 200, message = "Source name must be 200 characters or fewer")
        String sourceName,

        @Pattern(
                regexp = "^https://\\S+$",
                message = "Source URL must use HTTPS"
        )
        @Size(max = 2048, message = "Source URL must be 2048 characters or fewer")
        String sourceUrl

) {
}
