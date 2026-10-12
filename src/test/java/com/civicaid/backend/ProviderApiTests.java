
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
class ProviderApiTests {

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
                "DELETE FROM audit_logs WHERE target_type = 'PROVIDER'");
        jdbcTemplate.update("DELETE FROM providers");
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateAnUnverifiedProvider() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "TEST_PROVIDER",
                                        "Test Assistive Provider")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isCreated())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.code").value("TEST_PROVIDER"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.name").value("Test Assistive Provider"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.providerType").value("NGO"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.lastVerifiedAt").doesNotExist())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.active").value(true));

        Integer providerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM providers WHERE code = ?",
                Integer.class,
                "TEST_PROVIDER");

        assertEquals(1, providerCount);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND action_type = 'CREATE'",
                Integer.class);

        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void duplicateProviderCodeIsRejected() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "DUPLICATE_PROVIDER", "First Provider")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isCreated());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "DUPLICATE_PROVIDER", "Second Provider")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isConflict());

        Integer providerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM providers WHERE code = ?",
                Integer.class,
                "DUPLICATE_PROVIDER");

        assertEquals(1, providerCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void duplicateProviderNameIsRejected() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "PROVIDER_ONE", "Same Provider Name")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isCreated());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "PROVIDER_TWO", "Same Provider Name")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isConflict());

        Integer providerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM providers WHERE name = ?",
                Integer.class,
                "Same Provider Name");

        assertEquals(1, providerCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidProviderRequestIsRejected() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "code": "invalid code",
                                            "name": "",
                                            "providerType": "NGO",
                                            "description": "",
                                            "websiteUrl": "http://insecure.example.org",
                                            "sourceName": "",
                                            "sourceUrl": "http://insecure.example.org"
                                        }
                                        """))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isBadRequest());
    }

    @Test
    void providerCreationRequiresAdminAuthorization() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "AUTH_PROVIDER",
                                        "Authorization Test Provider")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isUnauthorized());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .user("ordinary-user").roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "AUTH_PROVIDER",
                                        "Authorization Test Provider")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanListAndRetrieveProviders() throws Exception {
        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/providers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerRequest(
                                        "LOOKUP_PROVIDER", "Provider Lookup Test")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID providerId = UUID.fromString(
                jsonMapper.readTree(response).get("id").asText());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$[0].code").value("LOOKUP_PROVIDER"));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/admin/providers/{providerId}", providerId))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.id").value(providerId.toString()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.name").value("Provider Lookup Test"));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanUpdateProvider() throws Exception {
        UUID providerId = createProvider(
                "UPDATE_PROVIDER", "Original Provider Name");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerUpdateRequest(
                                        "Updated Provider Name",
                                        "Updated provider description.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.id").value(providerId.toString()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.code").value("UPDATE_PROVIDER"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.name").value("Updated Provider Name"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.description")
                                .value("Updated provider description."))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.lastVerifiedAt").doesNotExist());

        Integer updateAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                providerId.toString());

        assertEquals(1, updateAuditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void changingVerifiedProviderInformationResetsVerification() throws Exception {
        UUID providerId = createProvider(
                "RESET_VERIFICATION_PROVIDER",
                "Verified Provider Before Update");

        LocalDate previousVerificationDate = LocalDate.now().minusDays(10);

        jdbcTemplate.update(
                "UPDATE providers "
                        + "SET verification_status = 'VERIFIED', last_verified_at = ? "
                        + "WHERE id = ?",
                previousVerificationDate,
                providerId);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerUpdateRequest(
                                        "Verified Provider After Update",
                                        "Updated provider information.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.name")
                                .value("Verified Provider After Update"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.lastVerifiedAt").doesNotExist());

        String storedVerificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM providers WHERE id = ?",
                String.class,
                providerId);

        LocalDate storedVerificationDate = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM providers WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getDate(1) == null
                        ? null
                        : resultSet.getDate(1).toLocalDate(),
                providerId);

        assertEquals("UNVERIFIED", storedVerificationStatus);
        assertEquals(null, storedVerificationDate);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void unchangedProviderUpdateDoesNotCreateAuditOrResetVerification()
            throws Exception {
        UUID providerId = createProvider(
                "NO_OP_UPDATE_PROVIDER",
                "No Op Provider");

        LocalDate previousVerificationDate = LocalDate.now().minusDays(10);

        jdbcTemplate.update(
                "UPDATE providers "
                        + "SET verification_status = 'VERIFIED', last_verified_at = ? "
                        + "WHERE id = ?",
                previousVerificationDate,
                providerId);

        Integer auditCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                providerId.toString());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", providerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(unchangedProviderUpdateRequest(
                                        "No Op Provider")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.name").value("No Op Provider"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.lastVerifiedAt")
                                .value(previousVerificationDate.toString()));

        Integer auditCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                providerId.toString());

        assertEquals(auditCountBefore, auditCountAfter);

        String storedVerificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM providers WHERE id = ?",
                String.class,
                providerId);

        LocalDate storedVerificationDate = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM providers WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getDate(1) == null
                        ? null
                        : resultSet.getDate(1).toLocalDate(),
                providerId);

        assertEquals("VERIFIED", storedVerificationStatus);
        assertEquals(previousVerificationDate, storedVerificationDate);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void updatingProviderToAnotherProvidersNameIsRejected() throws Exception {
        createProvider("UPDATE_FIRST", "First Existing Provider");

        UUID secondProviderId = createProvider(
                "UPDATE_SECOND", "Second Existing Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", secondProviderId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerUpdateRequest(
                                        "First Existing Provider",
                                        "Updated description.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void updatingUnknownProviderReturnsNotFound() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerUpdateRequest(
                                        "Updated Provider Name",
                                        "Updated description.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void providerUpdateRequiresAdminAuthorization() throws Exception {
        UUID providerId = createProvider(
                "UPDATE_AUTH_PROVIDER", "Update Authorization Provider");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", providerId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .anonymous())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerUpdateRequest(
                                        "Updated Authorization Provider",
                                        "Updated description.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isUnauthorized());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/admin/providers/{providerId}", providerId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors
                                                .user("ordinary-user").roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(providerUpdateRequest(
                                        "Updated Authorization Provider",
                                        "Updated description.")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isForbidden());
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

    private String providerUpdateRequest(String name, String description) {
        return """
                {
                    "name": "%s",
                    "providerType": "NGO",
                    "description": "%s",
                    "websiteUrl": "https://example.org/updated-provider",
                    "sourceName": "Official provider source",
                    "sourceUrl": "https://example.org/updated-source"
                }
                """.formatted(name, description);
    }

    private String unchangedProviderUpdateRequest(String name) {
        return """
                {
                    "name": "%s",
                    "providerType": "NGO",
                    "description": "Provides assistive technology support.",
                    "websiteUrl": "https://example.org/provider",
                    "sourceName": "Official provider source",
                    "sourceUrl": "https://example.org/source"
                }
                """.formatted(name);
    }
}
