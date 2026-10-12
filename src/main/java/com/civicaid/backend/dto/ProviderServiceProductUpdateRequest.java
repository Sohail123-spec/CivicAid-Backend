package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderServiceProductUpdateRequest(
        @NotBlank @Size(max = 500) String rationale
) {
}