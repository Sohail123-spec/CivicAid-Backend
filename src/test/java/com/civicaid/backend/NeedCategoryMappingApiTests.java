package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class NeedCategoryMappingApiTests {

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
    void clearNeedCategoryMappingTestData() {
        jdbcTemplate.update(
                "DELETE FROM audit_logs "
                        + "WHERE target_type = 'NEED_CATEGORY_MAPPING'");

        jdbcTemplate.update("DELETE FROM need_category_mappings");

        jdbcTemplate.update(
                "DELETE FROM at_categories "
                        + "WHERE code LIKE 'TEST_NCM\\_%'");

        jdbcTemplate.update(
                "DELETE FROM functional_needs "
                        + "WHERE code LIKE 'TEST_NCM\\_%'");
    }

    @Test
    void publicDiscoveryReturnsOnlyEligibleMappings() throws Exception {
        UUID activeNeedId = createFunctionalNeed(
                "TEST_NCM_ACTIVE_NEED",
                "Mobility Support",
                true,
                true,
                1);

        UUID inactiveNeedId = createFunctionalNeed(
                "TEST_NCM_INACTIVE_NEED",
                "Inactive Need",
                true,
                false,
                2);

        UUID nonSelectableNeedId = createFunctionalNeed(
                "TEST_NCM_NONSELECTABLE_NEED",
                "Non Selectable Need",
                false,
                true,
                3);

        UUID freshCategoryId = createCategory(
                "TEST_NCM_FRESH_CATEGORY",
                "Fresh Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID staleCategoryId = createCategory(
                "TEST_NCM_STALE_CATEGORY",
                "Stale Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now().minusDays(181));

        UUID unverifiedCategoryId = createCategory(
                "TEST_NCM_UNVERIFIED_CATEGORY",
                "Unverified Category",
                true,
                VerificationStatus.UNVERIFIED,
                null);

        UUID inactiveCategoryId = createCategory(
                "TEST_NCM_INACTIVE_CATEGORY",
                "Inactive Category",
                false,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID futureVerifiedCategoryId = createCategory(
                "TEST_NCM_FUTURE_CATEGORY",
                "Future Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now().plusDays(1));

        createMapping(activeNeedId, freshCategoryId, true);
        createMapping(activeNeedId, staleCategoryId, true);
        createMapping(activeNeedId, unverifiedCategoryId, true);
        createMapping(activeNeedId, inactiveCategoryId, true);
        createMapping(activeNeedId, futureVerifiedCategoryId, true);
        createMapping(inactiveNeedId, freshCategoryId, true);
        createMapping(nonSelectableNeedId, freshCategoryId, true);

        mockMvc.perform(get("/api/need-category-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].functionalNeedId")
                        .value(activeNeedId.toString()))
                .andExpect(jsonPath("$[0].atCategoryId")
                        .value(freshCategoryId.toString()))
                .andExpect(jsonPath("$[0].categoryName")
                        .value("Fresh Category"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void inactiveMappingIsExcludedFromPublicDiscovery() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_INACTIVE_MAPPING_NEED",
                "Inactive Mapping Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_INACTIVE_MAPPING_CATEGORY",
                "Inactive Mapping Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        createMapping(needId, categoryId, false);

        mockMvc.perform(get("/api/need-category-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void publicFilteringByFunctionalNeedReturnsOnlyEligibleMappings()
            throws Exception {

        UUID firstNeedId = createFunctionalNeed(
                "TEST_NCM_FILTER_NEED_ONE",
                "Filter Need One",
                true,
                true,
                1);

        UUID secondNeedId = createFunctionalNeed(
                "TEST_NCM_FILTER_NEED_TWO",
                "Filter Need Two",
                true,
                true,
                2);

        UUID firstCategoryId = createCategory(
                "TEST_NCM_FILTER_CATEGORY_ONE",
                "Filter Category One",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID secondCategoryId = createCategory(
                "TEST_NCM_FILTER_CATEGORY_TWO",
                "Filter Category Two",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        createMapping(firstNeedId, firstCategoryId, true);
        createMapping(firstNeedId, secondCategoryId, true);
        createMapping(secondNeedId, firstCategoryId, true);

        mockMvc.perform(get(
                        "/api/need-category-mappings/needs/{functionalNeedId}",
                        firstNeedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].functionalNeedId")
                        .value(firstNeedId.toString()))
                .andExpect(jsonPath("$[1].functionalNeedId")
                        .value(firstNeedId.toString()));
    }

    @Test
    void publicFilteringByCategoryReturnsOnlyEligibleMappings()
            throws Exception {

        UUID firstNeedId = createFunctionalNeed(
                "TEST_NCM_CATEGORY_FILTER_NEED_ONE",
                "Category Filter Need One",
                true,
                true,
                1);

        UUID secondNeedId = createFunctionalNeed(
                "TEST_NCM_CATEGORY_FILTER_NEED_TWO",
                "Category Filter Need Two",
                true,
                true,
                2);

        UUID targetCategoryId = createCategory(
                "TEST_NCM_CATEGORY_FILTER_TARGET",
                "Target Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID otherCategoryId = createCategory(
                "TEST_NCM_CATEGORY_FILTER_OTHER",
                "Other Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        createMapping(firstNeedId, targetCategoryId, true);
        createMapping(secondNeedId, targetCategoryId, true);
        createMapping(firstNeedId, otherCategoryId, true);

        mockMvc.perform(get(
                        "/api/need-category-mappings/categories/{atCategoryId}",
                        targetCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].atCategoryId")
                        .value(targetCategoryId.toString()))
                .andExpect(jsonPath("$[1].atCategoryId")
                        .value(targetCategoryId.toString()));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateMapping() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_CREATE_NEED",
                "Create Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_CREATE_CATEGORY",
                "Create Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        mockMvc.perform(post("/api/admin/need-category-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                needId,
                                categoryId,
                                "This category is relevant to this functional need.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.functionalNeedId")
                        .value(needId.toString()))
                .andExpect(jsonPath("$.functionalNeedLabel")
                        .value("Create Need"))
                .andExpect(jsonPath("$.atCategoryId")
                        .value(categoryId.toString()))
                .andExpect(jsonPath("$.categoryName")
                        .value("Create Category"))
                .andExpect(jsonPath("$.rationale")
                        .value("This category is relevant to this functional need."))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void duplicateMappingIsRejected() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_DUP_NEED",
                "Duplicate Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_DUP_CATEGORY",
                "Duplicate Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        createMapping(needId, categoryId, true);

        mockMvc.perform(post("/api/admin/need-category-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                needId,
                                categoryId,
                                "Duplicate mapping.")))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanUpdateMappingRationale() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_UPDATE_NEED",
                "Update Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_UPDATE_CATEGORY",
                "Update Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID mappingId = createMapping(needId, categoryId, true);

        mockMvc.perform(put(
                        "/api/admin/need-category-mappings/{mappingId}",
                        mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest("Updated rationale for this mapping.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mappingId.toString()))
                .andExpect(jsonPath("$.rationale")
                        .value("Updated rationale for this mapping."))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanArchiveAndActivateMapping() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_LIFECYCLE_NEED",
                "Lifecycle Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_LIFECYCLE_CATEGORY",
                "Lifecycle Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID mappingId = createMapping(needId, categoryId, true);

        mockMvc.perform(patch(
                        "/api/admin/need-category-mappings/{mappingId}/archive",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mappingId.toString()))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/need-category-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch(
                        "/api/admin/need-category-mappings/{mappingId}/activate",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mappingId.toString()))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/need-category-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void repeatedArchiveAndActivateAreIdempotent() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_IDEMPOTENT_NEED",
                "Idempotent Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_IDEMPOTENT_CATEGORY",
                "Idempotent Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        UUID mappingId = createMapping(needId, categoryId, true);

        mockMvc.perform(patch(
                        "/api/admin/need-category-mappings/{mappingId}/archive",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch(
                        "/api/admin/need-category-mappings/{mappingId}/archive",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch(
                        "/api/admin/need-category-mappings/{mappingId}/activate",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(patch(
                        "/api/admin/need-category-mappings/{mappingId}/activate",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        int auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'NEED_CATEGORY_MAPPING'",
                Integer.class);

        assertEquals(2, auditCount);
    }

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/need-category-mappings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/need-category-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Unauthorized request.")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotManageMappings() throws Exception {
        mockMvc.perform(get("/api/admin/need-category-mappings")
                        .with(user("user@example.org").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/need-category-mappings")
                        .with(user("user@example.org").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Non-admin request.")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void noOpUpdateDoesNotCreateAdditionalAuditEvent() throws Exception {
        UUID needId = createFunctionalNeed(
                "TEST_NCM_NOOP_NEED",
                "No Op Need",
                true,
                true,
                1);

        UUID categoryId = createCategory(
                "TEST_NCM_NOOP_CATEGORY",
                "No Op Category",
                true,
                VerificationStatus.VERIFIED,
                LocalDate.now());

        String rationale = "Original rationale.";

        UUID mappingId = createMapping(
                needId,
                categoryId,
                true,
                rationale);

        int auditCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'NEED_CATEGORY_MAPPING' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                mappingId);

        mockMvc.perform(put(
                        "/api/admin/need-category-mappings/{mappingId}",
                        mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(rationale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rationale").value(rationale));

        int auditCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'NEED_CATEGORY_MAPPING' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                mappingId);

        assertEquals(auditCountBefore, auditCountAfter);
    }

    private UUID createFunctionalNeed(
            String code,
            String label,
            boolean selectable,
            boolean active,
            int displayOrder) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO functional_needs "
                        + "(id, code, label, description, parent_need_id, "
                        + "is_selectable, display_order, active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                code,
                label,
                "Test functional need.",
                null,
                selectable,
                displayOrder,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createCategory(
            String code,
            String name,
            boolean active,
            VerificationStatus verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO at_categories "
                        + "(id, code, name, description, display_order, source_name, "
                        + "source_url, verification_status, last_verified_at, "
                        + "active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                code,
                name,
                "Test AT category.",
                1,
                "Test Source",
                null,
                verificationStatus.name(),
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createMapping(
            UUID functionalNeedId,
            UUID atCategoryId,
            boolean active) {

        return createMapping(
                functionalNeedId,
                atCategoryId,
                active,
                "This mapping explains the relationship.");
    }

    private UUID createMapping(
            UUID functionalNeedId,
            UUID atCategoryId,
            boolean active,
            String rationale) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO need_category_mappings "
                        + "(id, functional_need_id, at_category_id, rationale, "
                        + "active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                functionalNeedId,
                atCategoryId,
                rationale,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private String createRequest(
            UUID functionalNeedId,
            UUID atCategoryId,
            String rationale) {

        return "{"
                + "\"functionalNeedId\":\"" + functionalNeedId + "\","
                + "\"atCategoryId\":\"" + atCategoryId + "\","
                + "\"rationale\":\"" + rationale + "\""
                + "}";
    }

    private String updateRequest(String rationale) {
        return "{"
                + "\"rationale\":\"" + rationale + "\""
                + "}";
    }
}