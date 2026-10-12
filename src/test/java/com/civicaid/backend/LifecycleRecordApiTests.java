package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.security.AuthenticatedUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
class LifecycleRecordApiTests {

    private static final String USER_ONE =
            "lifecycle-user-one@example.org";

    private static final String USER_TWO =
            "lifecycle-user-two@example.org";

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
    void clearLifecycleTestData() {

        jdbcTemplate.update(
                "DELETE FROM lifecycle_records "
                        + "WHERE user_id IN ("
                        + "SELECT id FROM users WHERE email IN (?, ?)"
                        + ") "
                        + "AND replaces_lifecycle_record_id IS NOT NULL",
                USER_ONE,
                USER_TWO);

        jdbcTemplate.update(
                "DELETE FROM lifecycle_records "
                        + "WHERE user_id IN ("
                        + "SELECT id FROM users WHERE email IN (?, ?)"
                        + ")",
                USER_ONE,
                USER_TWO);

        jdbcTemplate.update(
                "DELETE FROM provider_service_locations "
                        + "WHERE provider_service_id IN ("
                        + "SELECT id FROM provider_services "
                        + "WHERE code LIKE 'TEST_LIFECYCLE_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM provider_services "
                        + "WHERE code LIKE 'TEST_LIFECYCLE_%'");

        jdbcTemplate.update(
                "DELETE FROM provider_locations "
                        + "WHERE provider_id IN ("
                        + "SELECT id FROM providers "
                        + "WHERE code LIKE 'TEST_LIFECYCLE_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM providers "
                        + "WHERE code LIKE 'TEST_LIFECYCLE_%'");

        jdbcTemplate.update(
                "DELETE FROM at_products "
                        + "WHERE code LIKE 'TEST_LIFECYCLE_%'");

        jdbcTemplate.update(
                "DELETE FROM at_categories "
                        + "WHERE code LIKE 'TEST_LIFECYCLE_%'");

        jdbcTemplate.update(
                "DELETE FROM users WHERE email IN (?, ?)",
                USER_ONE,
                USER_TWO);
    }

    @Test
    void unauthenticatedGetReturns401() throws Exception {

        mockMvc.perform(
                        get("/api/users/me/lifecycle-records"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPostReturns401() throws Exception {

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validLifecycleJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteReturns401() throws Exception {

        mockMvc.perform(
                        delete(
                                "/api/users/me/lifecycle-records/{lifecycleRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateCategoryBasedLifecycleRecord()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "My Mobility Device",
                                                LocalDate.now().minusDays(10),
                                                categoryId,
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(
                        jsonPath("$.itemName")
                                .value("My Mobility Device"))
                .andExpect(
                        jsonPath("$.atCategoryId")
                                .value(categoryId.toString()))
                .andExpect(
                        jsonPath("$.atProductId")
                                .doesNotExist())
                .andExpect(
                        jsonPath("$.lifecycleStatus")
                                .value("IN_USE"))
                .andExpect(
                        jsonPath("$.userId")
                                .doesNotExist());
    }

    @Test
    void authenticatedUserCanCreateProductBasedLifecycleRecord()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_PRODUCT_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID productId = createProduct(
                categoryId,
                "TEST_LIFECYCLE_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "My Product",
                                                LocalDate.now().minusDays(20),
                                                null,
                                                productId,
                                                null,
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.atProductId")
                                .value(productId.toString()))
                .andExpect(
                        jsonPath("$.atCategoryId")
                                .doesNotExist());
    }

    @Test
    void authenticatedUserCanCreateLifecycleRecordWithPreferredServiceLocation()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_LIFECYCLE_PROVIDER",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID locationId = createProviderLocation(
                providerId,
                "TEST_LIFECYCLE_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceId = createProviderService(
                providerId,
                "TEST_LIFECYCLE_SERVICE",
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
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Serviced Device",
                                                LocalDate.now().minusDays(5),
                                                null,
                                                null,
                                                serviceLocationId,
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.preferredProviderServiceLocationId")
                                .value(serviceLocationId.toString()));
    }

    @Test
    void authenticatedUserCanListOwnLifecycleRecords()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_LIST",
                true,
                "VERIFIED",
                LocalDate.now());

        createLifecycleRecord(
                USER_ONE,
                "Listed Device",
                LocalDate.now().minusDays(3),
                categoryId,
                null,
                null,
                null);

        mockMvc.perform(
                        get("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].itemName")
                                .value("Listed Device"))
                .andExpect(
                        jsonPath("$[0].userId")
                                .doesNotExist());
    }

    @Test
    void authenticatedUserCanGetOwnLifecycleRecord()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_GET",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID lifecycleRecordId = createLifecycleRecord(
                USER_ONE,
                "Retrievable Device",
                LocalDate.now().minusDays(4),
                categoryId,
                null,
                null,
                null);

