package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FunctionalNeedSelectionRequest(
        @NotEmpty(message = "At least one functional need is required")
        List<
                @NotBlank(message = "Functional need code is required")
                @Size(max = 64, message = "Functional need code must be 64 characters or fewer")
                String> functionalNeedCodes
) {
}
