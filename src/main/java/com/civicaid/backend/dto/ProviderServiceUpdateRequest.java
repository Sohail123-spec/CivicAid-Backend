package com.civicaid.backend.dto;

import com.civicaid.backend.entity.ProviderServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderServiceUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        ProviderServiceType serviceType,

        @NotBlank
        String description,

        @NotBlank
        @Size(max = 200)
        String sourceName,

        @Pattern(regexp = "^https://\\S+$")
        @Size(max = 2048)
        String sourceUrl
) {}