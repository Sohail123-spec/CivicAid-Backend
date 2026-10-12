package com.civicaid.backend.dto;

import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String displayName, String accountStatus) {
}
