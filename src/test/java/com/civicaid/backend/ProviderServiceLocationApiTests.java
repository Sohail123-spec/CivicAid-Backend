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
class ProviderServiceLocationApiTests {

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
    void clearProviderServiceLocationTestData() {
        jdbcTemplate.update(
                "DELETE FROM verification_records "
                        + "WHERE provider_service_location_id IS NOT NULL");

        jdbcTemplate.update(
                "DELETE FROM audit_logs "
                        + "WHERE target_type IN "
                        + "('PROVIDER_SERVICE_LOCATION', 'PROVIDER_SERVICE', "
                        + "'PROVIDER_LOCATION', 'PROVIDER')");

        jdbcTemplate.update("DELETE FROM provider_service_locations");
        jdbcTemplate.update("DELETE FROM provider_services");
        jdbcTemplate.update("DELETE FROM provider_locations");
        jdbcTemplate.update("DELETE FROM providers");
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanCreateServiceLocationAssociation() throws Exception {
        UUID providerId = createProvider("SERVICE_LOCATION_PROVIDER");
        UUID serviceId = createService(providerId, "ASSESSMENT");
        UUID locationId = createLocation(providerId, "Main Road");

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceLocationRequest(locationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(providerId.toString()))
                .andExpect(jsonPath("$.providerServiceId").value(serviceId.toString()))
                .andExpect(jsonPath("$.providerLocationId").value(locationId.toString()))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.active").value(true));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_locations "
                        + "WHERE provider_service_id = ? "
                        + "AND provider_location_id = ?",
                Integer.class,
                serviceId,
                locationId);

        assertEquals(1, count);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_LOCATION' "
                        + "AND action_type = 'CREATE'",
                Integer.class);

