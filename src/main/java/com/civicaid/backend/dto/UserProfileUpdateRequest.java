package com.civicaid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/users/me.
 * Only display name can be updated by the user;
 * email and account status are immutable by the user.
 */
public record UserProfileUpdateRequest(
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName) {
}
