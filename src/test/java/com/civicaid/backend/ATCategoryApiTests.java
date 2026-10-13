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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ATCategoryApiTests {

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
    void clearCategories() {
        jdbcTemplate.update("DELETE FROM verification_records WHERE at_category_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM at_categories");
    }

    @Test
    void publicDiscoveryReturnsOnlyActiveVerifiedFreshCategories() throws Exception {
        saveCategory("FRESH", true, "VERIFIED", LocalDate.now().minusDays(180));
        saveCategory("STALE", true, "VERIFIED", LocalDate.now().minusDays(181));
        saveCategory("UNVERIFIED", true, "UNVERIFIED", null);
        saveCategory("INACTIVE", false, "VERIFIED", LocalDate.now());

        mockMvc.perform(get("/api/at-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("FRESH"));
    }

    @Test
    void categoryCreationRequiresAdminAuthorization() throws Exception {
        mockMvc.perform(post("/api/admin/at-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest("MOBILITY")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/at-categories")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest("MOBILITY")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateAnUnverifiedCategory() throws Exception {
        mockMvc.perform(post("/api/admin/at-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest("VISION")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("VISION"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void substantiveAdminUpdateResetsVerification() throws Exception {
        UUID categoryId = saveCategory(
                "MOBILITY",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(put("/api/admin/at-categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated mobility category",
                                  "description": "Updated informational description.",
                                  "displayOrder": 0,
                                  "sourceName": "Updated official source",
                                  "sourceUrl": "https://example.org/updated-mobility",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanVerifyCategoryAndReadImmutableHistory() throws Exception {
        UUID categoryId = saveCategory("HEARING", true, "UNVERIFIED", null);

        mockMvc.perform(post("/api/admin/at-categories/{categoryId}/verifications", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": "VERIFIED",
                                  "sourceName": "Official assistive technology source",
                                  "sourceUrl": "https://example.org/hearing",
                                  "sourceVersion": "2026-10",
                                  "reviewNotes": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationResult").value("VERIFIED"))
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()));

        mockMvc.perform(get("/api/admin/at-categories/{categoryId}/verification-records", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sourceVersion").value("2026-10"));

        mockMvc.perform(get("/api/at-categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_id = ? AND action_type = 'VERIFY'",
                Integer.class,
                categoryId);
        org.junit.jupiter.api.Assertions.assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationRequiresNotesAndKeepsCategoryOutOfPublicDiscovery() throws Exception {
        UUID categoryId = saveCategory("COMMUNICATION", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(post("/api/admin/at-categories/{categoryId}/verifications", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": "REJECTED",
                                  "sourceName": "Official source",
                                  "sourceUrl": "https://example.org/communication",
                                  "sourceVersion": null,
                                  "reviewNotes": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/at-categories/{categoryId}/verifications", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": "REJECTED",
                                  "sourceName": "Official source",
                                  "sourceUrl": "https://example.org/communication",
                                  "sourceVersion": null,
                                  "reviewNotes": "The source no longer supports this category."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationResult").value("REJECTED"));

        mockMvc.perform(get("/api/at-categories/{categoryId}", categoryId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/at-categories/{categoryId}/verification-records", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewNotes").value("The source no longer supports this category."));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanListUnverifiedCategoriesForReview() throws Exception {
        saveCategory("REVIEW_CATEGORY", true, "UNVERIFIED", null);

        mockMvc.perform(get("/api/admin/at-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("REVIEW_CATEGORY"))
                .andExpect(jsonPath("$[0].verificationStatus").value("UNVERIFIED"));
    }

    @Test
    void categoryAdminListRequiresAdminAuthorization() throws Exception {
        mockMvc.perform(get("/api/admin/at-categories")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ordinary-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    private UUID saveCategory(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO at_categories (
                    id, code, name, display_order, source_name, source_url,
                    verification_status, last_verified_at, active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id,
                code,
                code + " category",
                0,
                "Official source",
                "https://example.org/" + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active);
        return id;
    }

    private String categoryRequest(String code) {
        return """
                {
                  "code": "%s",
                  "name": "%s category",
                  "description": "Informational category description.",
                  "displayOrder": 0,
                  "sourceName": "Official source",
                  "sourceUrl": "https://example.org/%s",
                  "active": true
                }
                """.formatted(code, code, code.toLowerCase());
    }
}
