package com.civicaid.backend.service;

import com.civicaid.backend.dto.ProviderServiceLocationCreateRequest;
import com.civicaid.backend.dto.ProviderServiceLocationResponse;
import com.civicaid.backend.dto.ProviderServiceLocationUpdateRequest;
import com.civicaid.backend.dto.ProviderServiceLocationVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.ProviderLocation;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceLocation;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ProviderLocationRepository;
import com.civicaid.backend.repository.ProviderRepository;
import com.civicaid.backend.repository.ProviderServiceLocationRepository;
import com.civicaid.backend.repository.ProviderServiceRepository;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.repository.VerificationRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderServiceLocationService {

    private static final long VERIFICATION_FRESHNESS_DAYS = 90;

    private final ProviderServiceLocationRepository providerServiceLocationRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderLocationRepository providerLocationRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AuditLogService auditLogService;

    public ProviderServiceLocationService(
            ProviderServiceLocationRepository providerServiceLocationRepository,
            ProviderServiceRepository providerServiceRepository,
            ProviderLocationRepository providerLocationRepository,
            ProviderRepository providerRepository,
            UserRepository userRepository,
            VerificationRecordRepository verificationRecordRepository,
            AuditLogService auditLogService) {

        this.providerServiceLocationRepository = providerServiceLocationRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.providerLocationRepository = providerLocationRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceLocationResponse> getPublicServiceLocations() {
        LocalDate today = LocalDate.now();
        LocalDate freshnessCutoff = today.minusDays(VERIFICATION_FRESHNESS_DAYS);

        return providerServiceLocationRepository
                .findPubliclyDiscoverableServiceLocations(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff,
                        today)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceLocationResponse> getPublicServiceLocationsByProvider(
            UUID providerId) {

        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider not found");
        }

        LocalDate today = LocalDate.now();
        LocalDate freshnessCutoff = today.minusDays(VERIFICATION_FRESHNESS_DAYS);

        return providerServiceLocationRepository
                .findPubliclyDiscoverableServiceLocationsByProvider(
                        providerId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff,
                        today)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceLocationResponse> getPublicServiceLocationsByLocation(
            UUID locationId) {

        if (!providerLocationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Provider location not found");
        }

        LocalDate today = LocalDate.now();
        LocalDate freshnessCutoff = today.minusDays(VERIFICATION_FRESHNESS_DAYS);

        return providerServiceLocationRepository
                .findPubliclyDiscoverableServiceLocationsByLocation(
                        locationId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff,
                        today)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceLocationResponse> getServiceLocationsForAdmin() {
        return providerServiceLocationRepository
                .findAllByOrderByProviderLocation_CityAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceLocationResponse> getServiceLocationsByServiceForAdmin(
            UUID providerServiceId) {

        if (!providerServiceRepository.existsById(providerServiceId)) {
            throw new ResourceNotFoundException("Provider service not found");
        }

        return providerServiceLocationRepository
                .findByProviderService_IdOrderByProviderLocation_CityAsc(
                        providerServiceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceLocationResponse> getServiceLocationsByLocationForAdmin(
            UUID providerLocationId) {

        if (!providerLocationRepository.existsById(providerLocationId)) {
            throw new ResourceNotFoundException("Provider location not found");
        }

        return providerServiceLocationRepository
                .findByProviderLocation_IdOrderByProviderService_NameAsc(
                        providerLocationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderServiceLocationResponse getServiceLocationForAdmin(
            UUID serviceLocationId) {

        return toResponse(getServiceLocation(serviceLocationId));
    }

    @Transactional
    public ProviderServiceLocationResponse createServiceLocation(
            UUID providerServiceId,
            ProviderServiceLocationCreateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderService providerService = providerServiceRepository.findById(providerServiceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Provider service not found"));

        ProviderLocation providerLocation = providerLocationRepository
                .findById(request.providerLocationId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Provider location not found"));

        validateSameProvider(providerService, providerLocation);

        if (providerServiceLocationRepository
                .existsByProviderService_IdAndProviderLocation_Id(
                        providerServiceId,
                        providerLocation.getId())) {

            throw new BadRequestException(
                    "This service is already associated with the selected provider location");
        }

        ProviderServiceLocation serviceLocation =
                new ProviderServiceLocation();

        serviceLocation.setProviderService(providerService);
        serviceLocation.setProviderLocation(providerLocation);
        serviceLocation.setSourceName(normalizeRequired(
                request.sourceName(),
                "Source name"));
        serviceLocation.setSourceUrl(normalizeOptional(request.sourceUrl()));
        serviceLocation.setVerificationStatus(
                VerificationStatus.UNVERIFIED);
        serviceLocation.setLastVerifiedAt(null);
        serviceLocation.setActive(true);

        serviceLocation =
                providerServiceLocationRepository.save(serviceLocation);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_LOCATION,
                serviceLocation.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "provider_service_id",
                    "provider_location_id",
                    "source_name",
                    "source_url",
                    "verification_status",
                    "last_verified_at",
                    "active"
                },
                "Provider service-location association created");

        return toResponse(serviceLocation);
    }

    @Transactional
    public ProviderServiceLocationResponse updateServiceLocation(
            UUID serviceLocationId,
            ProviderServiceLocationUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);
        ProviderServiceLocation serviceLocation =
                getServiceLocation(serviceLocationId);

        String sourceName = normalizeRequired(
                request.sourceName(),
                "Source name");
        String sourceUrl = normalizeOptional(request.sourceUrl());

        boolean sourceNameChanged =
                !Objects.equals(serviceLocation.getSourceName(), sourceName);

        boolean sourceUrlChanged =
                !Objects.equals(serviceLocation.getSourceUrl(), sourceUrl);

        if (!sourceNameChanged && !sourceUrlChanged) {
            return toResponse(serviceLocation);
        }

        if (sourceNameChanged) {
            serviceLocation.setSourceName(sourceName);
        }

        if (sourceUrlChanged) {
            serviceLocation.setSourceUrl(sourceUrl);
        }

        serviceLocation.setVerificationStatus(
                VerificationStatus.UNVERIFIED);
        serviceLocation.setLastVerifiedAt(null);

        serviceLocation =
                providerServiceLocationRepository.save(serviceLocation);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_LOCATION,
                serviceLocation.getId(),
                AuditActionType.UPDATE,
                new String[] {
                    "source_name",
                    "source_url",
                    "verification_status",
                    "last_verified_at"
                },
                "Provider service-location updated; verification reset");

        return toResponse(serviceLocation);
    }

    @Transactional
    public ProviderServiceLocationResponse setServiceLocationActive(
            UUID serviceLocationId,
            boolean active,
            String actorEmail) {

        User actor = requireActor(actorEmail);
        ProviderServiceLocation serviceLocation =
                getServiceLocation(serviceLocationId);

        if (serviceLocation.isActive() == active) {
            return toResponse(serviceLocation);
        }

        serviceLocation.setActive(active);
        serviceLocation =
                providerServiceLocationRepository.save(serviceLocation);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_LOCATION,
                serviceLocation.getId(),
                active
                        ? AuditActionType.ACTIVATE
                        : AuditActionType.ARCHIVE,
                new String[] {"active"},
                active
                        ? "Provider service-location activated"
                        : "Provider service-location archived");

        return toResponse(serviceLocation);
    }

    @Transactional
    public ProviderServiceLocationResponse verifyServiceLocation(
            UUID serviceLocationId,
            ProviderServiceLocationVerificationRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);
        ProviderServiceLocation serviceLocation =
                getServiceLocation(serviceLocationId);

        String sourceName = normalizeRequired(
                request.sourceName(),
                "Source name");
        String sourceUrl = normalizeOptional(request.sourceUrl());
        String sourceVersion = normalizeOptional(request.sourceVersion());
        String reviewNotes = normalizeOptional(request.reviewNotes());

        if (request.result() == null) {
            throw new BadRequestException("Verification result is required");
        }

        if (request.result() != VerificationResult.VERIFIED
                && reviewNotes == null) {

            throw new BadRequestException(
                    "Review notes are required when verification is rejected or revoked");
        }

        VerificationStatus resultingStatus =
                request.result() == VerificationResult.VERIFIED
                        ? VerificationStatus.VERIFIED
                        : VerificationStatus.UNVERIFIED;

        LocalDate resultingVerifiedDate =
                request.result() == VerificationResult.VERIFIED
                        ? LocalDate.now()
                        : null;

        serviceLocation.setSourceName(sourceName);
        serviceLocation.setSourceUrl(sourceUrl);
        serviceLocation.setVerificationStatus(resultingStatus);
        serviceLocation.setLastVerifiedAt(resultingVerifiedDate);

        serviceLocation =
                providerServiceLocationRepository.save(serviceLocation);

        VerificationRecord record = new VerificationRecord();
        record.setVerifierUser(actor);
        record.setProviderServiceLocation(serviceLocation);
        record.setVerificationResult(request.result());
        record.setSourceName(sourceName);
        record.setSourceUrl(sourceUrl);
        record.setSourceVersion(sourceVersion);
        record.setReviewNotes(reviewNotes);
        record.setPerformedAt(Instant.now());

        record = verificationRecordRepository.save(record);

        AuditActionType actionType = switch (request.result()) {
            case VERIFIED -> AuditActionType.VERIFY;
            case REJECTED -> AuditActionType.REJECT_VERIFICATION;
            case REVOKED -> AuditActionType.REVOKE_VERIFICATION;
        };

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_LOCATION,
                serviceLocation.getId(),
                actionType,
                new String[] {
                    "source_name",
                    "source_url",
                    "verification_status",
                    "last_verified_at",
                    "verification_result",
                    "source_version",
                    "review_notes"
                },
                "Provider service-location verification recorded: "
                        + request.result().name());

        return toResponse(serviceLocation);
    }

    @Transactional(readOnly = true)
    public List<VerificationRecordResponse> getVerificationHistory(
            UUID serviceLocationId) {

        if (!providerServiceLocationRepository.existsById(serviceLocationId)) {
            throw new ResourceNotFoundException(
                    "Provider service-location not found");
        }

        return verificationRecordRepository
                .findByProviderServiceLocation_IdOrderByPerformedAtDesc(
                        serviceLocationId)
                .stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    private ProviderServiceLocation getServiceLocation(
            UUID serviceLocationId) {

        return providerServiceLocationRepository.findById(serviceLocationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider service-location not found"));
    }

    private User requireActor(String actorEmail) {
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated admin account was not found"));
    }

    private void validateSameProvider(
            ProviderService providerService,
            ProviderLocation providerLocation) {

        UUID serviceProviderId =
                providerService.getProvider().getId();

        UUID locationProviderId =
                providerLocation.getProvider().getId();

        if (!Objects.equals(serviceProviderId, locationProviderId)) {
            throw new BadRequestException(
                    "Provider service and provider location must belong to the same provider");
        }
    }

    private String normalizeRequired(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    fieldName + " is required");
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ProviderServiceLocationResponse toResponse(
            ProviderServiceLocation serviceLocation) {

        ProviderService service =
                serviceLocation.getProviderService();

        ProviderLocation location =
                serviceLocation.getProviderLocation();

        return new ProviderServiceLocationResponse(
                serviceLocation.getId(),

                service.getProvider().getId(),
                service.getProvider().getName(),

                service.getId(),
                service.getCode(),
                service.getName(),

                location.getId(),
                location.getAddressLine1(),
                location.getAddressLine2(),
                location.getLocality(),
                location.getCity(),
                location.getStateCode(),
                location.getPostalCode(),
                location.getCountryCode(),

                location.getLatitude(),
                location.getLongitude(),

                location.getPublicPhone(),
                location.getPublicEmail(),
                location.getAccessibilityNotes(),

                serviceLocation.getSourceName(),
                serviceLocation.getSourceUrl(),

                serviceLocation.getVerificationStatus(),
                serviceLocation.getLastVerifiedAt(),

                serviceLocation.isActive(),

                serviceLocation.getCreatedAt(),
                serviceLocation.getUpdatedAt());
    }

    private VerificationRecordResponse toVerificationResponse(
            VerificationRecord record) {

        return new VerificationRecordResponse(
                record.getId(),
                record.getProviderServiceLocation().getId(),
                record.getVerificationResult(),
                record.getSourceName(),
                record.getSourceUrl(),
                record.getSourceVersion(),
                record.getReviewNotes(),
                record.getPerformedAt());
    }
}