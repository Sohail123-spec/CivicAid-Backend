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
import org.springframework.test.web.servlet.MvcResult;
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
class ProviderServiceProductApiTests {
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
    void clearProviderServiceProductTestData() {
        jdbcTemplate.update(
                "DELETE FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_PRODUCT'");
        jdbcTemplate.update(
                "DELETE FROM provider_service_products");
        jdbcTemplate.update(
                "DELETE FROM provider_service_categories");
        jdbcTemplate.update(
                "DELETE FROM provider_services");
        jdbcTemplate.update(
                "DELETE FROM provider_locations");
        jdbcTemplate.update(
                "DELETE FROM providers");
        jdbcTemplate.update(
                "DELETE FROM at_products "
                        + "WHERE code LIKE 'TEST_PSP_%'");
        jdbcTemplate.update(
                "DELETE FROM at_categories "
                        + "WHERE code LIKE 'TEST_PSP_%'");
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateServiceProductMapping() throws Exception {
        UUID providerId =
                createProvider("PSP_CREATE_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_CREATE_SERVICE");
        UUID categoryId =
                createCategory("PSP_CREATE_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_CREATE_PRODUCT");
        createCategoryMapping(serviceId, categoryId);
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productRequest(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$.providerServiceId")
                        .value(serviceId.toString()))
                .andExpect(jsonPath("$.atProductId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.atCategoryId")
                        .value(categoryId.toString()))
                .andExpect(jsonPath("$.rationale")
                        .value("This service may support this product type."))
                .andExpect(jsonPath("$.active").value(true));
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void duplicateServiceProductMappingIsRejected() throws Exception {
        UUID providerId =
                createProvider("PSP_DUP_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_DUP_SERVICE");
        UUID categoryId =
                createCategory("PSP_DUP_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_DUP_PRODUCT");
        createCategoryMapping(serviceId, categoryId);
        createMapping(serviceId, productId);
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productRequest(productId)))
                .andExpect(status().isConflict());
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void productMappingRequiresParentCategoryMapping() throws Exception {
        UUID providerId =
                createProvider("PSP_PARENT_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_PARENT_SERVICE");
        UUID categoryId =
                createCategory("PSP_PARENT_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_PARENT_PRODUCT");
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productRequest(productId)))
                .andExpect(status().isConflict());
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void inactiveParentCategoryMappingPreventsProductMappingCreation()
            throws Exception {
        UUID providerId =
                createProvider("PSP_INACTIVE_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_INACTIVE_SERVICE");
        UUID categoryId =
                createCategory("PSP_INACTIVE_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_INACTIVE_PRODUCT");
        UUID categoryMappingId =
                createCategoryMapping(serviceId, categoryId);
        mockMvc.perform(patch(
                        "/api/admin/provider-service-categories/{mappingId}/archive",
                        categoryMappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productRequest(productId)))
                .andExpect(status().isConflict());
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidServiceProductRequestIsRejected() throws Exception {
        UUID providerId =
                createProvider("PSP_INVALID_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_INVALID_SERVICE");
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "atProductId": null,
                            "rationale": ""
                        }
                        """))
                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void mappingUpdateChangesRationaleAndCreatesAuditEvent()
            throws Exception {
        UUID providerId =
                createProvider("PSP_UPDATE_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_UPDATE_SERVICE");
        UUID categoryId =
                createCategory("PSP_UPDATE_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_UPDATE_PRODUCT");
        createCategoryMapping(serviceId, categoryId);
        UUID mappingId =
                createMapping(serviceId, productId);
        mockMvc.perform(put(
                        "/api/admin/provider-service-products/{mappingId}",
                        mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "rationale": "Updated product-specific service rationale."
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rationale")
                        .value("Updated product-specific service rationale."));
        Integer updateEvents =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM audit_logs "
                                + "WHERE target_type = 'PROVIDER_SERVICE_PRODUCT' "
                                + "AND target_id = ? "
                                + "AND action_type = 'UPDATE'",
                        Integer.class,
                        mappingId);
        assertEquals(1, updateEvents);
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void noOpMappingUpdateDoesNotCreateAnotherAuditEvent()
            throws Exception {
        UUID providerId =
                createProvider("PSP_NOOP_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_NOOP_SERVICE");
        UUID categoryId =
                createCategory("PSP_NOOP_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_NOOP_PRODUCT");
        createCategoryMapping(serviceId, categoryId);
        UUID mappingId =
                createMapping(serviceId, productId);
        mockMvc.perform(put(
                        "/api/admin/provider-service-products/{mappingId}",
                        mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "rationale": "This service may support this product type."
                        }
                        """))
                .andExpect(status().isOk());
        Integer updateEvents =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM audit_logs "
                                + "WHERE target_type = 'PROVIDER_SERVICE_PRODUCT' "
                                + "AND target_id = ? "
                                + "AND action_type = 'UPDATE'",
                        Integer.class,
                        mappingId);
        assertEquals(0, updateEvents);
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void archiveAndActivateMappingWork() throws Exception {
        UUID providerId =
                createProvider("PSP_ACTIVE_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_ACTIVE_SERVICE");
        UUID categoryId =
                createCategory("PSP_ACTIVE_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_ACTIVE_PRODUCT");
        createCategoryMapping(serviceId, categoryId);
        UUID mappingId =
                createMapping(serviceId, productId);
        mockMvc.perform(patch(
                        "/api/admin/provider-service-products/{mappingId}/archive",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch(
                        "/api/admin/provider-service-products/{mappingId}/archive",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch(
                        "/api/admin/provider-service-products/{mappingId}/activate",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        Integer archiveEvents =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM audit_logs "
                                + "WHERE target_type = 'PROVIDER_SERVICE_PRODUCT' "
                                + "AND target_id = ? "
                                + "AND action_type = 'ARCHIVE'",
                        Integer.class,
                        mappingId);
        Integer activateEvents =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM audit_logs "
                                + "WHERE target_type = 'PROVIDER_SERVICE_PRODUCT' "
                                + "AND target_id = ? "
                                + "AND action_type = 'ACTIVATE'",
                        Integer.class,
                        mappingId);
        assertEquals(1, archiveEvents);
        assertEquals(1, activateEvents);
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanRetrieveMappingsByServiceAndProduct()
            throws Exception {
        UUID providerId =
                createProvider("PSP_RETRIEVE_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_RETRIEVE_SERVICE");
        UUID categoryId =
                createCategory("PSP_RETRIEVE_CATEGORY");
        UUID productId =
                createProduct(categoryId, "PSP_RETRIEVE_PRODUCT");
        createCategoryMapping(serviceId, categoryId);
        UUID mappingId =
                createMapping(serviceId, productId);
        assertNotNull(mappingId);
        mockMvc.perform(get(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(mappingId.toString()));
        mockMvc.perform(get(
                        "/api/admin/provider-service-products/products/{productId}",
                        productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(mappingId.toString()));
        mockMvc.perform(get(
                        "/api/admin/provider-service-products/{mappingId}",
                        mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(mappingId.toString()));
    }
    @Test
    void serviceProductAdminEndpointsRequireAdminAuthorization()
            throws Exception {
        UUID providerId =
                createProvider("PSP_AUTH_PROVIDER");
        UUID serviceId =
                createService(providerId, "PSP_AUTH_SERVICE");
        UUID categoryId =
                saveCategory(
                        "TEST_PSP_AUTH_CATEGORY",
                        true,
                        "VERIFIED",
                        LocalDate.now());
        UUID productId =
                saveProduct(
                        categoryId,
                        "TEST_PSP_AUTH_PRODUCT",
                        true,
                        "VERIFIED",
                        LocalDate.now());
        createCategoryMapping(serviceId, categoryId);
        String request =
                productRequest(productId);
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .with(user("ordinary-user").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void missingServiceIsRejected() throws Exception {
        UUID categoryId =
                createCategory("PSP_MISSING_SERVICE_CATEGORY");
        UUID productId =
                createProduct(
                        categoryId,
                        "PSP_MISSING_SERVICE_PRODUCT");
        UUID missingServiceId =
                UUID.randomUUID();
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        missingServiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productRequest(productId)))
                .andExpect(status().isNotFound());
    }
    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void missingProductIsRejected() throws Exception {
        UUID providerId =
                createProvider("PSP_MISSING_PRODUCT_PROVIDER");
        UUID serviceId =
                createService(
                        providerId,
                        "PSP_MISSING_PRODUCT_SERVICE");
        UUID missingProductId =
                UUID.randomUUID();
        mockMvc.perform(post(
                        "/api/admin/provider-service-products/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productRequest(missingProductId)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void publicDiscoveryReturnsOnlyEligibleMappings() throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_PUBLIC_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_PUBLIC_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_PUBLIC_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_PUBLIC_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productCode")
                        .value("TEST_PSP_PUBLIC_PRODUCT"))
                .andExpect(jsonPath("$[0].serviceCode")
                        .value("TEST_PSP_PUBLIC_SERVICE"));
    }
    @Test
    void staleServiceIsExcludedFromPublicDiscovery() throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_STALE_SERVICE_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_STALE_SERVICE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_STALE_SERVICE_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_STALE_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(91));
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void staleProductIsExcludedFromPublicDiscovery() throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_STALE_PRODUCT_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_STALE_PRODUCT_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_STALE_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(181));
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_STALE_PRODUCT_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void staleCategoryIsExcludedFromPublicDiscovery() throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_STALE_CATEGORY_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_STALE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(181));
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_STALE_CATEGORY_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_STALE_CATEGORY_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void inactiveMappingIsExcludedFromPublicDiscovery() throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_INACTIVE_MAPPING_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_INACTIVE_MAPPING_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_INACTIVE_MAPPING_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_INACTIVE_MAPPING_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                false);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void inactiveCategoryMappingIsExcludedFromPublicDiscovery()
            throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_INACTIVE_CATEGORY_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_INACTIVE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_INACTIVE_CATEGORY_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_INACTIVE_CATEGORY_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                false);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void unverifiedParentRecordsAreExcludedFromPublicDiscovery()
            throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_UNVERIFIED_PROVIDER",
                true,
                "UNVERIFIED",
                null);
        UUID categoryId = saveCategory(
                "TEST_PSP_UNVERIFIED_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_UNVERIFIED_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_UNVERIFIED_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void futureVerificationDateIsExcludedFromPublicDiscovery()
            throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_FUTURE_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_FUTURE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_FUTURE_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_FUTURE_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now().plusDays(1));
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void publicDiscoveryDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/provider-service-products"))
                .andExpect(status().isOk());
    }
    @Test
    void publicProductSpecificLookupReturnsOnlyEligibleMappings()
            throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_PRODUCT_LOOKUP_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_PRODUCT_LOOKUP_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID firstProductId = saveProduct(
                categoryId,
                "TEST_PSP_PRODUCT_LOOKUP_ONE",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID secondProductId = saveProduct(
                categoryId,
                "TEST_PSP_PRODUCT_LOOKUP_TWO",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_PRODUCT_LOOKUP_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                firstProductId,
                true);
        saveServiceProductMapping(
                serviceId,
                secondProductId,
                true);
        mockMvc.perform(
                        get(
                                "/api/provider-service-products/products/{productId}",
                                firstProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productCode")
                        .value("TEST_PSP_PRODUCT_LOOKUP_ONE"));
    }
    @Test
    void publicProductSpecificLookupHidesIneligibleMapping()
            throws Exception {
        UUID providerId = saveProvider(
                "TEST_PSP_PRODUCT_LOOKUP_STALE_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID categoryId = saveCategory(
                "TEST_PSP_PRODUCT_LOOKUP_STALE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID productId = saveProduct(
                categoryId,
                "TEST_PSP_PRODUCT_LOOKUP_STALE_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());
        UUID serviceId = saveService(
                providerId,
                "TEST_PSP_PRODUCT_LOOKUP_STALE_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(91));
        saveServiceCategoryMapping(
                serviceId,
                categoryId,
                true);
        saveServiceProductMapping(
                serviceId,
                productId,
                true);
        mockMvc.perform(
                        get(
                                "/api/provider-service-products/products/{productId}",
                                productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    private UUID createProvider(String code) throws Exception {
        MvcResult result =
                mockMvc.perform(
                        post("/api/admin/providers")
                                .with(user("admin@example.org")
                                        .roles("ADMIN"))
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
        String response =
                result.getResponse().getContentAsString();
        String id =
                com.fasterxml.jackson.databind.json.JsonMapper
                        .builder()
                        .build()
                        .readTree(response)
                        .get("id")
                        .asText();
        assertNotNull(id);
        return UUID.fromString(id);
    }
    private UUID createService(
            UUID providerId,
            String code) throws Exception {
        MvcResult result =
                mockMvc.perform(
                        post(
                                "/api/admin/provider-services/providers/{providerId}",
                                providerId)
                                .with(user("admin@example.org")
                                        .roles("ADMIN"))
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
        String response =
                result.getResponse().getContentAsString();
        String id =
                com.fasterxml.jackson.databind.json.JsonMapper
                        .builder()
                        .build()
                        .readTree(response)
                        .get("id")
                        .asText();
        assertNotNull(id);
        return UUID.fromString(id);
    }
    private UUID createCategory(String code) throws Exception {
        MvcResult result =
                mockMvc.perform(
                        post("/api/admin/at-categories")
                                .with(user("admin@example.org")
                                        .roles("ADMIN"))
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
        String response =
                result.getResponse().getContentAsString();
        String id =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();
        return UUID.fromString(id);
    }
    private UUID createProduct(
            UUID categoryId,
            String code) {
        return saveProduct(
                categoryId,
                "TEST_PSP_" + code,
                true,
                "VERIFIED",
                LocalDate.now());
    }
    private UUID createCategoryMapping(
            UUID serviceId,
            UUID categoryId) throws Exception {
        MvcResult result =
                mockMvc.perform(
                        post(
                                "/api/admin/provider-service-categories/services/{providerServiceId}",
                                serviceId)
                                .with(user("admin@example.org")
                                        .roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(categoryRequest(categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String response =
                result.getResponse().getContentAsString();
        String id =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();
        return UUID.fromString(id);
    }
    private UUID createMapping(
            UUID serviceId,
            UUID productId) throws Exception {
        MvcResult result =
                mockMvc.perform(
                        post(
                                "/api/admin/provider-service-products/services/{providerServiceId}",
                                serviceId)
                                .with(user("admin@example.org")
                                        .roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productRequest(productId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String response =
                result.getResponse().getContentAsString();
        String id =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();
        return UUID.fromString(id);
    }
    private String categoryRequest(UUID categoryId) {
        return """
                {
                    "atCategoryId": "%s",
                    "rationale": "This service may help users explore options in this category."
                }
                """.formatted(categoryId);
    }
    private String productRequest(UUID productId) {
        return """
                {
                    "atProductId": "%s",
                    "rationale": "This service may support this product type."
                }
                """.formatted(productId);
    }
    private UUID saveProvider(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO providers (
                    id,
                    code,
                    name,
                    provider_type,
                    description,
                    website_url,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                id,
                code,
                code + " Provider",
                "NGO",
                "Test provider description.",
                "https://example.org/" + code.toLowerCase(),
                "Official source",
                "https://example.org/" + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active);
        return id;
    }
    private UUID saveService(
            UUID providerId,
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO provider_services (
                    id,
                    provider_id,
                    code,
                    name,
                    service_type,
                    description,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                id,
                providerId,
                code,
                code + " Service",
                "ASSESSMENT_AND_FITTING",
                "Test provider service description.",
                "Official source",
                "https://example.org/" + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active);
        return id;
    }
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
                    description,
                    display_order,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
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
                    id,
                    at_category_id,
                    code,
                    name,
                    description,
                    considerations,
                    display_order,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
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
    private UUID saveServiceCategoryMapping(
            UUID serviceId,
            UUID categoryId,
            boolean active) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO provider_service_categories (
                    id,
                    provider_service_id,
                    at_category_id,
                    rationale,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                id,
                serviceId,
                categoryId,
                "This service may help users explore or maintain options in this category.",
                active);
        return id;
    }
    private UUID saveServiceProductMapping(
            UUID serviceId,
            UUID productId,
            boolean active) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO provider_service_products (
                    id,
                    provider_service_id,
                    at_product_id,
                    rationale,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                id,
                serviceId,
                productId,
                "This service may support this product type.",
                active);
        return id;
    }
}
