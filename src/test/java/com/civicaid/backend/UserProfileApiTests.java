package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.security.AuthenticatedUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for PUT /api/users/me (profile update) and
 * DELETE /api/users/me (account deletion).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;

    @BeforeEach
    void createTestUser() {
        testUser = userRepository.findByEmail("profile-test@example.org")
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail("profile-test@example.org");
                    u.setDisplayName("Original Name");
                    u.setPasswordHash(passwordEncoder.encode("Test@1234"));
                    u.setAccountStatus(AccountStatus.ACTIVE);
                    return userRepository.save(u);
                });
        // Reset display name between tests.
        testUser.setDisplayName("Original Name");
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE verifier_user_id = ?",
                testUser.getId());
        jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE actor_user_id = ?",
                testUser.getId());
        userRepository.findByEmail("profile-test@example.org")
                .ifPresent(userRepository::delete);
    }

    private RequestPostProcessor authenticatedUser(String email) {
        return authentication(authenticationFor(email));
    }

    private Authentication authenticationFor(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        AuthenticatedUser principal = new AuthenticatedUser(user);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
    }

    // ─── GET /api/users/me ────────────────────────────────────────────────────

    @Test
    void getProfileReturnsCurrentUserDetails() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(authenticatedUser(testUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-test@example.org"))
                .andExpect(jsonPath("$.displayName").value("Original Name"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void getProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── PUT /api/users/me ────────────────────────────────────────────────────

    @Test
    void updateProfileUpdatesDisplayName() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .with(authenticatedUser(testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("profile-test@example.org"));
    }

    @Test
    void updateProfileRejectsBlankDisplayName() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .with(authenticatedUser(testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfileRejectsDisplayNameExceeding100Characters() throws Exception {
        String longName = "A".repeat(101);
        mockMvc.perform(put("/api/users/me")
                        .with(authenticatedUser(testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Should Not Work\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── DELETE /api/users/me ─────────────────────────────────────────────────

    @Test
    void deleteAccountSucceedsForUserWithNoHistory() throws Exception {
        // Ensure testUser has no audit or verification references.
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE verifier_user_id = ?",
                testUser.getId());
        jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE actor_user_id = ?",
                testUser.getId());

        mockMvc.perform(delete("/api/users/me")
                        .with(authenticatedUser(testUser.getEmail())))
                .andExpect(status().isNoContent());

        // User row should be gone.
        org.junit.jupiter.api.Assertions.assertFalse(
                userRepository.existsById(testUser.getId()),
                "User row should have been deleted");
    }

    @Test
    void deleteAccountReturns409WhenReferencedInAuditLog() throws Exception {
        // Seed an audit log row referencing testUser as actor.
        UUID targetId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO audit_logs "
                + "(id, actor_user_id, target_type, target_id, action_type, occurred_at) "
                + "VALUES (gen_random_uuid(), ?, 'USER_ACCOUNT', ?, 'CREATE', NOW())",
                testUser.getId(), targetId);

        mockMvc.perform(delete("/api/users/me")
                        .with(authenticatedUser(testUser.getEmail())))
                .andExpect(status().isConflict());

        // User row must still exist.
        org.junit.jupiter.api.Assertions.assertTrue(
                userRepository.existsById(testUser.getId()),
                "User row must not have been deleted when audit history exists");
    }

    @Test
    void deleteAccountRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