        assertEquals(1, auditCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void serviceLocationAssociationRejectsDifferentProviders() throws Exception {
        UUID providerOne = createProvider("SERVICE_PROVIDER_ONE");
        UUID providerTwo = createProvider("LOCATION_PROVIDER_TWO");

        UUID serviceId = createService(providerOne, "ASSESSMENT");
        UUID locationId = createLocation(providerTwo, "Different Provider Road");

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceLocationRequest(locationId)))
                .andExpect(status().isBadRequest());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_locations",
                Integer.class);

        assertEquals(0, count);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void duplicateServiceLocationAssociationIsRejected() throws Exception {
        UUID providerId = createProvider("DUPLICATE_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "DUPLICATE");
        UUID locationId = createLocation(providerId, "Duplicate Road");

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceLocationRequest(locationId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceLocationRequest(locationId)))
                .andExpect(status().isBadRequest());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_locations "
                        + "WHERE provider_service_id = ? "
                        + "AND provider_location_id = ?",
                Integer.class,
                serviceId,
                locationId);

        assertEquals(1, count);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void invalidServiceLocationRequestIsRejected() throws Exception {
        UUID providerId = createProvider("INVALID_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "INVALID");

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "providerLocationId": null,
                            "sourceName": "",
                            "sourceUrl": "http://example.org/source"
                        }
                        """))
                .andExpect(status().isBadRequest());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM provider_service_locations",
                Integer.class);

        assertEquals(0, count);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void sourceUpdateResetsServiceLocationVerification() throws Exception {
        UUID providerId = createProvider("UPDATE_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "UPDATE");
        UUID locationId = createLocation(providerId, "Update Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        jdbcTemplate.update(
                "UPDATE provider_service_locations "
                        + "SET verification_status = 'VERIFIED', "
                        + "last_verified_at = CURRENT_DATE "
                        + "WHERE id = ?",
                serviceLocationId);

        mockMvc.perform(put(
                        "/api/admin/provider-service-locations/{serviceLocationId}",
                        serviceLocationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "sourceName": "Updated official source",
                            "sourceUrl": "https://example.org/updated-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"));

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status "
                        + "FROM provider_service_locations WHERE id = ?",
                String.class,
                serviceLocationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at "
                        + "FROM provider_service_locations WHERE id = ?",
                java.sql.Date.class,
                serviceLocationId);

        assertEquals("UNVERIFIED", verificationStatus);
        assertEquals(null, verifiedAt);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void noOpServiceLocationUpdateDoesNotCreateAnotherAuditEvent()
            throws Exception {

        UUID providerId = createProvider("NOOP_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "NOOP");
        UUID locationId = createLocation(providerId, "Noop Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        Integer auditCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                serviceLocationId);

        mockMvc.perform(put(
                        "/api/admin/provider-service-locations/{serviceLocationId}",
                        serviceLocationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "sourceName": "Official provider directory",
                            "sourceUrl": "https://example.org/locations"
                        }
                        """))
                .andExpect(status().isOk());

        Integer auditCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs "
                        + "WHERE target_type = 'PROVIDER_SERVICE_LOCATION' "
                        + "AND target_id = ? AND action_type = 'UPDATE'",
                Integer.class,
                serviceLocationId);

        assertEquals(auditCountBefore, auditCountAfter);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void adminCanVerifyServiceLocationAndVerificationRecordIsCreated()
            throws Exception {

        UUID providerId = createProvider("VERIFY_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "VERIFY");
        UUID locationId = createLocation(providerId, "Verify Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/{serviceLocationId}/verifications",
                        serviceLocationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "result": "VERIFIED",
                            "sourceName": "Official provider directory",
                            "sourceUrl": "https://example.org/verification",
                            "sourceVersion": "2026-10",
                            "reviewNotes": "Verified against official provider information"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").isNotEmpty());

        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status "
                        + "FROM provider_service_locations WHERE id = ?",
                String.class,
                serviceLocationId);

        java.sql.Date verifiedAt = jdbcTemplate.queryForObject(
                "SELECT last_verified_at "
                        + "FROM provider_service_locations WHERE id = ?",
                java.sql.Date.class,
                serviceLocationId);

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_service_location_id = ?",
                Integer.class,
                serviceLocationId);

        assertEquals("VERIFIED", verificationStatus);
        assertNotNull(verifiedAt);
        assertEquals(1, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationRequiresReviewNotes() throws Exception {
        UUID providerId = createProvider("REJECT_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "REJECT");
        UUID locationId = createLocation(providerId, "Reject Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/{serviceLocationId}/verifications",
                        serviceLocationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "result": "REJECTED",
                            "sourceName": "Official provider directory",
                            "sourceUrl": "https://example.org/verification",
                            "sourceVersion": "2026-10"
                        }
                        """))
                .andExpect(status().isBadRequest());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_service_location_id = ?",
                Integer.class,
                serviceLocationId);

        assertEquals(0, verificationCount);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void rejectedVerificationWithNotesCreatesImmutableHistoryRecord()
            throws Exception {

        UUID providerId = createProvider("REJECT_NOTES_PROVIDER");
        UUID serviceId = createService(providerId, "REJECT_NOTES");
        UUID locationId = createLocation(providerId, "Reject Notes Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/{serviceLocationId}/verifications",
                        serviceLocationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "result": "REJECTED",
                            "sourceName": "Official provider directory",
                            "sourceUrl": "https://example.org/verification",
                            "sourceVersion": "2026-10",
                            "reviewNotes": "Provider information could not be verified"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.lastVerifiedAt").isEmpty());

        Integer verificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_records "
                        + "WHERE provider_service_location_id = ?",
                Integer.class,
                serviceLocationId);

        String result = jdbcTemplate.queryForObject(
                "SELECT verification_result FROM verification_records "
                        + "WHERE provider_service_location_id = ?",
                String.class,
                serviceLocationId);

        assertEquals(1, verificationCount);
        assertEquals("REJECTED", result);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void archiveAndActivateServiceLocationWork() throws Exception {
        UUID providerId = createProvider("ACTIVE_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "ACTIVE");
        UUID locationId = createLocation(providerId, "Active Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        mockMvc.perform(patch(
                        "/api/admin/provider-service-locations/{serviceLocationId}/archive",
                        serviceLocationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Boolean archivedStatus = jdbcTemplate.queryForObject(
        "SELECT active FROM provider_service_locations WHERE id = ?",
        Boolean.class,
        serviceLocationId);

        assertEquals(Boolean.FALSE, archivedStatus);

        mockMvc.perform(patch(
                        "/api/admin/provider-service-locations/{serviceLocationId}/activate",
                        serviceLocationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        Boolean activeStatus = jdbcTemplate.queryForObject(
        "SELECT active FROM provider_service_locations WHERE id = ?",
        Boolean.class,
        serviceLocationId);

        assertEquals(Boolean.TRUE, activeStatus);
    }

    @Test
    @WithMockUser(username = "admin@example.org", roles = "ADMIN")
    void verificationHistoryCanBeRetrieved() throws Exception {
        UUID providerId = createProvider("HISTORY_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "HISTORY");
        UUID locationId = createLocation(providerId, "History Road");

        UUID serviceLocationId =
                createServiceLocation(serviceId, locationId);

        verifyServiceLocation(serviceLocationId, "VERIFIED",
                "Initial verification");

        verifyServiceLocation(serviceLocationId, "REVOKED",
                "Verification is no longer valid");

        mockMvc.perform(get(
                        "/api/admin/provider-service-locations/{serviceLocationId}/verification-records",
                        serviceLocationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].verificationResult").value("REVOKED"))
                .andExpect(jsonPath("$[1].verificationResult").value("VERIFIED"));
    }

    @Test
    void serviceLocationAdminEndpointsRequireAdminAuthorization()
            throws Exception {

        UUID providerId = createProvider("AUTH_ASSOC_PROVIDER");
        UUID serviceId = createService(providerId, "AUTH");
        UUID locationId = createLocation(providerId, "Auth Road");

        String request = serviceLocationRequest(locationId);

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
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

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);
        return UUID.fromString(id);
    }

    private UUID createService(UUID providerId, String code)
            throws Exception {

        MvcResult result = mockMvc.perform(post(
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

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(response)
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
                .content("""
                        {
                            "addressLine1": "%s",
                            "locality": "Madhapur",
                            "city": "Hyderabad",
                            "stateCode": "TS",
                            "postalCode": "500081",
                            "publicPhone": "+914000000000",
                            "sourceName": "Official provider directory",
                            "sourceUrl": "https://example.org/locations"
                        }
                        """.formatted(address)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);
        return UUID.fromString(id);
    }

    private UUID createServiceLocation(
            UUID serviceId,
            UUID locationId) throws Exception {

        MvcResult result = mockMvc.perform(post(
                        "/api/admin/provider-service-locations/services/{providerServiceId}",
                        serviceId)
                .with(user("admin@example.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceLocationRequest(locationId)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(response)
                .get("id")
                .asText();

        assertNotNull(id);
        return UUID.fromString(id);
    }

    private void verifyServiceLocation(
            UUID serviceLocationId,
            String result,
            String reviewNotes) throws Exception {

        mockMvc.perform(post(
                        "/api/admin/provider-service-locations/{serviceLocationId}/verifications",
                        serviceLocationId)
                .with(user("admin@example.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "result": "%s",
                            "sourceName": "Official provider directory",
                            "sourceUrl": "https://example.org/verification",
                            "sourceVersion": "2026-10",
                            "reviewNotes": "%s"
                        }
                        """.formatted(result, reviewNotes)))
                .andExpect(status().isOk());
    }

    private String serviceLocationRequest(UUID providerLocationId) {
        return """
                {
                    "providerLocationId": "%s",
                    "sourceName": "Official provider directory",
                    "sourceUrl": "https://example.org/locations"
                }
                """.formatted(providerLocationId);
    }
}