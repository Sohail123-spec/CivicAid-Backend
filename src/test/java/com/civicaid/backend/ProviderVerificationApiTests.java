package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderVerificationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

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
    void clearProviders() {
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE provider_id IS NOT NULL");
        jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE target_type = 'PROVIDER'");
        jdbcTemplate.update("DELETE FROM providers");
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanVerifyProvider() throws Exception {
        UUID providerId = createProvider(
                "VERIFY_PROVIDER",
                "Provider To Verify");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Official Provider Registry",
                                        "https://example.org/registry/provider",
                                        "2026-10",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.providerId")
                                .value(providerId.toString()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationResult")
                                .value("VERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.sourceName")
                                .value("Official Provider Registry"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.sourceUrl")
                                .value("https://example.org/registry/provider"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.sourceVersion")
                                .value("2026-10"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.reviewNotes")
                                .doesNotExist())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.performedAt")
                                .exists());

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM providers WHERE id = ?",
                String.class,
                providerId);

        LocalDate lastVerifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM providers WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getDate(1) == null
                        ? null
                        : resultSet.getDate(1).toLocalDate(),
                providerId);

        assertEquals("VERIFIED", verificationStatus);
        assertEquals(LocalDate.now(), lastVerifiedAt);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(1, verificationCount);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND target_id = ? "
                        + "AND action_type = 'VERIFY'",
                Integer.class,
                providerId.toString());

        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationRequiresReviewNotes() throws Exception {
        UUID providerId = createProvider(
                "REJECTED_NO_NOTES",
                "Rejected Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "REJECTED",
                                        "Official Provider Registry",
                                        "https://example.org/registry/provider",
                                        "2026-10",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(0, verificationCount);

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM providers WHERE id = ?",
                String.class,
                providerId);

        assertEquals("UNVERIFIED", verificationStatus);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationWithReviewNotesCreatesHistory() throws Exception {
        UUID providerId = createProvider(
                "REJECTED_WITH_NOTES",
                "Rejected Provider With Notes");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "REJECTED",
                                        "Official Provider Registry",
                                        "https://example.org/registry/provider",
                                        "2026-10",
                                        "Source could not verify the provider details.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationResult")
                                .value("REJECTED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.reviewNotes")
                                .value("Source could not verify the provider details."));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM providers WHERE id = ?",
                String.class,
                providerId);

        LocalDate lastVerifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM providers WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getDate(1) == null
                        ? null
                        : resultSet.getDate(1).toLocalDate(),
                providerId);

        assertEquals("UNVERIFIED", verificationStatus);
        assertEquals(null, lastVerifiedAt);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(1, verificationCount);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND target_id = ? "
                        + "AND action_type = 'REJECT_VERIFICATION'",
                Integer.class,
                providerId.toString());

        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void revokedVerificationRequiresReviewNotes() throws Exception {
        UUID providerId = createProvider(
                "REVOKED_PROVIDER",
                "Revoked Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "REVOKED",
                                        "Official Provider Registry",
                                        "https://example.org/registry/provider",
                                        "2026-10",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(0, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void revokedVerificationWithReviewNotesCreatesHistory() throws Exception {
        UUID providerId = createProvider(
                "REVOKE_PROVIDER",
                "Provider To Revoke");

        jdbcTemplate.update(
                "UPDATE providers "
                        + "SET verification_status = 'VERIFIED', last_verified_at = ? "
                        + "WHERE id = ?",
                LocalDate.now().minusDays(10),
                providerId);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "REVOKED",
                                        "Official Provider Registry",
                                        "https://example.org/registry/provider",
                                        "2026-10",
                                        "Provider information is no longer current.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationResult")
                                .value("REVOKED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.reviewNotes")
                                .value("Provider information is no longer current."));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM providers WHERE id = ?",
                String.class,
                providerId);

        LocalDate lastVerifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM providers WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getDate(1) == null
                        ? null
                        : resultSet.getDate(1).toLocalDate(),
                providerId);

        assertEquals("UNVERIFIED", verificationStatus);
        assertEquals(null, lastVerifiedAt);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(1, verificationCount);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND target_id = ? "
                        + "AND action_type = 'REVOKE_VERIFICATION'",
                Integer.class,
                providerId.toString());

        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void verificationHistoryReturnsNewestFirst() throws Exception {
        UUID providerId = createProvider(
                "HISTORY_PROVIDER",
                "History Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "First Source",
                                        "https://example.org/first",
                                        "v1",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "REJECTED",
                                        "Second Source",
                                        "https://example.org/second",
                                        "v2",
                                        "Second verification did not confirm the provider.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}/verification-records",
                                        providerId))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.length()")
                                .value(2))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].providerId")
                                .value(providerId.toString()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].verificationResult")
                                .value("REJECTED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].sourceName")
                                .value("Second Source"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[1].verificationResult")
                                .value("VERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[1].sourceName")
                                .value("First Source"));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void verificationHistoryIsImmutableAfterProviderVerification() throws Exception {
        UUID providerId = createProvider(
                "IMMUTABLE_HISTORY",
                "Immutable History Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Original Source",
                                        "https://example.org/original",
                                        "v1",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "New Source",
                                        "https://example.org/new",
                                        "v2",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}/verification-records",
                                        providerId))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.length()")
                                .value(2))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[1].sourceName")
                                .value("Original Source"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[1].sourceUrl")
                                .value("https://example.org/original"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[1].sourceVersion")
                                .value("v1"));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void verificationRequiresValidSourceUrl() throws Exception {
        UUID providerId = createProvider(
                "INVALID_SOURCE_URL",
                "Invalid Source URL Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Official Source",
                                        "http://example.org/insecure",
                                        "v1",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(0, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void unknownProviderVerificationReturnsNotFound() throws Exception {
        UUID providerId = UUID.randomUUID();

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Official Source",
                                        "https://example.org/source",
                                        "v1",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isNotFound());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}/verification-records",
                                        providerId))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isNotFound());
    }

            @Test
        @WithMockUser(username = "admin@example.org", roles = "ADMIN")
        void providerVerificationRequiresAdminAuthorization() throws Exception {
        UUID providerId = createProvider(
                "VERIFICATION_AUTH",
                "Verification Authorization Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .anonymous())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Official Source",
                                        "https://example.org/source",
                                        "v1",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isUnauthorized());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .user("ordinary-user")
                                                .roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Official Source",
                                        "https://example.org/source",
                                        "v1",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isForbidden());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}/verification-records",
                                        providerId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .anonymous()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isUnauthorized());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}/verification-records",
                                        providerId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .user("ordinary-user")
                                                .roles("USER")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidVerificationResultIsRejected() throws Exception {
        UUID providerId = createProvider(
                "INVALID_RESULT",
                "Invalid Result Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "result": "INVALID_RESULT",
                                            "sourceName": "Official Source",
                                            "sourceUrl": "https://example.org/source",
                                            "sourceVersion": "v1"
                                        }
                                        """))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(0, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void missingSourceNameIsRejected() throws Exception {
        UUID providerId = createProvider(
                "MISSING_SOURCE_NAME",
                "Missing Source Name Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "result": "VERIFIED",
                                            "sourceName": "",
                                            "sourceUrl": "https://example.org/source",
                                            "sourceVersion": "v1"
                                        }
                                        """))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_id = ?",
                Integer.class,
                providerId.toString());

        assertEquals(0, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void verificationHistoryContainsExpectedFields() throws Exception {
        UUID providerId = createProvider(
                "HISTORY_FIELDS",
                "History Fields Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers/{providerId}/verifications",
                                        providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verificationRequest(
                                        "VERIFIED",
                                        "Official Source",
                                        "https://example.org/source",
                                        "2026.10",
                                        null)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}/verification-records",
                                        providerId))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].id")
                                .exists())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].providerId")
                                .value(providerId.toString()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].verificationResult")
                                .value("VERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].sourceName")
                                .value("Official Source"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].sourceUrl")
                                .value("https://example.org/source"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].sourceVersion")
                                .value("2026.10"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].reviewNotes")
                                .doesNotExist())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].performedAt")
                                .exists());
    }

    private UUID createProvider(String code, String name) throws Exception {
        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(code, name)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(
                jsonMapper.readTree(response).get("id").asText());
    }

    private String providerRequest(String code, String name) {
        return """
                {
                    "code": "%s",
                    "name": "%s",
                    "providerType": "NGO",
                    "description": "Provides assistive technology support.",
                    "websiteUrl": "https://example.org/provider",
                    "sourceName": "Official provider source",
                    "sourceUrl": "https://example.org/source"
                }
                """.formatted(code, name);
    }

    private String verificationRequest(
            String result,
            String sourceName,
            String sourceUrl,
            String sourceVersion,
            String reviewNotes) {

        String reviewNotesJson = reviewNotes == null
                ? ""
                : ",\n                    \"reviewNotes\": \""
                        + reviewNotes
                        + "\"";

        return """
                {
                    "result": "%s",
                    "sourceName": "%s",
                    "sourceUrl": "%s",
                    "sourceVersion": "%s"%s
                }
                """.formatted(
                result,
                sourceName,
                sourceUrl,
                sourceVersion,
                reviewNotesJson);
    }
}