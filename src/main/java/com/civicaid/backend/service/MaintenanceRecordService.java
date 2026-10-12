package com.civicaid.backend.service;

import com.civicaid.backend.dto.MaintenanceRecordCreateRequest;
import com.civicaid.backend.dto.MaintenanceRecordResponse;
import com.civicaid.backend.dto.MaintenanceRecordUpdateRequest;
import com.civicaid.backend.entity.LifecycleRecord;
import com.civicaid.backend.entity.MaintenanceRecord;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderLocation;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceLocation;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.LifecycleRecordRepository;
import com.civicaid.backend.repository.MaintenanceRecordRepository;
import com.civicaid.backend.repository.ProviderServiceLocationRepository;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceRecordService {

    private static final int DIRECTORY_FRESHNESS_DAYS = 90;

    private static final String PROHIBITED_DETAILS_PATTERN =
            "(?i).*\\b("
                    + "diagnos\\w*|medical|clinical|clinician|patient|"
                    + "symptom\\w*|treatment|prescription|medication|"
                    + "certificate|certification|"
                    + "payment|invoice|receipt|upi|price|cost|paid|"
                    + "doctor|physician|therapist|staff|employee|technician"
                    + ")\\b.*";

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final LifecycleRecordRepository lifecycleRecordRepository;
    private final ProviderServiceLocationRepository
            providerServiceLocationRepository;
    private final UserRepository userRepository;

    public MaintenanceRecordService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            LifecycleRecordRepository lifecycleRecordRepository,
            ProviderServiceLocationRepository
                    providerServiceLocationRepository,
            UserRepository userRepository) {

        this.maintenanceRecordRepository =
                maintenanceRecordRepository;
        this.lifecycleRecordRepository =
                lifecycleRecordRepository;
        this.providerServiceLocationRepository =
                providerServiceLocationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getMyMaintenanceRecords(
            UUID lifecycleRecordId,
            String userEmail) {

        User user = requireUser(userEmail);

        requireOwnedLifecycleRecord(
                lifecycleRecordId,
                user.getId());

        return maintenanceRecordRepository
                .findByLifecycleRecord_User_IdAndLifecycleRecord_IdOrderByMaintenanceDateDescCreatedAtDesc(
                        user.getId(),
                        lifecycleRecordId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceRecordResponse getMyMaintenanceRecord(
            UUID maintenanceRecordId,
            String userEmail) {

        User user = requireUser(userEmail);

        MaintenanceRecord record =
                maintenanceRecordRepository
                        .findByIdAndLifecycleRecord_User_Id(
                                maintenanceRecordId,
                                user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Maintenance record not found"));

        return toResponse(record);
    }

    @Transactional
    public MaintenanceRecordResponse createMaintenanceRecord(
            MaintenanceRecordCreateRequest request,
            String userEmail) {

        User user = requireUser(userEmail);

        LifecycleRecord lifecycleRecord =
                requireOwnedLifecycleRecord(
                        request.lifecycleRecordId(),
                        user.getId());

        validateMaintenanceDates(
                request.maintenanceDate(),
                request.nextMaintenanceDueOn(),
                lifecycleRecord.getAcquiredOn());

        String details =
                normalizeAndValidateDetails(
                        request.details());

        ProviderServiceLocation providerServiceLocation =
                resolveProviderServiceLocation(
                        request.providerServiceLocationId());

        MaintenanceRecord record =
                new MaintenanceRecord();

        record.setLifecycleRecord(lifecycleRecord);
        record.setMaintenanceType(
                request.maintenanceType());
        record.setMaintenanceDate(
                request.maintenanceDate());
        record.setProviderServiceLocation(
                providerServiceLocation);
        record.setDetails(details);
        record.setNextMaintenanceDueOn(
                request.nextMaintenanceDueOn());

        MaintenanceRecord savedRecord =
                maintenanceRecordRepository.save(record);

        return toResponse(savedRecord);
    }

    @Transactional
    public MaintenanceRecordResponse updateMaintenanceRecord(
            UUID maintenanceRecordId,
            MaintenanceRecordUpdateRequest request,
            String userEmail) {

        User user = requireUser(userEmail);

        MaintenanceRecord record =
                maintenanceRecordRepository
                        .findByIdAndLifecycleRecord_User_Id(
                                maintenanceRecordId,
                                user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Maintenance record not found"));

        LifecycleRecord lifecycleRecord =
                record.getLifecycleRecord();

        validateMaintenanceDates(
                request.maintenanceDate(),
                request.nextMaintenanceDueOn(),
                lifecycleRecord.getAcquiredOn());

        String details =
                normalizeAndValidateDetails(
                        request.details());

        ProviderServiceLocation providerServiceLocation =
                resolveProviderServiceLocation(
                        request.providerServiceLocationId());

        record.setMaintenanceType(
                request.maintenanceType());
        record.setMaintenanceDate(
                request.maintenanceDate());
        record.setProviderServiceLocation(
                providerServiceLocation);
        record.setDetails(details);
        record.setNextMaintenanceDueOn(
                request.nextMaintenanceDueOn());

        MaintenanceRecord savedRecord =
                maintenanceRecordRepository.save(record);

        return toResponse(savedRecord);
    }

    @Transactional
    public void deleteMyMaintenanceRecord(
            UUID maintenanceRecordId,
            String userEmail) {

        User user = requireUser(userEmail);

        MaintenanceRecord record =
                maintenanceRecordRepository
                        .findByIdAndLifecycleRecord_User_Id(
                                maintenanceRecordId,
                                user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Maintenance record not found"));

        maintenanceRecordRepository.delete(record);
    }

    private LifecycleRecord requireOwnedLifecycleRecord(
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
                    UUID providerServiceLocationId) {

        if (providerServiceLocationId == null) {
            return null;
        }

        ProviderServiceLocation serviceLocation =
                providerServiceLocationRepository
                        .findById(providerServiceLocationId)
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

        return isEligible(
                location.isActive(),
                location.getVerificationStatus(),
                location.getLastVerifiedAt(),
                freshnessCutoff);
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

    private void validateMaintenanceDates(
            LocalDate maintenanceDate,
            LocalDate nextMaintenanceDueOn,
            LocalDate acquiredOn) {

        LocalDate today = LocalDate.now();

        if (maintenanceDate.isAfter(today)) {
            throw new BadRequestException(
                    "Maintenance date cannot be in the future");
        }

        if (maintenanceDate.isBefore(acquiredOn)) {
            throw new BadRequestException(
                    "Maintenance date cannot be before the lifecycle acquisition date");
        }

        if (nextMaintenanceDueOn != null
                && nextMaintenanceDueOn.isBefore(
                        maintenanceDate)) {

            throw new BadRequestException(
                    "Next maintenance due date cannot be before the maintenance date");
        }
    }

    private String normalizeAndValidateDetails(
            String details) {

        if (details == null) {
            return null;
        }

        String normalized =
                details.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > 1000) {
            throw new BadRequestException(
                    "Maintenance details must not exceed 1000 characters");
        }

        if (normalized.matches(
                PROHIBITED_DETAILS_PATTERN)) {

            throw new BadRequestException(
                    "Maintenance details must contain only equipment or service facts");
        }

        return normalized;
    }

    private MaintenanceRecordResponse toResponse(
            MaintenanceRecord record) {

        UUID lifecycleRecordId =
                record.getLifecycleRecord() != null
                        ? record.getLifecycleRecord().getId()
                        : null;

        UUID providerServiceLocationId =
                record.getProviderServiceLocation() != null
                        ? record.getProviderServiceLocation().getId()
                        : null;

        return new MaintenanceRecordResponse(
                record.getId(),
                lifecycleRecordId,
                record.getMaintenanceType(),
                record.getMaintenanceDate(),
                providerServiceLocationId,
                record.getDetails(),
                record.getNextMaintenanceDueOn(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private User requireUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }
}