package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchemeApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void ensureAdminActorExists() {
        userRepository.findByEmail("admin@example.org").orElseGet(() -> {
            User admin = new User();
            admin.setEmail("admin@example.org");
            admin.setPasswordHash("test-only-password-hash");
            admin.setAccountStatus(AccountStatus.ACTIVE);
            return userRepository.save(admin);
        });
    }

    @AfterEach
    void clearSchemes() {
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE scheme_id IS NOT NULL");
        jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE target_type IN "
                        + "('SCHEME', 'SCHEME_CATEGORY', 'SCHEME_PRODUCT')");
        jdbcTemplate.update("DELETE FROM schemes");
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanVerifySchemeAndReadVerificationHistory() throws Exception {
        UUID schemeId = saveScheme(
                "VERIFY_SCHEME", true, "UNVERIFIED", null);

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "VERIFIED", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("VERIFIED"));

        mockMvc.perform(get(
                        "/api/admin/schemes/{schemeId}/verification-records",
                        schemeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].result").value("VERIFIED"))
                .andExpect(jsonPath("$[0].sourceName")
                        .value("Official government source"));

        mockMvc.perform(get("/api/schemes/{schemeId}", schemeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records WHERE scheme_id = ?",
                Integer.class,
                schemeId);

        assertEquals(1, recordCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationRequiresNotesAndHidesScheme() throws Exception {
        UUID schemeId = saveScheme(
                "REJECT_SCHEME", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest("REJECTED", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "REJECTED",
                                "The official source does not support this scheme.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("REJECTED"));

        mockMvc.perform(get("/api/schemes/{schemeId}", schemeId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/admin/schemes/{schemeId}/verification-records",
                        schemeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewNotes")
                        .value("The official source does not support this scheme."));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void revocationRequiresNotesAndAddsAnotherHistoryRecord() throws Exception {
        UUID schemeId = saveScheme(
                "REVOKE_SCHEME", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest("REVOKED", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "REVOKED", "Verification has been withdrawn.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("REVOKED"));

        mockMvc.perform(get("/api/schemes/{schemeId}", schemeId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/admin/schemes/{schemeId}/verification-records",
                        schemeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void publicDiscoveryExcludesStaleInactiveAndUnverifiedSchemes()
            throws Exception {

        saveScheme(
                "FRESH_SCHEME", true, "VERIFIED", LocalDate.now());
        saveScheme(
                "STALE_SCHEME", true, "VERIFIED",
                LocalDate.now().minusDays(91));
        saveScheme(
                "INACTIVE_SCHEME", false, "VERIFIED", LocalDate.now());
        saveScheme(
                "UNVERIFIED_SCHEME", true, "UNVERIFIED", null);

        mockMvc.perform(get("/api/schemes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("FRESH_SCHEME"));
    }

    @Test
    void schemeVerificationEndpointsRequireAdminAuthorization()
            throws Exception {

        UUID schemeId = saveScheme(
                "PROTECTED_SCHEME", true, "UNVERIFIED", null);

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest("VERIFIED", null)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(
                        "/api/admin/schemes/{schemeId}/verifications", schemeId)
                        .with(user("ordinary-user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest("VERIFIED", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                        "/api/admin/schemes/{schemeId}/verification-records",
                        schemeId)
                        .with(user("ordinary-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    private UUID saveScheme(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO schemes (
                    id, code, name, authority_name, description,
                    benefits_summary, eligibility_summary,
                    required_documents_summary, application_method,
                    application_instructions, official_source_url,
                    official_application_url, jurisdiction_level,
                    applicable_state_code, source_version,
                    effective_from, effective_to, verification_status,
                    last_verified_at, active, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                id,
                code,
                code + " scheme",
                "Government Authority",
                "Description of the scheme.",
                "Benefits provided by the scheme.",
                "Eligibility criteria for applicants.",
                "Identity and supporting documents.",
                "ONLINE",
                "Apply through the official website.",
                "https://example.org/" + code.toLowerCase(),
                "https://example.org/apply",
                "NATIONAL",
                null,
                "2026-10",
                null,
                null,
                verificationStatus,
                lastVerifiedAt,
                active);

        return id;
    }

    private String verificationRequest(String result, String reviewNotes) {
        String notesJson = reviewNotes == null
                ? "null"
                : "\"" + reviewNotes.replace("\\", "\\\\")
                        .replace("\"", "\\\"") + "\"";

        return """
                {
                    "result": "%s",
                    "reviewNotes": %s,
                    "sourceName": "Official government source",
                    "sourceUrl": "https://example.org/official-scheme",
                    "sourceVersion": "2026-10"
                }
                """.formatted(result, notesJson);
    }
}
