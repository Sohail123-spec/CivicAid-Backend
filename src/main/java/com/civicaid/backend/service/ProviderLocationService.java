package com.civicaid.backend.service;

import com.civicaid.backend.dto.ProviderLocationCreateRequest;
import com.civicaid.backend.dto.ProviderLocationResponse;
import com.civicaid.backend.dto.ProviderLocationUpdateRequest;
import com.civicaid.backend.dto.ProviderLocationVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderLocation;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ProviderLocationRepository;
import com.civicaid.backend.repository.ProviderRepository;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.repository.VerificationRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderLocationService {

    private static final int DIRECTORY_FRESHNESS_DAYS = 90;

    private static final Set<String> VALID_INDIAN_STATE_UT_CODES = Set.of(
            "AN", "AP", "AR", "AS", "BR", "CH", "CG", "DH", "DL",
            "GA", "GJ", "HR", "HP", "JK", "JH", "KA", "KL", "LA",
            "LD", "MP", "MH", "MN", "ML", "MZ", "NL", "OD", "PY",
            "PB", "RJ", "SK", "TN", "TS", "TR", "UP", "UK", "WB"
    );

    private final ProviderLocationRepository providerLocationRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AuditLogService auditLogService;

    public ProviderLocationService(
            ProviderLocationRepository providerLocationRepository,
            ProviderRepository providerRepository,
            UserRepository userRepository,
            VerificationRecordRepository verificationRecordRepository,
            AuditLogService auditLogService) {

        this.providerLocationRepository = providerLocationRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ProviderLocationResponse createLocation(
            UUID providerId,
            ProviderLocationCreateRequest request,
            String actorEmail) {

        User actor = getActor(actorEmail);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Provider not found"));

        String addressLine1 = normalizeRequired(
                request.addressLine1(), "Address line 1");
        String locality = normalizeRequired(request.locality(), "Locality");
        String city = normalizeRequired(request.city(), "City");
        String stateCode = normalizeRequired(
                request.stateCode(), "State code");
        String sourceName = normalizeRequired(
                request.sourceName(), "Source name");

        String publicPhone = normalizeOptional(request.publicPhone());
        String publicEmail = normalizeOptional(request.publicEmail());

        validateStateCode(stateCode);
        validateContact(publicPhone, publicEmail);
        validateCoordinates(request.latitude(), request.longitude());

        ProviderLocation location = new ProviderLocation();

        location.setProvider(provider);
        location.setAddressLine1(addressLine1);
        location.setAddressLine2(
                normalizeOptional(request.addressLine2()));
        location.setLocality(locality);
        location.setCity(city);
        location.setStateCode(stateCode);
        location.setPostalCode(
                normalizeOptional(request.postalCode()));
        location.setCountryCode("IN");
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setPublicPhone(publicPhone);
        location.setPublicEmail(publicEmail);
        location.setAccessibilityNotes(
                normalizeOptional(request.accessibilityNotes()));
        location.setSourceName(sourceName);
        location.setSourceUrl(normalizeOptional(request.sourceUrl()));
        location.setVerificationStatus(VerificationStatus.UNVERIFIED);
        location.setLastVerifiedAt(null);
        location.setActive(true);

        location = providerLocationRepository.save(location);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_LOCATION,
                location.getId(),
                AuditActionType.CREATE,
                new String[] {
                        "provider_id",
                        "address_line_1",
                        "address_line_2",
                        "locality",
                        "city",
                        "state_code",
                        "postal_code",
                        "country_code",
                        "latitude",
                        "longitude",
                        "public_phone",
                        "public_email",
                        "accessibility_notes",
                        "source_name",
                        "source_url",
                        "verification_status",
                        "last_verified_at",
                        "active"
                },
                "Provider location created");

        return toResponse(location);
    }

    @Transactional(readOnly = true)
    public List<ProviderLocationResponse> getAllLocationsForAdmin() {
        return providerLocationRepository.findAllByOrderByCityAscLocalityAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderLocationResponse> getLocationsForProvider(
            UUID providerId) {

        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider not found");
        }

        return providerLocationRepository
                .findByProvider_IdOrderByCityAscLocalityAsc(providerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderLocationResponse> getPublicLocationsForProvider(
            UUID providerId) {

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        LocalDate today = LocalDate.now();
        LocalDate freshnessCutoff =
                today.minusDays(DIRECTORY_FRESHNESS_DAYS);

        if (!isPubliclyDiscoverableProvider(
                provider,
                freshnessCutoff,
                today)) {

            throw new ResourceNotFoundException(
                    "Provider not found");
        }

        return providerLocationRepository
                .findPubliclyDiscoverableLocationsByProvider(
                        providerId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff,
                        today)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderLocationResponse getLocationForAdmin(UUID locationId) {
        ProviderLocation location = getLocation(locationId);
        return toResponse(location);
    }

    @Transactional
    public ProviderLocationResponse updateLocation(
            UUID locationId,
            ProviderLocationUpdateRequest request,
            String actorEmail) {

        User actor = getActor(actorEmail);
        ProviderLocation location = getLocation(locationId);

        String addressLine1 = normalizeRequired(
                request.addressLine1(), "Address line 1");
        String addressLine2 = normalizeOptional(request.addressLine2());
        String locality = normalizeRequired(request.locality(), "Locality");
        String city = normalizeRequired(request.city(), "City");
        String stateCode = normalizeRequired(
                request.stateCode(), "State code");
        String postalCode = normalizeOptional(request.postalCode());
        BigDecimal latitude = request.latitude();
        BigDecimal longitude = request.longitude();
        String publicPhone = normalizeOptional(request.publicPhone());
        String publicEmail = normalizeOptional(request.publicEmail());
        String accessibilityNotes =
                normalizeOptional(request.accessibilityNotes());
        String sourceName = normalizeRequired(
                request.sourceName(), "Source name");
        String sourceUrl = normalizeOptional(request.sourceUrl());

        validateStateCode(stateCode);
        validateContact(publicPhone, publicEmail);
        validateCoordinates(latitude, longitude);

        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(location.getAddressLine1(), addressLine1)) {
            location.setAddressLine1(addressLine1);
            changedFields.add("address_line_1");
        }

        if (!Objects.equals(location.getAddressLine2(), addressLine2)) {
            location.setAddressLine2(addressLine2);
            changedFields.add("address_line_2");
        }

        if (!Objects.equals(location.getLocality(), locality)) {
            location.setLocality(locality);
            changedFields.add("locality");
        }

        if (!Objects.equals(location.getCity(), city)) {
            location.setCity(city);
            changedFields.add("city");
        }

        if (!Objects.equals(location.getStateCode(), stateCode)) {
            location.setStateCode(stateCode);
            changedFields.add("state_code");
        }

        if (!Objects.equals(location.getPostalCode(), postalCode)) {
            location.setPostalCode(postalCode);
            changedFields.add("postal_code");
        }

        if (!sameCoordinate(location.getLatitude(), latitude)) {
            location.setLatitude(latitude);
            changedFields.add("latitude");
        }

        if (!sameCoordinate(location.getLongitude(), longitude)) {
            location.setLongitude(longitude);
            changedFields.add("longitude");
        }

        if (!Objects.equals(location.getPublicPhone(), publicPhone)) {
            location.setPublicPhone(publicPhone);
            changedFields.add("public_phone");
        }

        if (!Objects.equals(location.getPublicEmail(), publicEmail)) {
            location.setPublicEmail(publicEmail);
            changedFields.add("public_email");
        }

        if (!Objects.equals(
                location.getAccessibilityNotes(), accessibilityNotes)) {

            location.setAccessibilityNotes(accessibilityNotes);
            changedFields.add("accessibility_notes");
        }

        if (!Objects.equals(location.getSourceName(), sourceName)) {
            location.setSourceName(sourceName);
            changedFields.add("source_name");
        }

        if (!Objects.equals(location.getSourceUrl(), sourceUrl)) {
            location.setSourceUrl(sourceUrl);
            changedFields.add("source_url");
        }

        if (changedFields.isEmpty()) {
            return toResponse(location);
        }

        // A substantive location change invalidates its previous verification.
        location.setVerificationStatus(VerificationStatus.UNVERIFIED);
        location.setLastVerifiedAt(null);

        changedFields.add("verification_status");
        changedFields.add("last_verified_at");

        location = providerLocationRepository.save(location);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_LOCATION,
                location.getId(),
                AuditActionType.UPDATE,
                changedFields.toArray(String[]::new),
                "Provider location updated; verification reset");

        return toResponse(location);
    }

    @Transactional
    public VerificationRecordResponse verifyLocation(
            UUID locationId,
            ProviderLocationVerificationRequest request,
            String actorEmail) {

        User actor = getActor(actorEmail);
        ProviderLocation location = getLocation(locationId);

        String sourceName = normalizeRequired(
                request.sourceName(), "Source name");
        String sourceUrl = normalizeOptional(request.sourceUrl());
        String sourceVersion = normalizeOptional(request.sourceVersion());
        String reviewNotes = normalizeOptional(request.reviewNotes());

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

        List<String> changedFields = new ArrayList<>();

        addIfChanged(
                changedFields,
                "source_name",
                location.getSourceName(),
                sourceName);

        addIfChanged(
                changedFields,
                "source_url",
                location.getSourceUrl(),
                sourceUrl);

        addIfChanged(
                changedFields,
                "verification_status",
                location.getVerificationStatus(),
                resultingStatus);

        addIfChanged(
                changedFields,
                "last_verified_at",
                location.getLastVerifiedAt(),
                resultingVerifiedDate);

        changedFields.add("verification_result");

        if (sourceVersion != null) {
            changedFields.add("source_version");
        }

        if (reviewNotes != null) {
            changedFields.add("review_notes");
        }

        location.setSourceName(sourceName);
        location.setSourceUrl(sourceUrl);
        location.setVerificationStatus(resultingStatus);
        location.setLastVerifiedAt(resultingVerifiedDate);

        location = providerLocationRepository.save(location);

        VerificationRecord record = new VerificationRecord();

        record.setVerifierUser(actor);
        record.setProviderLocation(location);
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
                AuditTargetType.PROVIDER_LOCATION,
                location.getId(),
                actionType,
                changedFields.toArray(String[]::new),
                "Provider location verification recorded: "
                        + request.result().name());

        return toVerificationResponse(record);
    }

    @Transactional(readOnly = true)
    public List<VerificationRecordResponse> getVerificationHistory(
            UUID locationId) {

        if (!providerLocationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException(
                    "Provider location not found");
        }

        return verificationRecordRepository
                .findByProviderLocation_IdOrderByPerformedAtDesc(locationId)
                .stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    private ProviderLocation getLocation(UUID locationId) {
        return providerLocationRepository.findById(locationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider location not found"));
    }

    private User getActor(String actorEmail) {
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated admin account was not found"));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
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

    private void validateStateCode(String stateCode) {
        if (!VALID_INDIAN_STATE_UT_CODES.contains(stateCode)) {
            throw new BadRequestException(
                    "State code must be a valid Indian state or union territory code");
        }
    }

    private void validateContact(String publicPhone, String publicEmail) {
        if (publicPhone == null && publicEmail == null) {
            throw new BadRequestException(
                    "At least one public contact method (phone or email) is required");
        }
    }

    private void validateCoordinates(
            BigDecimal latitude,
            BigDecimal longitude) {

        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException(
                    "Latitude and longitude must either both be provided or both be omitted");
        }
    }

    private boolean sameCoordinate(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return first == second;
        }

        return first.compareTo(second) == 0;
    }

    private boolean isPubliclyDiscoverableProvider(
            Provider provider,
            LocalDate freshnessCutoff,
            LocalDate today) {

        return provider.isActive()
                && provider.getVerificationStatus()
                        == VerificationStatus.VERIFIED
                && provider.getLastVerifiedAt() != null
                && !provider.getLastVerifiedAt()
                        .isBefore(freshnessCutoff)
                && !provider.getLastVerifiedAt()
                        .isAfter(today);
    }

    private void addIfChanged(
            List<String> fields,
            String field,
            Object oldValue,
            Object newValue) {

        if (!Objects.equals(oldValue, newValue)) {
            fields.add(field);
        }
    }

    private ProviderLocationResponse toResponse(
            ProviderLocation location) {

        return new ProviderLocationResponse(
                location.getId(),
                location.getProvider().getId(),
                location.getProvider().getName(),
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
                location.getSourceName(),
                location.getSourceUrl(),
                location.getVerificationStatus(),
                location.getLastVerifiedAt(),
                location.isActive(),
                location.getCreatedAt(),
                location.getUpdatedAt());
    }

    private VerificationRecordResponse toVerificationResponse(
            VerificationRecord record) {

        return new VerificationRecordResponse(
                record.getId(),
                record.getProviderLocation().getId(),
                record.getVerificationResult(),
                record.getSourceName(),
                record.getSourceUrl(),
                record.getSourceVersion(),
                record.getReviewNotes(),
                record.getPerformedAt());
    }
}