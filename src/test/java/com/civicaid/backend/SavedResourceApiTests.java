package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.security.AuthenticatedUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SavedResourceApiTests {

    private static final String USER_ONE =
            "saved-resource-user-one@example.org";

    private static final String USER_TWO =
            "saved-resource-user-two@example.org";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void ensureTestUsersExist() {
        ensureUser(USER_ONE);
        ensureUser(USER_TWO);
    }

    @AfterEach
    void clearSavedResourceTestData() {

        jdbcTemplate.update(
                "DELETE FROM saved_resources "
                        + "WHERE user_id IN ("
                        + "SELECT id FROM users WHERE email IN (?, ?)"
                        + ")",
                USER_ONE,
                USER_TWO);

        jdbcTemplate.update(
                "DELETE FROM provider_service_locations "
                        + "WHERE provider_service_id IN ("
                        + "SELECT id FROM provider_services "
                        + "WHERE code LIKE 'TEST_SAVED_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM provider_services "
                        + "WHERE code LIKE 'TEST_SAVED_%'");

        jdbcTemplate.update(
                "DELETE FROM provider_locations "
                        + "WHERE provider_id IN ("
                        + "SELECT id FROM providers "
                        + "WHERE code LIKE 'TEST_SAVED_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM providers "
                        + "WHERE code LIKE 'TEST_SAVED_%'");

        jdbcTemplate.update(
                "DELETE FROM at_products "
                        + "WHERE code LIKE 'TEST_SAVED_%'");

        jdbcTemplate.update(
                "DELETE FROM at_categories "
                        + "WHERE code LIKE 'TEST_SAVED_%'");

        jdbcTemplate.update(
                "DELETE FROM schemes "
                        + "WHERE code LIKE 'TEST_SAVED_%'");

        jdbcTemplate.update(
                "DELETE FROM users WHERE email IN (?, ?)",
                USER_ONE,
                USER_TWO);
    }

    @Test
    void unauthenticatedGetReturns401() throws Exception {

        mockMvc.perform(
                        get("/api/users/me/saved-resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPostReturns401() throws Exception {

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteReturns401() throws Exception {

        mockMvc.perform(
                        delete(
                                "/api/users/me/saved-resources/{savedResourceId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanListOwnSavedResources()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_LIST_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                categoryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].resourceType")
                                .value("AT_CATEGORY"))
                .andExpect(
                        jsonPath("$[0].resourceId")
                                .value(categoryId.toString()));
    }

    @Test
    void saveAtCategory() throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                categoryId)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resourceType")
                                .value("AT_CATEGORY"))
                .andExpect(
                        jsonPath("$.resourceId")
                                .value(categoryId.toString()));
    }

    @Test
    void saveAtProduct() throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_PRODUCT_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID productId = createProduct(
                categoryId,
                "TEST_SAVED_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_PRODUCT",
                                                productId)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resourceType")
                                .value("AT_PRODUCT"))
                .andExpect(
                        jsonPath("$.resourceId")
                                .value(productId.toString()));
    }

    @Test
    void saveScheme() throws Exception {

        UUID schemeId = createScheme(
                "TEST_SAVED_SCHEME",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "SCHEME",
                                                schemeId)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resourceType")
                                .value("SCHEME"))
                .andExpect(
                        jsonPath("$.resourceId")
                                .value(schemeId.toString()));
    }

    @Test
    void saveProvider() throws Exception {

        UUID providerId = createProvider(
                "TEST_SAVED_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "PROVIDER",
                                                providerId)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resourceType")
                                .value("PROVIDER"))
                .andExpect(
                        jsonPath("$.resourceId")
                                .value(providerId.toString()));
    }

    @Test
    void saveProviderLocation() throws Exception {

        UUID providerId = createProvider(
                "TEST_SAVED_LOCATION_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID locationId = createProviderLocation(
                providerId,
                "TEST_SAVED_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "PROVIDER_LOCATION",
                                                locationId)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resourceType")
                                .value("PROVIDER_LOCATION"))
                .andExpect(
                        jsonPath("$.resourceId")
                                .value(locationId.toString()));
    }

    @Test
    void saveProviderServiceLocation()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_SAVED_PSL_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID locationId = createProviderLocation(
                providerId,
                "TEST_SAVED_PSL_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceId = createProviderService(
                providerId,
                "TEST_SAVED_PSL_SERVICE",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceLocationId =
                createProviderServiceLocation(
                        serviceId,
                        locationId,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "PROVIDER_SERVICE_LOCATION",
                                                serviceLocationId)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resourceType")
                                .value(
                                        "PROVIDER_SERVICE_LOCATION"))
                .andExpect(
                        jsonPath("$.resourceId")
                                .value(
                                        serviceLocationId.toString()));
    }

    @Test
    void duplicateSaveReturns409() throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_DUPLICATE",
                true,
                "VERIFIED",
                LocalDate.now());

        String request =
                requestJson(
                        "AT_CATEGORY",
                        categoryId);

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void nonexistentTargetReturns404() throws Exception {

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleTargetCannotBeSaved() throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_STALE",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(181));

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                categoryId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveTargetCannotBeSaved() throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_INACTIVE",
                false,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                categoryId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unverifiedTargetCannotBeSaved()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_UNVERIFIED",
                true,
                "UNVERIFIED",
                null);

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                categoryId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCanDeleteOwnSavedResource()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_DELETE",
                true,
                "VERIFIED",
                LocalDate.now());

        String response =
                mockMvc.perform(
                                post(
                                        "/api/users/me/saved-resources")
                                        .with(
                                                authenticatedUser(
                                                        USER_ONE))
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content(
                                                requestJson(
                                                        "AT_CATEGORY",
                                                        categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String savedResourceId =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();

        mockMvc.perform(
                        delete(
                                "/api/users/me/saved-resources/{savedResourceId}",
                                savedResourceId)
                                .with(
                                        authenticatedUser(
                                                USER_ONE)))
                .andExpect(status().isNoContent());

        assertEquals(
                0,
                countSavedResourcesForUser(USER_ONE));
    }

    @Test
    void userCannotDeleteAnotherUsersSavedResource()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_CROSS_DELETE",
                true,
                "VERIFIED",
                LocalDate.now());

        String response =
                mockMvc.perform(
                                post(
                                        "/api/users/me/saved-resources")
                                        .with(
                                                authenticatedUser(
                                                        USER_ONE))
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content(
                                                requestJson(
                                                        "AT_CATEGORY",
                                                        categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String savedResourceId =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();

        mockMvc.perform(
                        delete(
                                "/api/users/me/saved-resources/{savedResourceId}",
                                savedResourceId)
                                .with(
                                        authenticatedUser(
                                                USER_TWO)))
                .andExpect(status().isNotFound());

        assertEquals(
                1,
                countSavedResourcesForUser(USER_ONE));
    }

    @Test
    void usersCannotSeeEachOthersSavedResources()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_PRIVATE",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post(
                                "/api/users/me/saved-resources")
                                .with(
                                        authenticatedUser(
                                                USER_ONE))
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        requestJson(
                                                "AT_CATEGORY",
                                                categoryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/users/me/saved-resources")
                                .with(
                                        authenticatedUser(
                                                USER_TWO)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()").value(0));
    }

    @Test
    void invalidRequestReturns400()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(
                                        authenticatedUser(
                                                USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "resourceType": null,
                                            "resourceId": null
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownershipCannotBeControlledByClientInput()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_SAVED_CLIENT_OWNER",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/saved-resources")
                                .with(
                                        authenticatedUser(
                                                USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "resourceType": "AT_CATEGORY",
                                            "resourceId": "%s",
                                            "userId": "%s"
                                        }
                                        """.formatted(
                                        categoryId,
                                        USER_TWO)))
                .andExpect(status().isCreated());

        UUID userOneId = userId(USER_ONE);
        UUID userTwoId = userId(USER_TWO);

        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM saved_resources "
                                + "WHERE user_id = ? "
                                + "AND at_category_id = ?",
                        Integer.class,
                        userOneId,
                        categoryId));

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM saved_resources "
                                + "WHERE user_id = ? "
                                + "AND at_category_id = ?",
                        Integer.class,
                        userTwoId,
                        categoryId));
    }

    private RequestPostProcessor authenticatedUser(
            String email) {

        return authentication(
                authenticationFor(email));
    }

    private Authentication authenticationFor(
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        AuthenticatedUser principal =
                new AuthenticatedUser(user);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
    }

    private void ensureUser(String email) {

        userRepository.findByEmail(email).orElseGet(() -> {

            User user = new User();
            user.setEmail(email);
            user.setPasswordHash("test-only-password-hash");
            user.setAccountStatus(AccountStatus.ACTIVE);

            return userRepository.save(user);
        });
    }

    private UUID userId(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow()
                .getId();
    }

    private int countSavedResourcesForUser(
            String email) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM saved_resources "
                        + "WHERE user_id = ?",
                Integer.class,
                userId(email));
    }

    private UUID createCategory(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
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
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                code,
                code + " category",
                "Test assistive technology category.",
                1,
                "Test source",
                "https://example.org/"
                        + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createProduct(
            UUID categoryId,
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
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
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                categoryId,
                code,
                code + " product",
                "Test assistive technology product.",
                null,
                1,
                "Test source",
                "https://example.org/"
                        + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createScheme(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO schemes (
                    id,
                    code,
                    name,
                    authority_name,
                    description,
                    benefits_summary,
                    eligibility_summary,
                    required_documents_summary,
                    application_method,
                    application_instructions,
                    official_source_url,
                    official_application_url,
                    jurisdiction_level,
                    applicable_state_code,
                    source_version,
                    effective_from,
                    effective_to,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?
                )
                """,
                id,
                code,
                code + " Scheme",
                "Test Authority",
                "Test scheme description.",
                "Test benefits summary.",
                "Test eligibility summary.",
                "Test documents summary.",
                "ONLINE",
                "Apply through the official channel.",
                "https://example.org/schemes/"
                        + code.toLowerCase(),
                "https://example.org/apply/"
                        + code.toLowerCase(),
                "NATIONAL",
                null,
                "TEST",
                null,
                null,
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createProvider(
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
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
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                code,
                code + " Provider",
                "NGO",
                "Test provider description.",
                "https://example.org/providers/"
                        + code.toLowerCase(),
                "Test source",
                "https://example.org/source/"
                        + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createProviderLocation(
            UUID providerId,
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO provider_locations (
                    id,
                    provider_id,
                    address_line_1,
                    address_line_2,
                    locality,
                    city,
                    state_code,
                    postal_code,
                    country_code,
                    latitude,
                    longitude,
                    public_phone,
                    public_email,
                    accessibility_notes,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?
                )
                """,
                id,
                providerId,
                "1 Test Street",
                null,
                "Test Locality",
                "Hyderabad",
                "TS",
                "500001",
                "IN",
                null,
                null,
                "0401234567",
                null,
                "Test accessibility information.",
                "Test source",
                "https://example.org/locations/"
                        + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createProviderService(
            UUID providerId,
            String code,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
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
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                providerId,
                code,
                code + " Service",
                "ASSESSMENT_AND_FITTING",
                "Test provider service.",
                "Test source",
                "https://example.org/services/"
                        + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createProviderServiceLocation(
            UUID serviceId,
            UUID locationId,
            boolean active,
            String verificationStatus,
            LocalDate lastVerifiedAt) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO provider_service_locations (
                    id,
                    provider_service_id,
                    provider_location_id,
                    source_name,
                    source_url,
                    verification_status,
                    last_verified_at,
                    active,
                    created_at,
                    updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                serviceId,
                locationId,
                "Test source",
                "https://example.org/service-locations/"
                        + id,
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private String requestJson(
            String resourceType,
            UUID resourceId) {

        return """
                {
                    "resourceType": "%s",
                    "resourceId": "%s"
                }
                """.formatted(
                resourceType,
                resourceId);
    }
}