        mockMvc.perform(
                        get(
                                "/api/users/me/lifecycle-records/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(lifecycleRecordId.toString()))
                .andExpect(
                        jsonPath("$.itemName")
                                .value("Retrievable Device"));
    }

    @Test
    void userCannotGetAnotherUsersLifecycleRecord()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_CROSS_GET",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID lifecycleRecordId = createLifecycleRecord(
                USER_ONE,
                "Private Device",
                LocalDate.now().minusDays(6),
                categoryId,
                null,
                null,
                null);

        mockMvc.perform(
                        get(
                                "/api/users/me/lifecycle-records/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCanDeleteOwnLifecycleRecord()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_DELETE",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID lifecycleRecordId = createLifecycleRecord(
                USER_ONE,
                "Deletable Device",
                LocalDate.now().minusDays(7),
                categoryId,
                null,
                null,
                null);

        mockMvc.perform(
                        delete(
                                "/api/users/me/lifecycle-records/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isNoContent());

        assertEquals(
                0,
                countLifecycleRecordsForUser(USER_ONE));
    }

    @Test
    void userCannotDeleteAnotherUsersLifecycleRecord()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_CROSS_DELETE",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID lifecycleRecordId = createLifecycleRecord(
                USER_ONE,
                "Protected Device",
                LocalDate.now().minusDays(8),
                categoryId,
                null,
                null,
                null);

        mockMvc.perform(
                        delete(
                                "/api/users/me/lifecycle-records/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());

        assertEquals(
                1,
                countLifecycleRecordsForUser(USER_ONE));
    }

    @Test
    void bothProductAndCategoryReferencesReturn400()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_BOTH_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID productId = createProduct(
                categoryId,
                "TEST_LIFECYCLE_BOTH_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Invalid Catalog Reference",
                                                LocalDate.now().minusDays(2),
                                                categoryId,
                                                productId,
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonexistentCategoryReturns404()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Unknown Category",
                                                LocalDate.now().minusDays(2),
                                                UUID.randomUUID(),
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveCategoryCannotBeReferenced()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_INACTIVE_CATEGORY",
                false,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Inactive Category Device",
                                                LocalDate.now().minusDays(2),
                                                categoryId,
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleCategoryCannotBeReferenced()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_STALE_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(181));

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Stale Category Device",
                                                LocalDate.now().minusDays(2),
                                                categoryId,
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unverifiedCategoryCannotBeReferenced()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_UNVERIFIED_CATEGORY",
                true,
                "UNVERIFIED",
                null);

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Unverified Category Device",
                                                LocalDate.now().minusDays(2),
                                                categoryId,
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveProductCannotBeReferenced()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_INACTIVE_PRODUCT_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID productId = createProduct(
                categoryId,
                "TEST_LIFECYCLE_INACTIVE_PRODUCT",
                false,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Inactive Product Device",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                productId,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleProductCannotBeReferenced()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_STALE_PRODUCT_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID productId = createProduct(
                categoryId,
                "TEST_LIFECYCLE_STALE_PRODUCT",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(181));

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Stale Product Device",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                productId,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unverifiedProductCannotBeReferenced()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_UNVERIFIED_PRODUCT_CATEGORY",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID productId = createProduct(
                categoryId,
                "TEST_LIFECYCLE_UNVERIFIED_PRODUCT",
                true,
                "UNVERIFIED",
                null);

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Unverified Product Device",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                productId,
                                                null,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unavailablePreferredServiceLocationReturns404()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_LIFECYCLE_INACTIVE_PROVIDER",
                false,
                "VERIFIED",
                LocalDate.now());

        UUID locationId = createProviderLocation(
                providerId,
                "TEST_LIFECYCLE_INACTIVE_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceId = createProviderService(
                providerId,
                "TEST_LIFECYCLE_INACTIVE_SERVICE",
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
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Unavailable Service Device",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                null,
                                                serviceLocationId,
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void replacementPredecessorMustBelongToAuthenticatedUser()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_REPLACEMENT_OWNER",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID predecessorId = createLifecycleRecord(
                USER_ONE,
                "User One Old Device",
                LocalDate.now().minusDays(30),
                categoryId,
                null,
                null,
                null);

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_TWO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "User Two Replacement",
                                                LocalDate.now().minusDays(1),
                                                categoryId,
                                                null,
                                                null,
                                                predecessorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void predecessorCanOnlyHaveOneDirectReplacement()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_REPLACEMENT_DUPLICATE",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID predecessorId = createLifecycleRecord(
                USER_ONE,
                "Original Device",
                LocalDate.now().minusDays(60),
                categoryId,
                null,
                null,
                null);

        UUID firstReplacementId =
                createLifecycleRecord(
                        USER_ONE,
                        "First Replacement",
                        LocalDate.now().minusDays(10),
                        categoryId,
                        null,
                        null,
                        predecessorId);

