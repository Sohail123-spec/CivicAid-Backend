package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.UserRepository;

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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
class ProviderServiceCategoryApiTests {

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
    void clearProviderServiceCategoryTestData() {

        /*
         * Remove audit records first because provider, service,
         * category and mapping records may be referenced by audit logs.
         */
        jdbcTemplate.update(
                "DELETE FROM audit_logs "
                        + "WHERE target_type IN ("
                        + "'PROVIDER_SERVICE_CATEGORY', "
                        + "'PROVIDER_SERVICE', "
                        + "'PROVIDER', "
                        + "'AT_CATEGORY'"
                        + ")");

        /*
         * Remove mapping records before their parent records.
         */
        jdbcTemplate.update("DELETE FROM provider_service_categories");

        /*
         * Remove provider/service/location records created by these tests.
         */
        jdbcTemplate.update("DELETE FROM provider_services");
        jdbcTemplate.update("DELETE FROM provider_locations");
        jdbcTemplate.update("DELETE FROM providers");

        /*
         * Remove only the test AT categories created by this test class.
         */
        jdbcTemplate.update(
                "DELETE FROM at_categories "
                        + "WHERE code LIKE 'TEST_PSC\\_%'");
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateServiceCategoryMapping() throws Exception {

        UUID providerId = createProvider("PSC_CREATE_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_CREATE_SERVICE");
        UUID categoryId = createCategory("PSC_CREATE_CATEGORY");

        mockMvc.perform(post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryRequest(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(providerId.toString()))
                .andExpect(jsonPath("$.providerServiceId").value(serviceId.toString()))
                .andExpect(jsonPath("$.atCategoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.rationale").value(
                        "This service may help users explore this category."))
                .andExpect(jsonPath("$.active").value(true));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_categories "
                        + "WHERE provider_service_id = ? "
                        + "AND at_category_id = ?",
                Integer.class,
                serviceId,
                categoryId);

        assertEquals(1, count);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_CATEGORY' "
                        + "AND action_type = 'CREATE'",
                Integer.class);

        assertEquals(1, auditCount);
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void duplicateServiceCategoryMappingIsRejected() throws Exception {

        UUID providerId = createProvider("PSC_DUPLICATE_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_DUPLICATE_SERVICE");
        UUID categoryId = createCategory("PSC_DUPLICATE_CATEGORY");

        mockMvc.perform(post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryRequest(categoryId)))
                .andExpect(status().isCreated());

        /*
         * Duplicate mapping is a conflict with the existing
         * provider-service/category relationship.
         */
        mockMvc.perform(post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryRequest(categoryId)))
                .andExpect(status().isConflict());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_categories "
                        + "WHERE provider_service_id = ? "
                        + "AND at_category_id = ?",
                Integer.class,
                serviceId,
                categoryId);

        assertEquals(1, count);
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidServiceCategoryRequestIsRejected() throws Exception {

        UUID providerId = createProvider("PSC_INVALID_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_INVALID_SERVICE");

        mockMvc.perform(post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "atCategoryId": null,
                            "rationale": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_categories",
                Integer.class);

        assertEquals(0, count);
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void mappingUpdateChangesRationaleAndCreatesAuditEvent()
            throws Exception {

        UUID providerId = createProvider("PSC_UPDATE_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_UPDATE_SERVICE");
        UUID categoryId = createCategory("PSC_UPDATE_CATEGORY");

        UUID mappingId = createMapping(serviceId, categoryId);

        mockMvc.perform(put(
                        "/api/admin/provider-service-categories/{mappingId}",
                        mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "rationale": "Updated rationale for this service category relationship."
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rationale").value(
                        "Updated rationale for this service category relationship."));

        String rationale = jdbcTemplate.queryForObject(
                "SELECT rationale FROM provider_service_categories "
                        + "WHERE id = ?",
                String.class,
                mappingId);

        assertEquals(
                "Updated rationale for this service category relationship.",
                rationale);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_CATEGORY' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                mappingId);

        assertEquals(1, auditCount);
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void noOpMappingUpdateDoesNotCreateAnotherAuditEvent()
            throws Exception {

        UUID providerId = createProvider("PSC_NOOP_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_NOOP_SERVICE");
        UUID categoryId = createCategory("PSC_NOOP_CATEGORY");

        UUID mappingId = createMapping(serviceId, categoryId);

        Integer auditCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_CATEGORY' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                mappingId);

        mockMvc.perform(put(
                        "/api/admin/provider-service-categories/{mappingId}",
                        mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "rationale": "This service may help users explore this category."
                        }
                        """))
                .andExpect(status().isOk());

        Integer auditCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_CATEGORY' "
                        + "AND target_id = ? "
                        + "AND action_type = 'UPDATE'",
                Integer.class,
                mappingId);

        assertEquals(auditCountBefore, auditCountAfter);
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void archiveAndActivateMappingWork() throws Exception {

        UUID providerId = createProvider("PSC_ACTIVE_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_ACTIVE_SERVICE");
        UUID categoryId = createCategory("PSC_ACTIVE_CATEGORY");

        UUID mappingId = createMapping(serviceId, categoryId);

        mockMvc.perform(patch(
                        "/api/admin/provider-service-categories/{mappingId}/archive",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Boolean archivedStatus = jdbcTemplate.queryForObject(
                "SELECT active FROM provider_service_categories WHERE id = ?",
                Boolean.class,
                mappingId);

        assertEquals(Boolean.FALSE, archivedStatus);

        mockMvc.perform(patch(
                        "/api/admin/provider-service-categories/{mappingId}/activate",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        Boolean activeStatus = jdbcTemplate.queryForObject(
                "SELECT active FROM provider_service_categories WHERE id = ?",
                Boolean.class,
                mappingId);

        assertEquals(Boolean.TRUE, activeStatus);
    }


    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanRetrieveMappingsByServiceAndCategory() throws Exception {

        UUID providerId = createProvider("PSC_RETRIEVE_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_RETRIEVE_SERVICE");
        UUID categoryId = createCategory("PSC_RETRIEVE_CATEGORY");

        UUID mappingId = createMapping(serviceId, categoryId);

        assertNotNull(mappingId);

        mockMvc.perform(get(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(mappingId.toString()));

        mockMvc.perform(get(
                        "/api/admin/provider-service-categories/categories/{categoryId}",
                        categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(mappingId.toString()));

        mockMvc.perform(get(
                        "/api/admin/provider-service-categories/{mappingId}",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mappingId.toString()));
    }


    @Test
    void serviceCategoryAdminEndpointsRequireAdminAuthorization()
            throws Exception {

        UUID providerId = createProvider("PSC_AUTH_PROVIDER");
        UUID serviceId = createService(providerId, "PSC_AUTH_SERVICE");

        /*
         * Do not call the protected AT-category API here.
         * Insert the prerequisite category directly so that this test
         * specifically tests authorization of the provider-service-category
         * endpoint.
         */
        UUID categoryId = saveCategory(
                "TEST_PSC_AUTH",
                true,
                "VERIFIED",
                LocalDate.now());

        String request = categoryRequest(categoryId);

        /*
         * No authentication -> 401 Unauthorized.
         */
        mockMvc.perform(post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());

        /*
         * Authenticated USER role -> 403 Forbidden.
         */
        mockMvc.perform(post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        serviceId)
                .with(user("ordinary-user").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isForbidden());
    }


    private UUID createProvider(String code) throws Exception {

        MvcResult result = mockMvc.perform(
                post("/api/admin/providers")
                        .with(user("admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "%s",
                                    "name": "%s Assistive Services",
                                    "providerType": "NGO",
                                    "sourceName": "Official provider directory",
                                    "sourceUrl": "https://example.org/providers"
                                }
                                """.formatted(code, code)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        String id = com.fasterxml.jackson.databind.json.JsonMapper
                .builder()
                .build()
                .readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);

        return UUID.fromString(id);
    }


    private UUID createService(UUID providerId, String code)
            throws Exception {

        MvcResult result = mockMvc.perform(
                post(
                        "/api/admin/provider-services/providers/{providerId}",
                        providerId)
                        .with(user("admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "%s",
                                    "name": "%s Service",
                                    "serviceType": "ASSESSMENT_AND_FITTING",
                                    "description": "Assistive technology service",
                                    "sourceName": "Official provider directory",
                                    "sourceUrl": "https://example.org/services"
                                }
                                """.formatted(code, code)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        String id = com.fasterxml.jackson.databind.json.JsonMapper
                .builder()
                .build()
                .readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);

        return UUID.fromString(id);
    }


    private UUID createCategory(String code) throws Exception {

        MvcResult result = mockMvc.perform(
                post("/api/admin/at-categories")
                        .with(user("admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "%s",
                                    "name": "%s category",
                                    "description": "Informational category description.",
                                    "displayOrder": 0,
                                    "sourceName": "Official source",
                                    "sourceUrl": "https://example.org/%s",
                                    "active": true
                                }
                                """.formatted(
                                code,
                                code,
                                code.toLowerCase())))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        return UUID.fromString(
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString());
    }


    /*
     * Direct database helper used only by authorization tests.
     *
     * This avoids calling another protected admin endpoint while testing
     * authorization for ProviderServiceCategory endpoints.
     */
    private UUID saveCategory(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO at_categories (
                    id,
                    code,
                    name,
                    display_order,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
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


    private UUID createMapping(
            UUID providerServiceId,
            UUID categoryId) throws Exception {

        MvcResult result = mockMvc.perform(
                post(
                        "/api/admin/provider-service-categories/services/{providerServiceId}",
                        providerServiceId)
                        .with(user("admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest(categoryId)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        String id = com.fasterxml.jackson.databind.json.JsonMapper
                .builder()
                .build()
                .readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);

        return UUID.fromString(id);
    }


    private String categoryRequest(UUID atCategoryId) {

        return """
                {
                    "atCategoryId": "%s",
                    "rationale": "This service may help users explore this category."
                }
                """.formatted(atCategoryId);
    }
}