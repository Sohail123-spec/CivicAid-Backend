package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProviderServiceLocationCreateRequest(

        @NotNull
        UUID providerLocationId,

        @NotBlank
        @Size(max = 200)
        String sourceName,

        @Pattern(regexp = "^https://\\S+$")
        @Size(max = 2048)
        String sourceUrl

) {
}