        mockMvc.perform(
        post("/api/users/me/lifecycle-records")
                .with(authenticatedUser(USER_ONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        lifecycleJson(
                                "Second Replacement",
                                LocalDate.now().minusDays(1),
                                categoryId,
                                null,
                                null,
                                predecessorId)))
        .andExpect(status().isConflict());

        assertEquals(
                1,
                countSuccessors(predecessorId));

        assertEquals(
                2,
                countLifecycleRecordsForUser(USER_ONE));
    }

    @Test
    void futureAcquiredOnReturns400()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_FUTURE_DATE",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        lifecycleJson(
                                                "Future Device",
                                                LocalDate.now().plusDays(1),
                                                categoryId,
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredFieldsReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/lifecycle-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "itemName": "",
                                            "acquiredOn": null,
                                            "acquisitionMethod": null,
                                            "conditionAtAcquisition": null,
                                            "lifecycleStatus": null
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientCannotControlLifecycleRecordOwnership()
            throws Exception {

        UUID categoryId = createCategory(
                "TEST_LIFECYCLE_CLIENT_OWNER",
                true,
                "VERIFIED",
                LocalDate.now());

        String response =
                mockMvc.perform(
                                post("/api/users/me/lifecycle-records")
                                        .with(authenticatedUser(USER_ONE))
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content("""
                                                {
                                                    "itemName": "Ownership Test Device",
                                                    "atProductId": null,
                                                    "atCategoryId": "%s",
                                                    "acquiredOn": "%s",
                                                    "acquisitionMethod": "PURCHASED",
                                                    "conditionAtAcquisition": "NEW",
                                                    "lifecycleStatus": "IN_USE",
                                                    "userId": "%s"
                                                }
                                                """.formatted(
                                                        categoryId,
                                                        LocalDate.now()
                                                                .minusDays(2),
                                                        userId(USER_TWO))))
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.userId")
                                        .doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String lifecycleRecordId =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();

        UUID userOneId = userId(USER_ONE);
        UUID userTwoId = userId(USER_TWO);

        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM lifecycle_records "
                                + "WHERE id = ? AND user_id = ?",
                        Integer.class,
                        UUID.fromString(lifecycleRecordId),
                        userOneId));

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM lifecycle_records "
                                + "WHERE id = ? AND user_id = ?",
                        Integer.class,
                        UUID.fromString(lifecycleRecordId),
                        userTwoId));
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

    private int countLifecycleRecordsForUser(
            String email) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lifecycle_records "
                        + "WHERE user_id = ?",
                Integer.class,
                userId(email));
    }

    private int countSuccessors(
            UUID predecessorId) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lifecycle_records "
                        + "WHERE replaces_lifecycle_record_id = ?",
                Integer.class,
                predecessorId);
    }

    private UUID createLifecycleRecord(
            String email,
            String itemName,
            LocalDate acquiredOn,
            UUID categoryId,
            UUID productId,
            UUID serviceLocationId,
            UUID predecessorId)
            throws Exception {

        String request =
                lifecycleJson(
                        itemName,
                        acquiredOn,
                        categoryId,
                        productId,
                        serviceLocationId,
                        predecessorId);

        String response =
                mockMvc.perform(
                                post("/api/users/me/lifecycle-records")
                                        .with(authenticatedUser(email))
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content(request))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return UUID.fromString(
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString());
    }

    private String validLifecycleJson() {

        return lifecycleJson(
                "Test Lifecycle Device",
                LocalDate.now().minusDays(1),
                null,
                null,
                null,
                null);
    }

    private String lifecycleJson(
            String itemName,
            LocalDate acquiredOn,
            UUID categoryId,
            UUID productId,
            UUID serviceLocationId,
            UUID predecessorId) {

        String categoryField =
                categoryId == null
                        ? "null"
                        : "\"" + categoryId + "\"";

        String productField =
                productId == null
                        ? "null"
                        : "\"" + productId + "\"";

        String serviceLocationField =
                serviceLocationId == null
                        ? "null"
                        : "\"" + serviceLocationId + "\"";

        String predecessorField =
                predecessorId == null
                        ? "null"
                        : "\"" + predecessorId + "\"";

        return """
                {
                    "itemName": "%s",
                    "atProductId": %s,
                    "atCategoryId": %s,
                    "acquiredOn": "%s",
                    "acquisitionMethod": "PURCHASED",
                    "conditionAtAcquisition": "NEW",
                    "lifecycleStatus": "IN_USE",
                    "warrantyExpiresOn": "%s",
                    "preferredProviderServiceLocationId": %s,
                    "replacementDueOn": "%s",
                    "reassessmentDueOn": "%s",
                    "replacesLifecycleRecordId": %s
                }
                """.formatted(
                itemName,
                productField,
                categoryField,
                acquiredOn,
                LocalDate.now().plusDays(365),
                serviceLocationField,
                LocalDate.now().plusYears(5),
                LocalDate.now().plusYears(1),
                predecessorField);
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
}