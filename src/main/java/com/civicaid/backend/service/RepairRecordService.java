package com.civicaid.backend.service;

import com.civicaid.backend.dto.RepairRecordCreateRequest;
import com.civicaid.backend.dto.RepairRecordResponse;
import com.civicaid.backend.dto.RepairRecordUpdateRequest;
import com.civicaid.backend.entity.LifecycleRecord;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderLocation;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceLocation;
import com.civicaid.backend.entity.RepairRecord;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.LifecycleRecordRepository;
import com.civicaid.backend.repository.ProviderServiceLocationRepository;
import com.civicaid.backend.repository.RepairRecordRepository;
import com.civicaid.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepairRecordService {

    private static final int DIRECTORY_FRESHNESS_DAYS = 90;

    private static final Pattern PROHIBITED_CONTENT_PATTERN = Pattern.compile(
            "\\b("
                    + "diagnos\\w*|medical|clinical|clinician|patient|"
                    + "symptom\\w*|treatment|prescription|medication|"
                    + "certificate|certification|payment|invoice|receipt|"
                    + "upi|price|cost|paid|doctor|physician|therapist|"
                    + "staff|employee|technician"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    private final RepairRecordRepository repairRecordRepository;
    private final LifecycleRecordRepository lifecycleRecordRepository;
    private final ProviderServiceLocationRepository providerServiceLocationRepository;
    private final UserRepository userRepository;

    public RepairRecordService(
            RepairRecordRepository repairRecordRepository,
            LifecycleRecordRepository lifecycleRecordRepository,
            ProviderServiceLocationRepository providerServiceLocationRepository,
            UserRepository userRepository) {

        this.repairRecordRepository = repairRecordRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.providerServiceLocationRepository =
                providerServiceLocationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RepairRecordResponse> getMyRepairRecords(
            UUID lifecycleRecordId,
            String actorEmail) {

        User user = requireUser(actorEmail);

        getOwnedLifecycleRecord(
                lifecycleRecordId,
                user.getId());

        return repairRecordRepository
                .findByLifecycleRecord_User_IdAndLifecycleRecord_IdOrderByRepairDateDescCreatedAtDesc(
                        user.getId(),
                        lifecycleRecordId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepairRecordResponse getMyRepairRecord(
            UUID repairRecordId,
            String actorEmail) {

        User user = requireUser(actorEmail);

        RepairRecord repairRecord = repairRecordRepository
                .findByIdAndLifecycleRecord_User_Id(
                        repairRecordId,
                        user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair record not found"));

        return toResponse(repairRecord);
    }

    @Transactional
    public RepairRecordResponse createRepairRecord(
            RepairRecordCreateRequest request,
            String actorEmail) {

        User user = requireUser(actorEmail);

        LifecycleRecord lifecycleRecord =
                getOwnedLifecycleRecord(
                        request.lifecycleRecordId(),
                        user.getId());

        validateRepairDate(
                request.repairDate(),
                lifecycleRecord.getAcquiredOn());

        String issueDescription =
                normalizeRequiredText(
                        request.issueDescription(),
                        "Issue description");

        validateDeviceOnlyText(
                issueDescription,
                "Issue description");

        String workSummary =
                normalizeOptionalText(
                        request.workSummary());

        if (workSummary != null) {
            validateDeviceOnlyText(
                    workSummary,
                    "Work summary");
        }

        ProviderServiceLocation providerServiceLocation = null;

        if (request.providerServiceLocationId() != null) {
            providerServiceLocation =
                    resolveProviderServiceLocation(
                            request.providerServiceLocationId());
        }

        validateCost(request.costInr());

        RepairRecord repairRecord = new RepairRecord();

        repairRecord.setLifecycleRecord(lifecycleRecord);
        repairRecord.setRepairDate(request.repairDate());
        repairRecord.setProviderServiceLocation(
                providerServiceLocation);
        repairRecord.setIssueDescription(issueDescription);
        repairRecord.setRepairOutcomeStatus(
                request.repairOutcomeStatus());
        repairRecord.setWorkSummary(workSummary);
        repairRecord.setCostInr(request.costInr());
        repairRecord.setWarrantyUsed(request.warrantyUsed());

        return toResponse(
                repairRecordRepository.save(repairRecord));
    }

    @Transactional
    public RepairRecordResponse updateRepairRecord(
            UUID repairRecordId,
            RepairRecordUpdateRequest request,
            String actorEmail) {

        User user = requireUser(actorEmail);

        RepairRecord repairRecord = repairRecordRepository
                .findByIdAndLifecycleRecord_User_Id(
                        repairRecordId,
                        user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair record not found"));

        LifecycleRecord lifecycleRecord =
                repairRecord.getLifecycleRecord();

        validateRepairDate(
                request.repairDate(),
                lifecycleRecord.getAcquiredOn());

        String issueDescription =
                normalizeRequiredText(
                        request.issueDescription(),
                        "Issue description");

        validateDeviceOnlyText(
                issueDescription,
                "Issue description");

        String workSummary =
                normalizeOptionalText(
                        request.workSummary());

        if (workSummary != null) {
            validateDeviceOnlyText(
                    workSummary,
                    "Work summary");
        }

        ProviderServiceLocation providerServiceLocation = null;

        if (request.providerServiceLocationId() != null) {
            providerServiceLocation =
                    resolveProviderServiceLocation(
                            request.providerServiceLocationId());
        }

        validateCost(request.costInr());

        repairRecord.setRepairDate(
                request.repairDate());

        repairRecord.setProviderServiceLocation(
                providerServiceLocation);

        repairRecord.setIssueDescription(
                issueDescription);

        repairRecord.setRepairOutcomeStatus(
                request.repairOutcomeStatus());

        repairRecord.setWorkSummary(
                workSummary);

        repairRecord.setCostInr(
                request.costInr());

        repairRecord.setWarrantyUsed(
                request.warrantyUsed());

        return toResponse(
                repairRecordRepository.save(repairRecord));
    }

    @Transactional
    public void deleteMyRepairRecord(
            UUID repairRecordId,
            String actorEmail) {

        User user = requireUser(actorEmail);

        RepairRecord repairRecord = repairRecordRepository
                .findByIdAndLifecycleRecord_User_Id(
                        repairRecordId,
                        user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair record not found"));

        repairRecordRepository.delete(repairRecord);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    private LifecycleRecord getOwnedLifecycleRecord(
            UUID lifecycleRecordId,
            UUID userId) {

        return lifecycleRecordRepository
                .findByIdAndUser_Id(
                        lifecycleRecordId,
                        userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lifecycle record not found"));
    }

    private ProviderServiceLocation
            resolveProviderServiceLocation(
                    UUID serviceLocationId) {

        ProviderServiceLocation serviceLocation =
                providerServiceLocationRepository
                        .findById(serviceLocationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider service location not found"));

        if (!isEligibleServiceLocation(
                serviceLocation)) {

            throw new ResourceNotFoundException(
                    "Provider service location is not currently available");
        }

        return serviceLocation;
    }

    private boolean isEligibleServiceLocation(
            ProviderServiceLocation serviceLocation) {

        LocalDate today = LocalDate.now();

        LocalDate freshnessCutoff =
                today.minusDays(
                        DIRECTORY_FRESHNESS_DAYS);

        if (!isEligible(
                serviceLocation.isActive(),
                serviceLocation.getVerificationStatus(),
                serviceLocation.getLastVerifiedAt(),
                freshnessCutoff)) {

            return false;
        }

        ProviderService service =
                serviceLocation.getProviderService();

        ProviderLocation location =
                serviceLocation.getProviderLocation();

        if (service == null || location == null) {
            return false;
        }

        if (!isEligible(
                service.isActive(),
                service.getVerificationStatus(),
                service.getLastVerifiedAt(),
                freshnessCutoff)) {

            return false;
        }

        Provider provider =
                service.getProvider();

        if (provider == null
                || !isEligible(
                        provider.isActive(),
                        provider.getVerificationStatus(),
                        provider.getLastVerifiedAt(),
                        freshnessCutoff)) {

            return false;
        }

        if (!isEligible(
                location.isActive(),
                location.getVerificationStatus(),
                location.getLastVerifiedAt(),
                freshnessCutoff)) {

            return false;
        }

        return true;
    }

    private boolean isEligible(
            boolean active,
            VerificationStatus verificationStatus,
            LocalDate lastVerifiedAt,
            LocalDate freshnessCutoff) {

        if (!active
                || verificationStatus
                != VerificationStatus.VERIFIED
                || lastVerifiedAt == null) {

            return false;
        }

        LocalDate today = LocalDate.now();

        return !lastVerifiedAt.isAfter(today)
                && !lastVerifiedAt.isBefore(
                        freshnessCutoff);
    }

    private void validateRepairDate(
            LocalDate repairDate,
            LocalDate acquiredOn) {

        if (repairDate == null) {
            throw new BadRequestException(
                    "Repair date is required");
        }

        if (repairDate.isAfter(LocalDate.now())) {
            throw new BadRequestException(
                    "Repair date cannot be in the future");
        }

        if (acquiredOn != null
                && repairDate.isBefore(acquiredOn)) {

            throw new BadRequestException(
                    "Repair date cannot be before the lifecycle acquisition date");
        }
    }

    private String normalizeRequiredText(
            String value,
            String fieldName) {

        if (value == null) {
            throw new BadRequestException(
                    fieldName + " is required");
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new BadRequestException(
                    fieldName + " is required");
        }

        return normalized;
    }

    private String normalizeOptionalText(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void validateDeviceOnlyText(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            return;
        }

        if (PROHIBITED_CONTENT_PATTERN.matcher(value).find()) {
            throw new BadRequestException(
                    fieldName
                            + " may contain only device or service facts");
        }
    }

    private void validateCost(
            BigDecimal costInr) {

        if (costInr == null) {
            return;
        }

        if (costInr.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Repair cost cannot be negative");
        }

        if (costInr.scale() > 2) {
            throw new BadRequestException(
                    "Repair cost can have at most two decimal places");
        }
    }

    private RepairRecordResponse toResponse(
            RepairRecord repairRecord) {

        return new RepairRecordResponse(
                repairRecord.getId(),
                repairRecord.getLifecycleRecord().getId(),
                repairRecord.getRepairDate(),
                repairRecord.getProviderServiceLocation() == null
                        ? null
                        : repairRecord
                                .getProviderServiceLocation()
                                .getId(),
                repairRecord.getIssueDescription(),
                repairRecord.getRepairOutcomeStatus(),
                repairRecord.getWorkSummary(),
                repairRecord.getCostInr(),
                repairRecord.getWarrantyUsed(),
                repairRecord.getCreatedAt(),
                repairRecord.getUpdatedAt());
    }
}