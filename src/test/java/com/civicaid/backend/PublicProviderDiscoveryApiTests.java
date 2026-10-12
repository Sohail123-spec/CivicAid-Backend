package com.civicaid.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicProviderDiscoveryApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearProviderDiscoveryTestData() {

        jdbcTemplate.update(
                "DELETE FROM provider_service_locations "
                        + "WHERE provider_service_id IN ("
                        + "SELECT id FROM provider_services "
                        + "WHERE code LIKE 'TEST_PUBLIC_%')");

        jdbcTemplate.update(
                "DELETE FROM provider_service_locations "
                        + "WHERE provider_location_id IN ("
                        + "SELECT id FROM provider_locations "
                        + "WHERE address_line_1 LIKE 'TEST_PUBLIC_%')");

        jdbcTemplate.update(
                "DELETE FROM provider_services "
                        + "WHERE code LIKE 'TEST_PUBLIC_%'");

        jdbcTemplate.update(
                "DELETE FROM provider_locations "
                        + "WHERE address_line_1 LIKE 'TEST_PUBLIC_%'");

        jdbcTemplate.update(
                "DELETE FROM providers "
                        + "WHERE code LIKE 'TEST_PUBLIC_%'");
    }

    @Test
    void anonymousUserCanDiscoverPublicProviders()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_PROVIDER",
                "Public Assistive Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$[0].code")
                        .value("TEST_PUBLIC_PROVIDER"))
                .andExpect(jsonPath("$[0].name")
                        .value("Public Assistive Provider"))
                .andExpect(jsonPath("$[0].verificationStatus")
                        .value("VERIFIED"))
                .andExpect(jsonPath("$[0].active")
                        .value(true));
    }

    @Test
    void anonymousUserCanRetrievePublicProvider()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_PROVIDER_GET",
                "Public Provider Get",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get("/api/providers/{providerId}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$.code")
                        .value("TEST_PUBLIC_PROVIDER_GET"))
                .andExpect(jsonPath("$.name")
                        .value("Public Provider Get"));
    }

    @Test
    void inactiveUnverifiedAndStaleProvidersAreExcluded()
            throws Exception {

        createProvider(
                "TEST_PUBLIC_ELIGIBLE",
                "Eligible Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        createProvider(
                "TEST_PUBLIC_INACTIVE",
                "Inactive Provider",
                false,
                "VERIFIED",
                LocalDate.now());

        createProvider(
                "TEST_PUBLIC_UNVERIFIED",
                "Unverified Provider",
                true,
                "UNVERIFIED",
                null);

        createProvider(
                "TEST_PUBLIC_STALE",
                "Stale Provider",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(91));

        mockMvc.perform(
                        get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'TEST_PUBLIC_ELIGIBLE')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$[?(@.code == 'TEST_PUBLIC_INACTIVE')]")
                        .isEmpty())
                .andExpect(jsonPath("$[?(@.code == 'TEST_PUBLIC_UNVERIFIED')]")
                        .isEmpty())
                .andExpect(jsonPath("$[?(@.code == 'TEST_PUBLIC_STALE')]")
                        .isEmpty());
    }

    @Test
    void futureVerifiedProviderIsExcluded()
            throws Exception {

        createProvider(
                "TEST_PUBLIC_FUTURE",
                "Future Verification Provider",
                true,
                "VERIFIED",
                LocalDate.now().plusDays(1));

        mockMvc.perform(
                        get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'TEST_PUBLIC_FUTURE')]")
                        .isEmpty());
    }

    @Test
    void unavailableProviderReturnsNotFoundFromPublicLookup()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_HIDDEN_GET",
                "Hidden Provider",
                true,
                "UNVERIFIED",
                null);

        mockMvc.perform(
                        get("/api/providers/{providerId}", providerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonexistentProviderReturnsNotFound()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}",
                                UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserCanDiscoverProviderLocations()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_LOCATION_PROVIDER",
                "Location Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID eligibleLocationId = createProviderLocation(
                providerId,
                "TEST_PUBLIC_ELIGIBLE_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        createProviderLocation(
                providerId,
                "TEST_PUBLIC_STALE_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(91));

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/locations",
                                providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(eligibleLocationId.toString()))
                .andExpect(jsonPath("$[0].providerId")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$[0].providerName")
                        .value("Location Provider"))
                .andExpect(jsonPath("$[0].addressLine1")
                        .value("TEST_PUBLIC_ELIGIBLE_LOCATION"));
    }

    @Test
    void providerLocationsRequireEligibleParentProvider()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_LOCATION_PARENT",
                "Inactive Parent Provider",
                false,
                "VERIFIED",
                LocalDate.now());

        createProviderLocation(
                providerId,
                "TEST_PUBLIC_LOCATION_HIDDEN",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/locations",
                                providerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserCanDiscoverProviderServices()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_SERVICE_PROVIDER",
                "Service Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceId = createProviderService(
                providerId,
                "TEST_PUBLIC_SERVICE",
                "Public Assessment Service",
                true,
                "VERIFIED",
                LocalDate.now());

        createProviderService(
                providerId,
                "TEST_PUBLIC_STALE_SERVICE",
                "Stale Assessment Service",
                true,
                "VERIFIED",
                LocalDate.now().minusDays(91));

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/services",
                                providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(serviceId.toString()))
                .andExpect(jsonPath("$[0].providerId")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$[0].providerName")
                        .value("Service Provider"))
                .andExpect(jsonPath("$[0].code")
                        .value("TEST_PUBLIC_SERVICE"))
                .andExpect(jsonPath("$[0].name")
                        .value("Public Assessment Service"));
    }

    @Test
    void providerServicesRequireEligibleParentProvider()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_SERVICE_PARENT",
                "Inactive Service Parent",
                false,
                "VERIFIED",
                LocalDate.now());

        createProviderService(
                providerId,
                "TEST_PUBLIC_HIDDEN_SERVICE",
                "Hidden Service",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/services",
                                providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anonymousUserCanDiscoverProviderServiceLocations()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_PSL_PROVIDER",
                "Service Location Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID locationId = createProviderLocation(
                providerId,
                "TEST_PUBLIC_PSL_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceId = createProviderService(
                providerId,
                "TEST_PUBLIC_PSL_SERVICE",
                "Public Service",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceLocationId = createProviderServiceLocation(
                serviceId,
                locationId,
                true,
                "VERIFIED",
                LocalDate.now());

        createProviderServiceLocation(
                serviceId,
                createProviderLocation(
                        providerId,
                        "TEST_PUBLIC_PSL_STALE_LOCATION",
                        true,
                        "VERIFIED",
                        LocalDate.now().minusDays(91)),
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/service-locations",
                                providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(serviceLocationId.toString()))
                .andExpect(jsonPath("$[0].providerId")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$[0].providerServiceId")
                        .value(serviceId.toString()))
                .andExpect(jsonPath("$[0].providerLocationId")
                        .value(locationId.toString()))
                .andExpect(jsonPath("$[0].serviceCode")
                        .value("TEST_PUBLIC_PSL_SERVICE"));
    }

    @Test
    void providerServiceLocationsExcludeUnverifiedAndInactiveChildren()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_PSL_FILTER_PROVIDER",
                "PSL Filter Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID eligibleLocationId = createProviderLocation(
                providerId,
                "TEST_PUBLIC_PSL_FILTER_LOCATION",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID serviceId = createProviderService(
                providerId,
                "TEST_PUBLIC_PSL_FILTER_SERVICE",
                "Filter Service",
                true,
                "VERIFIED",
                LocalDate.now());

        UUID eligibleServiceLocationId =
                createProviderServiceLocation(
                        serviceId,
                        eligibleLocationId,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID unverifiedLocationId = createProviderLocation(
                providerId,
                "TEST_PUBLIC_PSL_UNVERIFIED_LOCATION",
                true,
                "UNVERIFIED",
                null);

        createProviderServiceLocation(
                serviceId,
                unverifiedLocationId,
                true,
                "VERIFIED",
                LocalDate.now());

        UUID inactiveLocationId = createProviderLocation(
                providerId,
                "TEST_PUBLIC_PSL_INACTIVE_LOCATION",
                false,
                "VERIFIED",
                LocalDate.now());

        createProviderServiceLocation(
                serviceId,
                inactiveLocationId,
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/service-locations",
                                providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(eligibleServiceLocationId.toString()));
    }

    @Test
    void publicProviderEndpointsDoNotRequireAuthentication()
            throws Exception {

        UUID providerId = createProvider(
                "TEST_PUBLIC_ANONYMOUS",
                "Anonymous Discovery Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get("/api/providers")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .anonymous()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/providers/{providerId}", providerId)
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .anonymous()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/locations",
                                providerId)
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .anonymous()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/services",
                                providerId)
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .anonymous()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/providers/{providerId}/service-locations",
                                providerId)
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .anonymous()))
                .andExpect(status().isOk());
    }

    @Test
    void providerPublicResponsesDoNotExposeInternalUserOwnership()
            throws Exception {

        createProvider(
                "TEST_PUBLIC_PRIVACY",
                "Privacy Provider",
                true,
                "VERIFIED",
                LocalDate.now());

        mockMvc.perform(
                        get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    private UUID createProvider(
            String code,
            String name,
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
                name,
                "NGO",
                "Test public provider.",
                "https://example.org/providers/" + code.toLowerCase(),
                "Test source",
                "https://example.org/source/" + code.toLowerCase(),
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }

    private UUID createProviderLocation(
            UUID providerId,
            String addressLine1,
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
                addressLine1,
                null,
                "Test Locality",
                "Hyderabad",
                "TS",
                "500001",
                "IN",
                new BigDecimal("17.3850"),
                new BigDecimal("78.4867"),
                "0401234567",
                null,
                "Accessible test location.",
                "Test source",
                "https://example.org/location/" + id,
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
            String name,
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
                name,
                "ASSESSMENT_AND_FITTING",
                "Test public provider service.",
                "Test source",
                "https://example.org/services/" + code.toLowerCase(),
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
                "https://example.org/service-locations/" + id,
                verificationStatus,
                lastVerifiedAt,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return id;
    }
}