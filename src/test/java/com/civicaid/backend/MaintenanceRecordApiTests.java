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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceRecordApiTests {

    private static final String USER_ONE =
            "maintenance-user-one@example.org";

    private static final String USER_TWO =
            "maintenance-user-two@example.org";

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
    void clearMaintenanceTestData() {
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
                        + "WHERE code LIKE 'TEST_MAINTENANCE\\_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM provider_services "
                        + "WHERE code LIKE 'TEST_MAINTENANCE\\_%'");

        jdbcTemplate.update(
                "DELETE FROM provider_locations "
                        + "WHERE provider_id IN ("
                        + "SELECT id FROM providers "
                        + "WHERE code LIKE 'TEST_MAINTENANCE\\_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM providers "
                        + "WHERE code LIKE 'TEST_MAINTENANCE\\_%'");

        jdbcTemplate.update(
                "DELETE FROM users WHERE email IN (?, ?)",
                USER_ONE,
                USER_TWO);
    }

    @Test
    void unauthenticatedListReturns401() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/lifecycle/{lifecycleRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetReturns401() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPostReturns401() throws Exception {
        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validMaintenanceJson(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPatchReturns401() throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUpdateJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteReturns401() throws Exception {
        mockMvc.perform(
                        delete(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Maintenance Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(5),
                                                null,
                                                "Battery and controls checked.",
                                                LocalDate.now().plusMonths(6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(
                        jsonPath("$.lifecycleRecordId")
                                .value(lifecycleRecordId.toString()))
                .andExpect(
                        jsonPath("$.maintenanceType")
                                .value("ROUTINE"))
                .andExpect(
                        jsonPath("$.details")
                                .value("Battery and controls checked."))
                .andExpect(
                        jsonPath("$.nextMaintenanceDueOn")
                                .value(
                                        LocalDate.now()
                                                .plusMonths(6)
                                                .toString()));
    }

    @Test
    void authenticatedUserCanListOwnMaintenanceRecords()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Listed Device",
                        LocalDate.now().minusDays(60));

        createMaintenanceRecord(
                USER_ONE,
                lifecycleRecordId,
                "CLEANING",
                LocalDate.now().minusDays(20),
                null,
                "Cleaning completed.",
                null);

        createMaintenanceRecord(
                USER_ONE,
                lifecycleRecordId,
                "ADJUSTMENT",
                LocalDate.now().minusDays(5),
                null,
                "Control adjustment completed.",
                null);

        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/lifecycle/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[0].maintenanceType")
                                .value("ADJUSTMENT"))
                .andExpect(
                        jsonPath("$[0].details")
                                .value("Control adjustment completed."))
                .andExpect(
                        jsonPath("$[1].maintenanceType")
                                .value("CLEANING"));
    }

    @Test
    void authenticatedUserCanGetOwnMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Retrievable Device",
                        LocalDate.now().minusDays(20));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "PREVENTIVE",
                        LocalDate.now().minusDays(3),
                        null,
                        "Preventive inspection completed.",
                        null);

        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(maintenanceRecordId.toString()))
                .andExpect(
                        jsonPath("$.lifecycleRecordId")
                                .value(lifecycleRecordId.toString()))
                .andExpect(
                        jsonPath("$.maintenanceType")
                                .value("PREVENTIVE"));
    }

    @Test
    void authenticatedUserCanUpdateOwnMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Updatable Device",
                        LocalDate.now().minusDays(30));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "ROUTINE",
                        LocalDate.now().minusDays(10),
                        null,
                        "Initial check.",
                        null);

        mockMvc.perform(
                        patch(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        updateJson(
                                                "PREVENTIVE",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "Preventive maintenance completed.",
                                                LocalDate.now().plusMonths(3))))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(maintenanceRecordId.toString()))
                .andExpect(
                        jsonPath("$.maintenanceType")
                                .value("PREVENTIVE"))
                .andExpect(
                        jsonPath("$.details")
                                .value("Preventive maintenance completed."))
                .andExpect(
                        jsonPath("$.nextMaintenanceDueOn")
                                .value(
                                        LocalDate.now()
                                                .plusMonths(3)
                                                .toString()));
    }

    @Test
    void authenticatedUserCanDeleteOwnMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Deletable Device",
                        LocalDate.now().minusDays(20));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "CLEANING",
                        LocalDate.now().minusDays(2),
                        null,
                        "Cleaning completed.",
                        null);

        mockMvc.perform(
                        delete(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isNoContent());

        assertEquals(
                0,
                countMaintenanceRecordsForUser(USER_ONE));
    }

    @Test
    void userCannotListAnotherUsersLifecycleMaintenance()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private Device",
                        LocalDate.now().minusDays(20));

        createMaintenanceRecord(
                USER_ONE,
                lifecycleRecordId,
                "ROUTINE",
                LocalDate.now().minusDays(2),
                null,
                "Private maintenance.",
                null);

        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/lifecycle/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotGetAnotherUsersMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private Device",
                        LocalDate.now().minusDays(20));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "ROUTINE",
                        LocalDate.now().minusDays(2),
                        null,
                        "Private maintenance.",
                        null);

        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUpdateAnotherUsersMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private Device",
                        LocalDate.now().minusDays(20));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "ROUTINE",
                        LocalDate.now().minusDays(2),
                        null,
                        "Private maintenance.",
                        null);

        mockMvc.perform(
                        patch(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_TWO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUpdateJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotDeleteAnotherUsersMaintenanceRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private Device",
                        LocalDate.now().minusDays(20));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "ROUTINE",
                        LocalDate.now().minusDays(2),
                        null,
                        "Private maintenance.",
                        null);

        mockMvc.perform(
                        delete(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());

        assertEquals(
                1,
                countMaintenanceRecordsForUser(USER_ONE));
    }

    @Test
    void clientCannotUseAnotherUsersLifecycleRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private Lifecycle",
                        LocalDate.now().minusDays(20));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_TWO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "Unauthorized lifecycle access.",
                                                null)))
                .andExpect(status().isNotFound());

        assertEquals(
                0,
                countMaintenanceRecordsForUser(USER_TWO));
    }

    @Test
    void nonexistentLifecycleRecordReturns404()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                UUID.randomUUID(),
                                                "ROUTINE",
                                                LocalDate.now().minusDays(1),
                                                null,
                                                "Maintenance.",
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void maintenanceDateBeforeAcquisitionReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Date Protected Device",
                        LocalDate.now().minusDays(5));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(6),
                                                null,
                                                "Invalid date.",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void futureMaintenanceDateReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Future Date Device",
                        LocalDate.now().minusDays(5));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().plusDays(1),
                                                null,
                                                "Future maintenance.",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextMaintenanceDueBeforeMaintenanceDateReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Due Date Device",
                        LocalDate.now().minusDays(20));

        LocalDate maintenanceDate =
                LocalDate.now().minusDays(5);

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                maintenanceDate,
                                                null,
                                                "Invalid next due date.",
                                                maintenanceDate.minusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankDetailsAreNormalizedToNull()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Blank Details Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "CLEANING",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "   ",
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void detailsAreTrimmed()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Trimmed Details Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ADJUSTMENT",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "   Control adjusted.   ",
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.details")
                                .value("Control adjusted."));
    }

    @Test
    void detailsLongerThan1000CharactersReturn400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Long Details Device",
                        LocalDate.now().minusDays(10));

        String longDetails = "A".repeat(1001);

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "OTHER",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                longDetails,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void diagnosisDetailsReturn400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Diagnosis Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "OTHER",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "Diagnosis recorded during service.",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentDetailsReturn400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Payment Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "OTHER",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "Payment completed by card.",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void providerStaffDetailsReturn400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Staff Details Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "OTHER",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "Technician staff member serviced the device.",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validEquipmentFactsAreAccepted()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Valid Details Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "PREVENTIVE",
                                                LocalDate.now().minusDays(2),
                                                null,
                                                "Battery contacts cleaned and control cable tightened.",
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.details")
                                .value(
                                        "Battery contacts cleaned and control cable tightened."));
    }

    @Test
    void validProviderServiceLocationIsAccepted()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Provider Linked Device",
                        LocalDate.now().minusDays(20));

        UUID serviceLocationId =
                createProviderServiceLocationChain(
                        "TEST_MAINTENANCE_VALID",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(2),
                                                serviceLocationId,
                                                "Routine service completed.",
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.providerServiceLocationId")
                                .value(serviceLocationId.toString()));
    }

    @Test
    void inactiveProviderServiceLocationReturns404()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Inactive Location Device",
                        LocalDate.now().minusDays(20));

        UUID serviceLocationId =
                createProviderServiceLocationChain(
                        "TEST_MAINTENANCE_INACTIVE",
                        false,
                        "VERIFIED",
                        LocalDate.now());

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(2),
                                                serviceLocationId,
                                                "Routine service.",
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unverifiedProviderServiceLocationReturns404()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Unverified Location Device",
                        LocalDate.now().minusDays(20));

        UUID serviceLocationId =
                createProviderServiceLocationChain(
                        "TEST_MAINTENANCE_UNVERIFIED",
                        true,
                        "UNVERIFIED",
                        null);

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(2),
                                                serviceLocationId,
                                                "Routine service.",
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleProviderServiceLocationReturns404()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Stale Location Device",
                        LocalDate.now().minusDays(20));

        UUID serviceLocationId =
                createProviderServiceLocationChain(
                        "TEST_MAINTENANCE_STALE",
                        true,
                        "VERIFIED",
                        LocalDate.now().minusDays(91));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        maintenanceJson(
                                                lifecycleRecordId,
                                                "ROUTINE",
                                                LocalDate.now().minusDays(2),
                                                serviceLocationId,
                                                "Routine service.",
                                                null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicalProviderServiceLocationRemainsLinkedAfterArchival()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Historical Provider Device",
                        LocalDate.now().minusDays(20));

        UUID serviceLocationId =
                createProviderServiceLocationChain(
                        "TEST_MAINTENANCE_HISTORY",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "ROUTINE",
                        LocalDate.now().minusDays(2),
                        serviceLocationId,
                        "Routine service completed.",
                        null);

        jdbcTemplate.update(
                "UPDATE provider_service_locations "
                        + "SET active = false "
                        + "WHERE id = ?",
                serviceLocationId);

        mockMvc.perform(
                        get(
                                "/api/users/me/maintenance-records/{maintenanceRecordId}",
                                maintenanceRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.providerServiceLocationId")
                                .value(serviceLocationId.toString()));
    }

    @Test
    void missingRequiredFieldsReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "lifecycleRecordId": null,
                                            "maintenanceType": null,
                                            "maintenanceDate": null
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidMaintenanceTypeReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Invalid Type Device",
                        LocalDate.now().minusDays(10));

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "lifecycleRecordId": "%s",
                                            "maintenanceType": "INVALID_TYPE",
                                            "maintenanceDate": "%s",
                                            "details": "Equipment adjustment."
                                        }
                                        """.formatted(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientSuppliedUserIdDoesNotControlOwnership()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Ownership Protected Device",
                        LocalDate.now().minusDays(10));

        UUID userTwoId = userId(USER_TWO);

        mockMvc.perform(
                        post("/api/users/me/maintenance-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "lifecycleRecordId": "%s",
                                            "maintenanceType": "ROUTINE",
                                            "maintenanceDate": "%s",
                                            "details": "Equipment checked.",
                                            "userId": "%s"
                                        }
                                        """.formatted(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                userTwoId)))
                .andExpect(status().isCreated());

        assertEquals(
                1,
                countMaintenanceRecordsForUser(USER_ONE));

        assertEquals(
                0,
                countMaintenanceRecordsForUser(USER_TWO));
    }

    @Test
    void maintenanceRecordsAreDeletedWhenLifecycleRecordIsDeleted()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Cascade Device",
                        LocalDate.now().minusDays(10));

        UUID maintenanceRecordId =
                createMaintenanceRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        "CLEANING",
                        LocalDate.now().minusDays(2),
                        null,
                        "Cleaning completed.",
                        null);

        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM maintenance_records "
                                + "WHERE id = ?",
                        Integer.class,
                        maintenanceRecordId));

        mockMvc.perform(
                        delete(
                                "/api/users/me/lifecycle-records/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isNoContent());

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM maintenance_records "
                                + "WHERE id = ?",
                        Integer.class,
                        maintenanceRecordId));
    }

    private RequestPostProcessor authenticatedUser(
            String email) {

        return authentication(
                authenticationFor(email));
    }

    private Authentication authenticationFor(
            String email) {

        User user =
                userRepository.findByEmail(email)
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

    private UUID createLifecycleRecord(
            String email,
            String itemName,
            LocalDate acquiredOn)
            throws Exception {

        String response =
                mockMvc.perform(
                                post(
                                        "/api/users/me/lifecycle-records")
                                        .with(authenticatedUser(email))
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content(
                                                lifecycleJson(
                                                        itemName,
                                                        acquiredOn)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return UUID.fromString(
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString());
    }

    private UUID createMaintenanceRecord(
            String email,
            UUID lifecycleRecordId,
            String maintenanceType,
            LocalDate maintenanceDate,
            UUID providerServiceLocationId,
            String details,
            LocalDate nextMaintenanceDueOn)
            throws Exception {

        String response =
                mockMvc.perform(
                                post(
                                        "/api/users/me/maintenance-records")
                                        .with(authenticatedUser(email))
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content(
                                                maintenanceJson(
                                                        lifecycleRecordId,
                                                        maintenanceType,
                                                        maintenanceDate,
                                                        providerServiceLocationId,
                                                        details,
                                                        nextMaintenanceDueOn)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return UUID.fromString(
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString());
    }

    private int countMaintenanceRecordsForUser(
            String email) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM maintenance_records "
                        + "WHERE lifecycle_record_id IN ("
                        + "SELECT id FROM lifecycle_records "
                        + "WHERE user_id = ?"
                        + ")",
                Integer.class,
                userId(email));
    }

    private String lifecycleJson(
            String itemName,
            LocalDate acquiredOn) {

        return """
                {
                    "itemName": "%s",
                    "atProductId": null,
                    "atCategoryId": null,
                    "acquiredOn": "%s",
                    "acquisitionMethod": "PURCHASED",
                    "conditionAtAcquisition": "NEW",
                    "lifecycleStatus": "IN_USE",
                    "warrantyExpiresOn": null,
                    "preferredProviderServiceLocationId": null,
                    "replacementDueOn": null,
                    "reassessmentDueOn": null,
                    "replacesLifecycleRecordId": null
                }
                """.formatted(
                itemName,
                acquiredOn);
    }

    private String validMaintenanceJson(
            UUID lifecycleRecordId) {

        return maintenanceJson(
                lifecycleRecordId,
                "ROUTINE",
                LocalDate.now().minusDays(1),
                null,
                "Equipment checked.",
                null);
    }

    private String maintenanceJson(
            UUID lifecycleRecordId,
            String maintenanceType,
            LocalDate maintenanceDate,
            UUID providerServiceLocationId,
            String details,
            LocalDate nextMaintenanceDueOn) {

        String providerLocationField =
                providerServiceLocationId == null
                        ? "null"
                        : "\"" + providerServiceLocationId + "\"";

        String detailsField =
                details == null
                        ? "null"
                        : "\"" + escapeJson(details) + "\"";

        String nextDueField =
                nextMaintenanceDueOn == null
                        ? "null"
                        : "\"" + nextMaintenanceDueOn + "\"";

        return """
                {
                    "lifecycleRecordId": "%s",
                    "maintenanceType": "%s",
                    "maintenanceDate": "%s",
                    "providerServiceLocationId": %s,
                    "details": %s,
                    "nextMaintenanceDueOn": %s
                }
                """.formatted(
                lifecycleRecordId,
                maintenanceType,
                maintenanceDate,
                providerLocationField,
                detailsField,
                nextDueField);
    }

    private String validUpdateJson() {

        return updateJson(
                "ROUTINE",
                LocalDate.now().minusDays(1),
                null,
                "Equipment checked.",
                null);
    }

    private String updateJson(
            String maintenanceType,
            LocalDate maintenanceDate,
            UUID providerServiceLocationId,
            String details,
            LocalDate nextMaintenanceDueOn) {

        String providerLocationField =
                providerServiceLocationId == null
                        ? "null"
                        : "\"" + providerServiceLocationId + "\"";

        String detailsField =
                details == null
                        ? "null"
                        : "\"" + escapeJson(details) + "\"";

        String nextDueField =
                nextMaintenanceDueOn == null
                        ? "null"
                        : "\"" + nextMaintenanceDueOn + "\"";

        return """
                {
                    "maintenanceType": "%s",
                    "maintenanceDate": "%s",
                    "providerServiceLocationId": %s,
                    "details": %s,
                    "nextMaintenanceDueOn": %s
                }
                """.formatted(
                maintenanceType,
                maintenanceDate,
                providerLocationField,
                detailsField,
                nextDueField);
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private UUID createProviderServiceLocationChain(
            String code,
            boolean serviceLocationActive,
            String serviceLocationVerificationStatus,
            LocalDate serviceLocationLastVerifiedAt) {

        UUID providerId =
                createProvider(
                        code,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID locationId =
                createProviderLocation(
                        providerId,
                        code,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID serviceId =
                createProviderService(
                        providerId,
                        code,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        return createProviderServiceLocation(
                serviceId,
                locationId,
                serviceLocationActive,
                serviceLocationVerificationStatus,
                serviceLocationLastVerifiedAt);
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