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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ATProductApiTests {

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
    void clearProducts() {
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE at_product_id IS NOT NULL");
        jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE target_type = 'AT_PRODUCT'");
        jdbcTemplate.update("DELETE FROM at_products");
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE at_category_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM at_categories");
    }

    @Test
    void publicDiscoveryReturnsOnlyActiveVerifiedFreshProductsInEligibleCategories()
            throws Exception {

        UUID categoryId = saveCategory(
                "MOBILITY", true, "VERIFIED", LocalDate.now());

        saveProduct(categoryId, "FRESH_PRODUCT", true,
                "VERIFIED", LocalDate.now());

        saveProduct(categoryId, "STALE_PRODUCT", true,
                "VERIFIED", LocalDate.now().minusDays(181));

        saveProduct(categoryId, "UNVERIFIED_PRODUCT", true,
                "UNVERIFIED", null);

        saveProduct(categoryId, "INACTIVE_PRODUCT", false,
                "VERIFIED", LocalDate.now());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("FRESH_PRODUCT"));
    }

    @Test
    void productsInUnverifiedCategoriesAreNotPubliclyDiscoverable()
            throws Exception {

        UUID categoryId = saveCategory(
                "HEARING", true, "UNVERIFIED", null);

        saveProduct(categoryId, "HEARING_PRODUCT", true,
                "VERIFIED", LocalDate.now());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void individualLookupHidesStaleAndUnverifiedProducts()
            throws Exception {

        UUID categoryId = saveCategory(
                "VISION", true, "VERIFIED", LocalDate.now());

        UUID staleId = saveProduct(
                categoryId, "STALE_LOOKUP", true,
                "VERIFIED", LocalDate.now().minusDays(181));

        UUID unverifiedId = saveProduct(
                categoryId, "UNVERIFIED_LOOKUP", true,
                "UNVERIFIED", null);

        mockMvc.perform(get("/api/products/{id}", staleId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products/{id}", unverifiedId))
                .andExpect(status().isNotFound());
    }

    @Test
    void productCreationRequiresAdminAuthorization() throws Exception {
        UUID categoryId = saveCategory(
                "COMMUNICATION", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(post(
                        "/api/admin/at-categories/{id}/products", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("COMM_DEVICE")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(
                        "/api/admin/at-categories/{id}/products", categoryId)
                        .with(user("ordinary-user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("COMM_DEVICE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateAnUnverifiedProduct() throws Exception {
        UUID categoryId = saveCategory(
                "MOBILITY", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(post(
                        "/api/admin/at-categories/{id}/products", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("WALKING_AID")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WALKING_AID"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void substantiveProductUpdateResetsVerification() throws Exception {
        UUID categoryId = saveCategory(
                "MOBILITY", true, "VERIFIED", LocalDate.now());

        UUID productId = saveProduct(
                categoryId, "UPDATE_PRODUCT", true,
                "VERIFIED", LocalDate.now());

        mockMvc.perform(put("/api/admin/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated walking aid",
                                  "description": "Updated product description.",
                                  "considerations": "Requires individual suitability assessment.",
                                  "displayOrder": 1,
                                  "sourceName": "Updated official source",
                                  "sourceUrl": "https://example.org/updated-product"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanVerifyProductAndReadVerificationHistory() throws Exception {
        UUID categoryId = saveCategory(
                "MOBILITY", true, "VERIFIED", LocalDate.now());

        UUID productId = saveProduct(
                categoryId, "VERIFY_PRODUCT", true,
                "UNVERIFIED", null);

        mockMvc.perform(post(
                        "/api/admin/products/{id}/verifications", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": "VERIFIED",
                                  "sourceName": "Official assistive technology source",
                                  "sourceUrl": "https://example.org/product",
                                  "sourceVersion": "2026-10",
                                  "reviewNotes": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationResult").value("VERIFIED"))
                .andExpect(jsonPath("$.productId").value(productId.toString()));

        mockMvc.perform(get(
                        "/api/admin/products/{id}/verification-records", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sourceVersion").value("2026-10"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records WHERE at_product_id = ?",
                Integer.class,
                productId);

        assertEquals(1, recordCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationRequiresNotesAndHidesProduct() throws Exception {
        UUID categoryId = saveCategory(
                "VISION", true, "VERIFIED", LocalDate.now());

        UUID productId = saveProduct(
                categoryId, "REJECT_PRODUCT", true,
                "VERIFIED", LocalDate.now());

        mockMvc.perform(post(
                        "/api/admin/products/{id}/verifications", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": "REJECTED",
                                  "sourceName": "Official source",
                                  "sourceUrl": "https://example.org/product",
                                  "sourceVersion": null,
                                  "reviewNotes": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/admin/products/{id}/verifications", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": "REJECTED",
                                  "sourceName": "Official source",
                                  "sourceUrl": "https://example.org/product",
                                  "sourceVersion": null,
                                  "reviewNotes": "The source no longer supports this product."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationResult").value("REJECTED"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/admin/products/{id}/verification-records", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewNotes")
                        .value("The source no longer supports this product."));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidProductRequestIsRejected() throws Exception {
        UUID categoryId = saveCategory(
                "MOBILITY", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(post(
                        "/api/admin/at-categories/{id}/products", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "invalid code",
                                  "name": "",
                                  "description": "",
                                  "considerations": null,
                                  "displayOrder": -1,
                                  "sourceName": "",
                                  "sourceUrl": "http://insecure.example.org"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void archivingProductRemovesItFromPublicDiscoveryAndAuditsOnce() throws Exception {
        UUID categoryId = saveCategory("MOBILITY", true, "VERIFIED", LocalDate.now());
        UUID productId = saveProduct(categoryId, "ARCHIVE_PRODUCT", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(patch("/api/admin/products/{productId}/archive", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/admin/products/{productId}/archive", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Integer archiveEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'AT_PRODUCT' "
                        + "AND target_id = ? AND action_type = 'ARCHIVE'",
                Integer.class, productId);
        assertEquals(1, archiveEvents);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void activatingUnverifiedProductDoesNotMakeItPubliclyDiscoverable() throws Exception {
        UUID categoryId = saveCategory("MOBILITY", true, "VERIFIED", LocalDate.now());
        UUID productId = saveProduct(categoryId, "ACTIVATE_UNVERIFIED", false, "UNVERIFIED", null);

        mockMvc.perform(patch("/api/admin/products/{productId}/activate", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").doesNotExist());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Integer activationEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'AT_PRODUCT' "
                        + "AND target_id = ? AND action_type = 'ACTIVATE'",
                Integer.class, productId);
        assertEquals(1, activationEvents);
    }

    @Test
    void productActivationAndArchivingRequireAdminAuthorization() throws Exception {
        UUID categoryId = saveCategory("MOBILITY", true, "VERIFIED", LocalDate.now());
        UUID productId = saveProduct(categoryId, "PROTECTED_PRODUCT", true, "VERIFIED", LocalDate.now());

        mockMvc.perform(patch("/api/admin/products/{productId}/archive", productId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/admin/products/{productId}/activate", productId)
                        .with(user("ordinary-user").roles("USER")))
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
                    id, code, name, description, display_order,
                    source_name, source_url, verification_status,
                    last_verified_at, active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id,
                code,
                code + " category",
                "Test category description.",
                0,
                "Official source",
                "https://example.org/" + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active);

        return id;
    }

    private UUID saveProduct(
            UUID categoryId,
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO at_products (
                    id, at_category_id, code, name, description,
                    considerations, display_order, source_name, source_url,
                    verification_status, last_verified_at, active,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id,
                categoryId,
                code,
                code + " product",
                "Test assistive technology product description.",
                null,
                0,
                "Official source",
                "https://example.org/" + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active);

        return id;
    }

    private String productRequest(String code) {
        return """
                {
                  "code": "%s",
                  "name": "%s product",
                  "description": "Assistive technology product description.",
                  "considerations": null,
                  "displayOrder": 0,
                  "sourceName": "Official source",
                  "sourceUrl": "https://example.org/%s"
                }
                """.formatted(code, code, code.toLowerCase());
    }
}

