package com.civicaid.backend.dto;

public record AuthResponse(String token, UserSummary user) {
    public record UserSummary(String email, String displayName) {
    }
}
