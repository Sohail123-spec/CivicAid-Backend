package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProviderServiceCategoryCreateRequest(
        @NotNull UUID atCategoryId,
        @NotBlank @Size(max = 500) String rationale
) {
}