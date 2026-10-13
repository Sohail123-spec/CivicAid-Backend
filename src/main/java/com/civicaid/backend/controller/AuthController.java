package com.civicaid.backend.controller;

import com.civicaid.backend.dto.FunctionalNeedResponse;
import com.civicaid.backend.dto.FunctionalNeedSelectionRequest;
import com.civicaid.backend.dto.LoginRequest;
import com.civicaid.backend.dto.RegisterRequest;
import com.civicaid.backend.dto.UserProfileResponse;
import com.civicaid.backend.dto.UserProfileUpdateRequest;
import com.civicaid.backend.security.AuthenticatedUser;
import com.civicaid.backend.service.AuthService;
import com.civicaid.backend.service.UserFunctionalNeedService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final UserFunctionalNeedService userFunctionalNeedService;

    public AuthController(
            AuthService authService,
            UserFunctionalNeedService userFunctionalNeedService) {

        this.authService = authService;
        this.userFunctionalNeedService = userFunctionalNeedService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileResponse> me(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required");
        }

        return ResponseEntity.ok(
                authService.getCurrentUserProfile(principal));
    }

    /**
     * Updates the authenticated user's display name.
     * Returns the updated profile. Email and account status are immutable by
     * the user.
     */
    @PutMapping("/users/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required");
        }

        return ResponseEntity.ok(
                authService.updateCurrentUserProfile(principal, request));
    }

    /**
     * Permanently deletes the authenticated user's account and all owned
     * data. Returns 204 No Content on success, or 409 Conflict when the
     * account is referenced in immutable audit or verification history.
     */
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required");
        }

        authService.deleteCurrentUserAccount(principal);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/functional-needs")
    public ResponseEntity<List<FunctionalNeedResponse>> getMyFunctionalNeeds(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required");
        }

        return ResponseEntity.ok(
                userFunctionalNeedService.getMyFunctionalNeeds(principal));
    }

    @PutMapping("/users/me/functional-needs")
    public ResponseEntity<List<FunctionalNeedResponse>> replaceMyFunctionalNeeds(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody FunctionalNeedSelectionRequest request) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required");
        }

        return ResponseEntity.ok(
                userFunctionalNeedService.replaceMyFunctionalNeeds(
                        principal,
                        request));
    }
}