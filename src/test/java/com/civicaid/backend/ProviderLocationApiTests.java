package com.civicaid.backend;

import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderLocationApiTests {

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
    void clearProviderLocationTestData() {
        jdbcTemplate.update(
                "DELETE FROM verification_records WHERE provider_location_id IS NOT NULL");

        jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE target_type IN "
                        + "('PROVIDER_LOCATION', 'PROVIDER')");

        jdbcTemplate.update("DELETE FROM provider_locations");
        jdbcTemplate.update("DELETE FROM providers");
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateLocationAssociatedWithProvider() throws Exception {
        UUID providerId = createProvider("LOCATION_PROVIDER");

        mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequest(
                                "10 Main Road", "Hyderabad")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(providerId.toString()))
                .andExpect(jsonPath("$.addressLine1").value("10 Main Road"))
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.stateCode").value("TS"))
                .andExpect(jsonPath("$.countryCode").value("IN"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.active").value(true));

        Integer locationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_locations WHERE provider_id = ?",
                Integer.class,
                providerId);

        assertEquals(1, locationCount);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND action_type = 'CREATE'",
                Integer.class);

        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanListLocationsForProvider() throws Exception {
        UUID providerId = createProvider("LIST_LOCATION_PROVIDER");

        createLocation(providerId, "First Road");
        createLocation(providerId, "Second Road");

        mockMvc.perform(get(
                        "/api/admin/providers/{providerId}/locations",
                        providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanRetrieveLocation() throws Exception {
        UUID providerId = createProvider("GET_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Retrieve Road");

        mockMvc.perform(get(
                        "/api/admin/provider-locations/{locationId}",
                        locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId.toString()))
                .andExpect(jsonPath("$.providerId").value(providerId.toString()))
                .andExpect(jsonPath("$.addressLine1").value("Retrieve Road"));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidLocationRequestIsRejected() throws Exception {
        UUID providerId = createProvider("INVALID_LOCATION_PROVIDER");

        mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "addressLine1": "",
                                    "locality": "Madhapur",
                                    "city": "Hyderabad",
                                    "stateCode": "TS",
                                    "postalCode": "123",
                                    "latitude": 17.385,
                                    "sourceName": "Official directory",
                                    "sourceUrl": "http://example.org/source"
                                }
                                """))
                .andExpect(status().isBadRequest());

        Integer locationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_locations WHERE provider_id = ?",
                Integer.class,
                providerId);

        assertEquals(0, locationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidStateCodeIsRejectedWhenCreatingLocation() throws Exception {
        UUID providerId = createProvider("INVALID_STATE_CREATE_PROVIDER");

        mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequestWithStateCode(
                                "Invalid State Road",
                                "Hyderabad",
                                "ZZ")))
                .andExpect(status().isBadRequest());

        Integer locationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_locations WHERE provider_id = ?",
                Integer.class,
                providerId);

        assertEquals(0, locationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidStateCodeIsRejectedWhenUpdatingLocation() throws Exception {
        UUID providerId = createProvider("INVALID_STATE_UPDATE_PROVIDER");
        UUID locationId = createLocation(providerId, "Original Road");

        mockMvc.perform(put(
                        "/api/admin/provider-locations/{locationId}",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequestWithStateCode(
                                "Changed Road",
                                "Hyderabad",
                                "ZZ")))
                .andExpect(status().isBadRequest());

        String addressAfterUpdate = jdbcTemplate.queryForObject(
                "SELECT address_line_1 FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        String stateCodeAfterUpdate = jdbcTemplate.queryForObject(
                "SELECT state_code FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        assertEquals("Original Road", addressAfterUpdate);
        assertEquals("TS", stateCodeAfterUpdate);

        Integer updateAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                locationId);

        assertEquals(0, updateAuditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void substantiveUpdateResetsLocationVerification() throws Exception {
        UUID providerId = createProvider("UPDATE_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Original Road");

        jdbcTemplate.update(
                "UPDATE provider_locations "
                        + "SET verification_status = 'VERIFIED', "
                        + "last_verified_at = CURRENT_DATE "
                        + "WHERE id = ?",
                locationId);

        mockMvc.perform(put(
                        "/api/admin/provider-locations/{locationId}",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequest(
                                "Updated Road", "Hyderabad")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("Updated Road"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM provider_locations WHERE id = ?",
                java.sql.Date.class,
                locationId);

        assertEquals("UNVERIFIED", verificationStatus);
        assertEquals(null, verifiedAt);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void noOpUpdateDoesNotCreateAnotherAuditEvent() throws Exception {
        UUID providerId = createProvider("NOOP_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Unchanged Road");

        Integer auditCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                locationId);

        mockMvc.perform(put(
                        "/api/admin/provider-locations/{locationId}",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequest(
                                "Unchanged Road", "Hyderabad")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("Unchanged Road"));

        Integer auditCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                locationId);

        assertEquals(auditCountBefore, auditCountAfter);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void equivalentCoordinateValuesDoNotResetVerificationOrCreateAuditEvent()
            throws Exception {
        UUID providerId = createProvider("COORDINATE_SCALE_PROVIDER");

        MvcResult createResult = mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequestWithCoordinates(
                                "Coordinate Road",
                                "Hyderabad",
                                "17.385000",
                                "78.486700")))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        UUID locationId = UUID.fromString(
                jsonMapper.readTree(response).get("id").asText());

        jdbcTemplate.update(
                "UPDATE provider_locations "
                        + "SET verification_status = 'VERIFIED', "
                        + "last_verified_at = CURRENT_DATE "
                        + "WHERE id = ?",
                locationId);

        Integer auditCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                locationId);

        mockMvc.perform(put(
                        "/api/admin/provider-locations/{locationId}",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequestWithCoordinates(
                                "Coordinate Road",
                                "Hyderabad",
                                "17.385",
                                "78.4867")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at FROM provider_locations WHERE id = ?",
                java.sql.Date.class,
                locationId);

        Integer auditCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                locationId);

        assertEquals("VERIFIED", verificationStatus);
        assertNotNull(verifiedAt);
        assertEquals(auditCountBefore, auditCountAfter);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanVerifyLocationAndVerificationRecordIsCreated()
            throws Exception {
        UUID providerId = createProvider("VERIFY_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Verify Road");

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "VERIFIED",
                                "Verified against official provider information")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationResult").value("VERIFIED"))
                .andExpect(jsonPath("$.categoryId").value(locationId.toString()));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status "
                        + "FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at "
                        + "FROM provider_locations WHERE id = ?",
                java.sql.Date.class,
                locationId);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_location_id = ?",
                Integer.class,
                locationId);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? AND action_type = 'VERIFY'",
                Integer.class,
                locationId);

        assertEquals("VERIFIED", verificationStatus);
        assertNotNull(verifiedAt);
        assertEquals(1, verificationCount);
        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationRequiresReviewNotes() throws Exception {
        UUID providerId = createProvider("REJECT_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Reject Road");

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "REJECTED",
                                null)))
                .andExpect(status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_location_id = ?",
                Integer.class,
                locationId);

        assertEquals(0, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationWithNotesResetsLocationVerification()
            throws Exception {
        UUID providerId = createProvider("REJECTED_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Rejected Road");

        jdbcTemplate.update(
                "UPDATE provider_locations "
                        + "SET verification_status = 'VERIFIED', "
                        + "last_verified_at = CURRENT_DATE "
                        + "WHERE id = ?",
                locationId);

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "REJECTED",
                                "The source no longer confirms this location.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationResult").value("REJECTED"));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status "
                        + "FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at "
                        + "FROM provider_locations WHERE id = ?",
                java.sql.Date.class,
                locationId);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_location_id = ?",
                Integer.class,
                locationId);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? "
                        + "AND action_type = 'REJECT_VERIFICATION'",
                Integer.class,
                locationId);

        assertEquals("UNVERIFIED", verificationStatus);
        assertEquals(null, verifiedAt);
        assertEquals(1, verificationCount);
        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void revokedVerificationWithNotesResetsLocationVerification()
            throws Exception {
        UUID providerId = createProvider("REVOKED_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Revoked Road");

        jdbcTemplate.update(
                "UPDATE provider_locations "
                        + "SET verification_status = 'VERIFIED', "
                        + "last_verified_at = CURRENT_DATE "
                        + "WHERE id = ?",
                locationId);

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "REVOKED",
                                "The previous verification is no longer valid.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationResult").value("REVOKED"));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status "
                        + "FROM provider_locations WHERE id = ?",
                String.class,
                locationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at "
                        + "FROM provider_locations WHERE id = ?",
                java.sql.Date.class,
                locationId);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_location_id = ?",
                Integer.class,
                locationId);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_LOCATION' "
                        + "AND target_id = ? "
                        + "AND action_type = 'REVOKE_VERIFICATION'",
                Integer.class,
                locationId);

        assertEquals("UNVERIFIED", verificationStatus);
        assertEquals(null, verifiedAt);
        assertEquals(1, verificationCount);
        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void verificationHistoryIsNewestFirst() throws Exception {
        UUID providerId = createProvider("HISTORY_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "History Road");

        verifyLocation(
                locationId,
                "VERIFIED",
                "Initial verification");

        verifyLocation(
                locationId,
                "REVOKED",
                "Verification is no longer valid.");

        mockMvc.perform(get(
                        "/api/admin/provider-locations/{locationId}/verification-records",
                        locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].verificationResult")
                        .value("REVOKED"))
                .andExpect(jsonPath("$[1].verificationResult")
                        .value("VERIFIED"))
                .andExpect(jsonPath("$[0].reviewNotes")
                        .value("Verification is no longer valid."));
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidVerificationSourceUrlIsRejected() throws Exception {
        UUID providerId = createProvider("INVALID_LOCATION_SOURCE_PROVIDER");
        UUID locationId = createLocation(providerId, "Invalid Source Road");

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "result": "VERIFIED",
                                    "sourceName": "Official provider directory",
                                    "sourceUrl": "http://example.org/verification",
                                    "sourceVersion": "2026-10",
                                    "reviewNotes": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_location_id = ?",
                Integer.class,
                locationId);

        assertEquals(0, verificationCount);
    }

    @Test
    void locationVerificationEndpointsRequireAdminAuthorization()
            throws Exception {
        UUID providerId = createProvider("AUTH_VERIFY_LOCATION_PROVIDER");
        UUID locationId = createLocation(providerId, "Auth Verify Road");

        String request = verificationRequest(
                "VERIFIED",
                "Verified against official provider information");

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .with(user("ordinary-user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                        "/api/admin/provider-locations/{locationId}/verification-records",
                        locationId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(
                        "/api/admin/provider-locations/{locationId}/verification-records",
                        locationId)
                        .with(user("ordinary-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void nonexistentProviderAndLocationReturnNotFound() throws Exception {
        UUID nonexistentProviderId = UUID.randomUUID();
        UUID nonexistentLocationId = UUID.randomUUID();

        mockMvc.perform(get(
                        "/api/admin/providers/{providerId}/locations",
                        nonexistentProviderId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/admin/provider-locations/{locationId}",
                        nonexistentLocationId))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        nonexistentLocationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(
                                "VERIFIED",
                                "Verified against official source")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/admin/provider-locations/{locationId}/verification-records",
                        nonexistentLocationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void locationAdminEndpointsRequireAdminAuthorization() throws Exception {
        UUID providerId = createProvider("AUTH_LOCATION_PROVIDER");
        String request = locationRequest("Secure Road", "Hyderabad");

        mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .with(user("ordinary-user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    private UUID createProvider(String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/providers")
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
        String id = jsonMapper.readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);
        return UUID.fromString(id);
    }

    private UUID createLocation(UUID providerId, String address)
            throws Exception {
        MvcResult result = mockMvc.perform(post(
                        "/api/admin/providers/{providerId}/locations",
                        providerId)
                        .with(user("admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationRequest(address, "Hyderabad")))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String id = jsonMapper.readTree(response)
                .get("id")
                .asText();

        return UUID.fromString(id);
    }

    private void verifyLocation(
            UUID locationId,
            String result,
            String reviewNotes) throws Exception {
        mockMvc.perform(post(
                        "/api/admin/provider-locations/{locationId}/verifications",
                        locationId)
                        .with(user("admin@example.org").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationRequest(result, reviewNotes)))
                .andExpect(status().isOk());
    }

    private String verificationRequest(
            String result,
            String reviewNotes) {
        return """
                {
                    "result": "%s",
                    "sourceName": "Official provider directory",
                    "sourceUrl": "https://example.org/verification",
                    "sourceVersion": "2026-10",
                    "reviewNotes": %s
                }
                """.formatted(
                result,
                reviewNotes == null
                        ? "null"
                        : "\"" + reviewNotes + "\"");
    }

    private String locationRequest(String address, String city) {
        return locationRequestWithStateCode(address, city, "TS");
    }

    private String locationRequestWithStateCode(
            String address,
            String city,
            String stateCode) {
        return """
                {
                    "addressLine1": "%s",
                    "locality": "Madhapur",
                    "city": "%s",
                    "stateCode": "%s",
                    "postalCode": "500081",
                    "publicPhone": "+914000000000",
                    "sourceName": "Official provider directory",
                    "sourceUrl": "https://example.org/locations"
                }
                """.formatted(address, city, stateCode);
    }

    private String locationRequestWithCoordinates(
            String address,
            String city,
            String latitude,
            String longitude) {
        return """
                {
                    "addressLine1": "%s",
                    "locality": "Madhapur",
                    "city": "%s",
                    "stateCode": "TS",
                    "postalCode": "500081",
                    "publicPhone": "+914000000000",
                    "latitude": %s,
                    "longitude": %s,
                    "sourceName": "Official provider directory",
                    "sourceUrl": "https://example.org/locations"
                }
                """.formatted(address, city, latitude, longitude);
    }
}