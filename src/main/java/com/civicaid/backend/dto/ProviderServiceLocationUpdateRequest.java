package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderServiceLocationUpdateRequest(

        @NotBlank
        @Size(max = 200)
        String sourceName,

        @Pattern(regexp = "^https://\\S+$")
        @Size(max = 2048)
        String sourceUrl

) {
}