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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepairRecordApiTests {

    private static final String USER_ONE =
            "repair-user-one@example.org";

    private static final String USER_TWO =
            "repair-user-two@example.org";

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
    void clearRepairTestData() {
        jdbcTemplate.update(
                "DELETE FROM repair_records "
                        + "WHERE lifecycle_record_id IN ("
                        + "SELECT id FROM lifecycle_records "
                        + "WHERE user_id IN ("
                        + "SELECT id FROM users WHERE email IN (?, ?)"
                        + "))",
                USER_ONE,
                USER_TWO);

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
                        + "WHERE code LIKE 'TEST_REPAIR_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM provider_services "
                        + "WHERE code LIKE 'TEST_REPAIR_%'");

        jdbcTemplate.update(
                "DELETE FROM provider_locations "
                        + "WHERE provider_id IN ("
                        + "SELECT id FROM providers "
                        + "WHERE code LIKE 'TEST_REPAIR_%'"
                        + ")");

        jdbcTemplate.update(
                "DELETE FROM providers "
                        + "WHERE code LIKE 'TEST_REPAIR_%'");

        jdbcTemplate.update(
                "DELETE FROM at_products "
                        + "WHERE code LIKE 'TEST_REPAIR_%'");

        jdbcTemplate.update(
                "DELETE FROM at_categories "
                        + "WHERE code LIKE 'TEST_REPAIR_%'");

        jdbcTemplate.update(
                "DELETE FROM users WHERE email IN (?, ?)",
                USER_ONE,
                USER_TWO);
    }

    @Test
    void unauthenticatedGetReturns401() throws Exception {
        mockMvc.perform(
                        get("/api/users/me/repair-records/lifecycle/{lifecycleRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetByIdReturns401() throws Exception {
        mockMvc.perform(
                        get("/api/users/me/repair-records/{repairRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPostReturns401() throws Exception {
        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRepairJson(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPatchReturns401() throws Exception {
        mockMvc.perform(
                        patch("/api/users/me/repair-records/{repairRecordId}",
                                UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRepairUpdateJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteReturns401() throws Exception {
        mockMvc.perform(
                        delete("/api/users/me/repair-records/{repairRecordId}",
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Repair Test Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(5),
                                                "Device stopped charging",
                                                "RESOLVED",
                                                "Charging connector was adjusted",
                                                "1250.50",
                                                true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(
                        jsonPath("$.lifecycleRecordId")
                                .value(lifecycleRecordId.toString()))
                .andExpect(
                        jsonPath("$.repairDate")
                                .value(
                                        LocalDate.now()
                                                .minusDays(5)
                                                .toString()))
                .andExpect(
                        jsonPath("$.issueDescription")
                                .value("Device stopped charging"))
                .andExpect(
                        jsonPath("$.repairOutcomeStatus")
                                .value("RESOLVED"))
                .andExpect(
                        jsonPath("$.workSummary")
                                .value(
                                        "Charging connector was adjusted"))
                .andExpect(
                        jsonPath("$.costInr")
                                .value(1250.5))
                .andExpect(
                        jsonPath("$.warrantyUsed")
                                .value(true));
    }

    @Test
    void authenticatedUserCanListOwnRepairRecords()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Repair List Device",
                        LocalDate.now().minusDays(30));

        createRepairRecord(
                USER_ONE,
                lifecycleRecordId,
                LocalDate.now().minusDays(10),
                "Battery connection issue",
                "RESOLVED",
                "Connection adjusted",
                null,
                false);

        createRepairRecord(
                USER_ONE,
                lifecycleRecordId,
                LocalDate.now().minusDays(5),
                "Charging issue",
                "PARTIALLY_RESOLVED",
                "Connector inspected",
                null,
                null);

        mockMvc.perform(
                        get(
                                "/api/users/me/repair-records/lifecycle/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[0].issueDescription")
                                .value("Charging issue"))
                .andExpect(
                        jsonPath("$[1].issueDescription")
                                .value("Battery connection issue"));
    }

    @Test
    void authenticatedUserCanGetOwnRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Repair Get Device",
                        LocalDate.now().minusDays(20));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(3),
                        "Loose connector",
                        "RESOLVED",
                        "Connector tightened",
                        "300.00",
                        false);

        mockMvc.perform(
                        get(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(repairRecordId.toString()))
                .andExpect(
                        jsonPath("$.lifecycleRecordId")
                                .value(lifecycleRecordId.toString()))
                .andExpect(
                        jsonPath("$.issueDescription")
                                .value("Loose connector"))
                .andExpect(
                        jsonPath("$.repairOutcomeStatus")
                                .value("RESOLVED"));
    }

    @Test
    void userCannotGetAnotherUsersRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private Repair Device",
                        LocalDate.now().minusDays(20));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(3),
                        "Private device issue",
                        "RESOLVED",
                        "Private repair work",
                        null,
                        null);

        mockMvc.perform(
                        get(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotListAnotherUsersRepairRecords()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Private List Device",
                        LocalDate.now().minusDays(20));

        createRepairRecord(
                USER_ONE,
                lifecycleRecordId,
                LocalDate.now().minusDays(3),
                "Private issue",
                "RESOLVED",
                "Private repair",
                null,
                null);

        mockMvc.perform(
                        get(
                                "/api/users/me/repair-records/lifecycle/{lifecycleRecordId}",
                                lifecycleRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCanUpdateOwnRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Repair Update Device",
                        LocalDate.now().minusDays(30));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(10),
                        "Original issue",
                        "UNRESOLVED",
                        "Initial inspection",
                        "100.00",
                        false);

        mockMvc.perform(
                        patch(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairUpdateJson(
                                                LocalDate.now().minusDays(2),
                                                "Updated issue",
                                                "RESOLVED",
                                                "Connector replaced",
                                                "750.25",
                                                true)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.issueDescription")
                                .value("Updated issue"))
                .andExpect(
                        jsonPath("$.repairOutcomeStatus")
                                .value("RESOLVED"))
                .andExpect(
                        jsonPath("$.workSummary")
                                .value("Connector replaced"))
                .andExpect(
                        jsonPath("$.costInr")
                                .value(750.25))
                .andExpect(
                        jsonPath("$.warrantyUsed")
                                .value(true));
    }

    @Test
    void userCannotUpdateAnotherUsersRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Protected Update Device",
                        LocalDate.now().minusDays(30));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(5),
                        "Original issue",
                        "UNRESOLVED",
                        "Initial inspection",
                        null,
                        null);

        mockMvc.perform(
                        patch(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_TWO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        validRepairUpdateJson()))
                .andExpect(status().isNotFound());

        assertEquals(
                1,
                countRepairRecordsForUser(USER_ONE));
    }

    @Test
    void userCanDeleteOwnRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Repair Delete Device",
                        LocalDate.now().minusDays(30));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(5),
                        "Delete test issue",
                        "RESOLVED",
                        "Repair completed",
                        null,
                        null);

        mockMvc.perform(
                        delete(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isNoContent());

        assertEquals(
                0,
                countRepairRecordsForUser(USER_ONE));
    }

    @Test
    void userCannotDeleteAnotherUsersRepairRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Protected Delete Device",
                        LocalDate.now().minusDays(30));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(5),
                        "Protected issue",
                        "RESOLVED",
                        "Protected repair",
                        null,
                        null);

        mockMvc.perform(
                        delete(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_TWO)))
                .andExpect(status().isNotFound());

        assertEquals(
                1,
                countRepairRecordsForUser(USER_ONE));
    }

    @Test
    void nonexistentLifecycleRecordReturns404()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        validRepairJson(
                                                UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotCreateRepairForAnotherUsersLifecycleRecord()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Owner Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_TWO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        validRepairJson(
                                                lifecycleRecordId)))
                .andExpect(status().isNotFound());

        assertEquals(
                0,
                countRepairRecordsForUser(USER_ONE));
    }

    @Test
    void repairDateBeforeAcquisitionReturns400()
            throws Exception {

        LocalDate acquiredOn =
                LocalDate.now().minusDays(10);

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Date Validation Device",
                        acquiredOn);

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                acquiredOn.minusDays(1),
                                                "Issue",
                                                "RESOLVED",
                                                "Work",
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void futureRepairDateReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Future Repair Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().plusDays(1),
                                                "Future issue",
                                                "RESOLVED",
                                                "Future work",
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredFieldsReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "lifecycleRecordId": null,
                                            "repairDate": null,
                                            "issueDescription": "",
                                            "repairOutcomeStatus": null
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRepairOutcomeReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Invalid Outcome Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "lifecycleRecordId": "%s",
                                            "repairDate": "%s",
                                            "issueDescription": "Device issue",
                                            "repairOutcomeStatus": "INVALID_OUTCOME"
                                        }
                                        """.formatted(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankIssueDescriptionReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Blank Issue Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "   ",
                                                "RESOLVED",
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void issueDescriptionIsTrimmed()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Trim Issue Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "  Charging port cleaned  ",
                                                "RESOLVED",
                                                "  Port cleaned  ",
                                                null,
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.issueDescription")
                                .value("Charging port cleaned"))
                .andExpect(
                        jsonPath("$.workSummary")
                                .value("Port cleaned"));
    }

    @Test
    void medicalContentInIssueDescriptionReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Medical Content Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Medical diagnosis recorded",
                                                "RESOLVED",
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentContentInIssueDescriptionReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Payment Content Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Invoice payment received",
                                                "RESOLVED",
                                                null,
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void medicalContentInWorkSummaryReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Medical Summary Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device adjustment",
                                                "RESOLVED",
                                                "Clinical treatment completed",
                                                null,
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeCostReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Negative Cost Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device repair",
                                                "RESOLVED",
                                                "Part replaced",
                                                "-1.00",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void costWithMoreThanTwoDecimalPlacesReturns400()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Decimal Cost Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device repair",
                                                "RESOLVED",
                                                "Part replaced",
                                                "100.123",
                                                null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zeroCostIsAccepted()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Zero Cost Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device inspected",
                                                "RESOLVED",
                                                "No replacement required",
                                                "0.00",
                                                false)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.costInr")
                                .value(0));
    }

    @Test
    void allRepairOutcomeStatusesAreAccepted()
            throws Exception {

        String[] outcomes = {
            "RESOLVED",
            "PARTIALLY_RESOLVED",
            "UNRESOLVED",
            "REPLACEMENT_CONSIDERED"
        };

        for (String outcome : outcomes) {
            UUID lifecycleRecordId =
                    createLifecycleRecord(
                            USER_ONE,
                            "Outcome " + outcome,
                            LocalDate.now().minusDays(30));

            mockMvc.perform(
                            post("/api/users/me/repair-records")
                                    .with(authenticatedUser(USER_ONE))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            repairJson(
                                                    lifecycleRecordId,
                                                    LocalDate.now().minusDays(2),
                                                    "Device issue " + outcome,
                                                    outcome,
                                                    "Repair work " + outcome,
                                                    null,
                                                    null)))
                    .andExpect(status().isCreated())
                    .andExpect(
                            jsonPath("$.repairOutcomeStatus")
                                    .value(outcome));
        }
    }

    @Test
    void replacementConsideredDoesNotChangeLifecycleStatus()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Replacement Considered Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device requires replacement",
                                                "REPLACEMENT_CONSIDERED",
                                                "Replacement discussed",
                                                null,
                                                null)))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.repairOutcomeStatus")
                                .value("REPLACEMENT_CONSIDERED"));

        assertEquals(
                "IN_USE",
                jdbcTemplate.queryForObject(
                        "SELECT lifecycle_status "
                                + "FROM lifecycle_records WHERE id = ?",
                        String.class,
                        lifecycleRecordId));
    }

    @Test
    void providerServiceLocationMustBeEligibleAtCreation()
            throws Exception {

        UUID providerId =
                createProvider(
                        "TEST_REPAIR_PROVIDER_INACTIVE",
                        false,
                        "VERIFIED",
                        LocalDate.now());

        UUID providerLocationId =
                createProviderLocation(
                        providerId,
                        "TEST_REPAIR_LOCATION_INACTIVE",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID serviceId =
                createProviderService(
                        providerId,
                        "TEST_REPAIR_SERVICE_INACTIVE",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID serviceLocationId =
                createProviderServiceLocation(
                        serviceId,
                        providerLocationId,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Provider Eligibility Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device issue",
                                                "RESOLVED",
                                                "Repair work",
                                                null,
                                                null,
                                                serviceLocationId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleProviderServiceLocationCannotBeSelected()
            throws Exception {

        UUID providerId =
                createProvider(
                        "TEST_REPAIR_PROVIDER_STALE",
                        true,
                        "VERIFIED",
                        LocalDate.now().minusDays(91));

        UUID providerLocationId =
                createProviderLocation(
                        providerId,
                        "TEST_REPAIR_LOCATION_STALE",
                        true,
                        "VERIFIED",
                        LocalDate.now().minusDays(91));

        UUID serviceId =
                createProviderService(
                        providerId,
                        "TEST_REPAIR_SERVICE_STALE",
                        true,
                        "VERIFIED",
                        LocalDate.now().minusDays(91));

        UUID serviceLocationId =
                createProviderServiceLocation(
                        serviceId,
                        providerLocationId,
                        true,
                        "VERIFIED",
                        LocalDate.now().minusDays(91));

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Stale Provider Device",
                        LocalDate.now().minusDays(30));

        mockMvc.perform(
                        post("/api/users/me/repair-records")
                                .with(authenticatedUser(USER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        repairJson(
                                                lifecycleRecordId,
                                                LocalDate.now().minusDays(2),
                                                "Device issue",
                                                "RESOLVED",
                                                "Repair work",
                                                null,
                                                null,
                                                serviceLocationId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void selectedProviderServiceLocationRemainsHistoricalAfterArchive()
            throws Exception {

        UUID providerId =
                createProvider(
                        "TEST_REPAIR_PROVIDER_HISTORY",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID providerLocationId =
                createProviderLocation(
                        providerId,
                        "TEST_REPAIR_LOCATION_HISTORY",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID serviceId =
                createProviderService(
                        providerId,
                        "TEST_REPAIR_SERVICE_HISTORY",
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID serviceLocationId =
                createProviderServiceLocation(
                        serviceId,
                        providerLocationId,
                        true,
                        "VERIFIED",
                        LocalDate.now());

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Historical Provider Device",
                        LocalDate.now().minusDays(30));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(2),
                        "Device issue",
                        "RESOLVED",
                        "Repair completed",
                        null,
                        null,
                        serviceLocationId);

        jdbcTemplate.update(
                "UPDATE provider_service_locations "
                        + "SET active = false WHERE id = ?",
                serviceLocationId);

        mockMvc.perform(
                        get(
                                "/api/users/me/repair-records/{repairRecordId}",
                                repairRecordId)
                                .with(authenticatedUser(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.providerServiceLocationId")
                                .value(serviceLocationId.toString()));
    }

    @Test
    void clientCannotControlRepairOwnership()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Ownership Test Device",
                        LocalDate.now().minusDays(30));

        String response =
                mockMvc.perform(
                                post("/api/users/me/repair-records")
                                        .with(authenticatedUser(USER_ONE))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {
                                                    "lifecycleRecordId": "%s",
                                                    "repairDate": "%s",
                                                    "issueDescription": "Ownership test issue",
                                                    "repairOutcomeStatus": "RESOLVED",
                                                    "workSummary": "Ownership test repair",
                                                    "costInr": 100.00,
                                                    "warrantyUsed": false,
                                                    "userId": "%s"
                                                }
                                                """.formatted(
                                                        lifecycleRecordId,
                                                        LocalDate.now().minusDays(2),
                                                        userId(USER_TWO))))
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.userId")
                                        .doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String repairRecordId =
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString();

        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM repair_records r "
                                + "JOIN lifecycle_records l "
                                + "ON r.lifecycle_record_id = l.id "
                                + "WHERE r.id = ? AND l.user_id = ?",
                        Integer.class,
                        UUID.fromString(repairRecordId),
                        userId(USER_ONE)));

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM repair_records r "
                                + "JOIN lifecycle_records l "
                                + "ON r.lifecycle_record_id = l.id "
                                + "WHERE r.id = ? AND l.user_id = ?",
                        Integer.class,
                        UUID.fromString(repairRecordId),
                        userId(USER_TWO)));
    }

    @Test
    void deletingLifecycleRecordDeletesItsRepairRecords()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Cascade Repair Device",
                        LocalDate.now().minusDays(30));

        createRepairRecord(
                USER_ONE,
                lifecycleRecordId,
                LocalDate.now().minusDays(2),
                "Cascade test issue",
                "RESOLVED",
                "Cascade test repair",
                null,
                null);

        assertEquals(
                1,
                countRepairRecordsForLifecycle(
                        lifecycleRecordId));

        jdbcTemplate.update(
                "DELETE FROM lifecycle_records WHERE id = ?",
                lifecycleRecordId);

        assertEquals(
                0,
                countRepairRecordsForLifecycle(
                        lifecycleRecordId));
    }

    @Test
    void maintenanceAndRepairRecordsRemainSeparate()
            throws Exception {

        UUID lifecycleRecordId =
                createLifecycleRecord(
                        USER_ONE,
                        "Separate Records Device",
                        LocalDate.now().minusDays(30));

        UUID repairRecordId =
                createRepairRecord(
                        USER_ONE,
                        lifecycleRecordId,
                        LocalDate.now().minusDays(2),
                        "Repair issue",
                        "RESOLVED",
                        "Repair work",
                        null,
                        null);

        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM repair_records "
                                + "WHERE id = ?",
                        Integer.class,
                        repairRecordId));
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
            user.setPasswordHash(
                    "test-only-password-hash");
            user.setAccountStatus(
                    AccountStatus.ACTIVE);
            return userRepository.save(user);
        });
    }

    private UUID userId(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow()
                .getId();
    }

    private int countRepairRecordsForUser(
            String email) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM repair_records r "
                        + "JOIN lifecycle_records l "
                        + "ON r.lifecycle_record_id = l.id "
                        + "WHERE l.user_id = ?",
                Integer.class,
                userId(email));
    }

    private int countRepairRecordsForLifecycle(
            UUID lifecycleRecordId) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM repair_records "
                        + "WHERE lifecycle_record_id = ?",
                Integer.class,
                lifecycleRecordId);
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

    private UUID createRepairRecord(
            String email,
            UUID lifecycleRecordId,
            LocalDate repairDate,
            String issueDescription,
            String outcome,
            String workSummary,
            String costInr,
            Boolean warrantyUsed)
            throws Exception {

        return createRepairRecord(
                email,
                lifecycleRecordId,
                repairDate,
                issueDescription,
                outcome,
                workSummary,
                costInr,
                warrantyUsed,
                null);
    }

    private UUID createRepairRecord(
            String email,
            UUID lifecycleRecordId,
            LocalDate repairDate,
            String issueDescription,
            String outcome,
            String workSummary,
            String costInr,
            Boolean warrantyUsed,
            UUID providerServiceLocationId)
            throws Exception {

        String response =
                mockMvc.perform(
                                post(
                                        "/api/users/me/repair-records")
                                        .with(authenticatedUser(email))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                repairJson(
                                                        lifecycleRecordId,
                                                        repairDate,
                                                        issueDescription,
                                                        outcome,
                                                        workSummary,
                                                        costInr,
                                                        warrantyUsed,
                                                        providerServiceLocationId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return UUID.fromString(
                com.jayway.jsonpath.JsonPath
                        .read(response, "$.id")
                        .toString());
    }

    private String validRepairJson(
            UUID lifecycleRecordId) {

        return repairJson(
                lifecycleRecordId,
                LocalDate.now().minusDays(2),
                "Device issue",
                "RESOLVED",
                "Repair completed",
                null,
                null);
    }

    private String repairJson(
            UUID lifecycleRecordId,
            LocalDate repairDate,
            String issueDescription,
            String outcome,
            String workSummary,
            String costInr,
            Boolean warrantyUsed) {

        return repairJson(
                lifecycleRecordId,
                repairDate,
                issueDescription,
                outcome,
                workSummary,
                costInr,
                warrantyUsed,
                null);
    }

    private String repairJson(
            UUID lifecycleRecordId,
            LocalDate repairDate,
            String issueDescription,
            String outcome,
            String workSummary,
            String costInr,
            Boolean warrantyUsed,
            UUID providerServiceLocationId) {

        String workSummaryField =
                workSummary == null
                        ? "null"
                        : "\"" + workSummary + "\"";

        String costField =
                costInr == null
                        ? "null"
                        : costInr;

        String warrantyField =
                warrantyUsed == null
                        ? "null"
                        : warrantyUsed.toString();

        String serviceLocationField =
                providerServiceLocationId == null
                        ? "null"
                        : "\"" + providerServiceLocationId + "\"";

        return """
                {
                    "lifecycleRecordId": "%s",
                    "repairDate": "%s",
                    "providerServiceLocationId": %s,
                    "issueDescription": "%s",
                    "repairOutcomeStatus": "%s",
                    "workSummary": %s,
                    "costInr": %s,
                    "warrantyUsed": %s
                }
                """.formatted(
                lifecycleRecordId,
                repairDate,
                serviceLocationField,
                issueDescription,
                outcome,
                workSummaryField,
                costField,
                warrantyField);
    }

    private String validRepairUpdateJson() {

        return repairUpdateJson(
                LocalDate.now().minusDays(1),
                "Updated device issue",
                "RESOLVED",
                "Updated repair work",
                null,
                false);
    }

    private String repairUpdateJson(
            LocalDate repairDate,
            String issueDescription,
            String outcome,
            String workSummary,
            String costInr,
            Boolean warrantyUsed) {

        String workSummaryField =
                workSummary == null
                        ? "null"
                        : "\"" + workSummary + "\"";

        String costField =
                costInr == null
                        ? "null"
                        : costInr;

        String warrantyField =
                warrantyUsed == null
                        ? "null"
                        : warrantyUsed.toString();

        return """
                {
                    "repairDate": "%s",
                    "providerServiceLocationId": null,
                    "issueDescription": "%s",
                    "repairOutcomeStatus": "%s",
                    "workSummary": %s,
                    "costInr": %s,
                    "warrantyUsed": %s
                }
                """.formatted(
                repairDate,
                issueDescription,
                outcome,
                workSummaryField,
                costField,
                warrantyField);
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