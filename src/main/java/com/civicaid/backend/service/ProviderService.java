package com.civicaid.backend.service;

import com.civicaid.backend.dto.ProviderCreateRequest;
import com.civicaid.backend.dto.ProviderResponse;
import com.civicaid.backend.dto.ProviderUpdateRequest;
import com.civicaid.backend.dto.ProviderVerificationRecordResponse;
import com.civicaid.backend.dto.ProviderVerificationRequest;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderType;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ProviderRepository;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.repository.VerificationRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {

    private static final int PROVIDER_FRESHNESS_DAYS = 90;

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AuditLogService auditLogService;

    public ProviderService(
            ProviderRepository providerRepository,
            UserRepository userRepository,
            VerificationRecordRepository verificationRecordRepository,
            AuditLogService auditLogService) {

        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getPublicProviders() {

        LocalDate today = LocalDate.now();

        LocalDate freshnessCutoff =
                today.minusDays(PROVIDER_FRESHNESS_DAYS);

        return providerRepository
                .findPubliclyDiscoverableProviders(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff,
                        today)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getPublicProvidersByType(
            ProviderType providerType) {

        LocalDate today = LocalDate.now();

        LocalDate freshnessCutoff =
                today.minusDays(PROVIDER_FRESHNESS_DAYS);

        return providerRepository
                .findPubliclyDiscoverableProvidersByType(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff,
                        today,
                        providerType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse getPublicProvider(UUID providerId) {

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        LocalDate today = LocalDate.now();

        LocalDate freshnessCutoff =
                today.minusDays(PROVIDER_FRESHNESS_DAYS);

        boolean publiclyDiscoverable =
                provider.isActive()
                        && provider.getVerificationStatus()
                                == VerificationStatus.VERIFIED
                        && provider.getLastVerifiedAt() != null
                        && !provider.getLastVerifiedAt()
                                .isBefore(freshnessCutoff)
                        && !provider.getLastVerifiedAt()
                                .isAfter(today);

        if (!publiclyDiscoverable) {
            throw new ResourceNotFoundException(
                    "Provider not found");
        }

        return toResponse(provider);
    }

    @Transactional
    public ProviderResponse createProvider(
            ProviderCreateRequest request,
            String actorEmail) {

        User actor = getActor(actorEmail);

        String code = request.code().trim();
        String name = request.name().trim();

        if (providerRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(
                    "Provider code already exists");
        }

        if (providerRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(
                    "Provider name already exists");
        }

        Provider provider = new Provider();

        provider.setCode(code);
        provider.setName(name);
        provider.setProviderType(request.providerType());
        provider.setDescription(
                normalizeOptional(request.description()));
        provider.setWebsiteUrl(
                normalizeOptional(request.websiteUrl()));
        provider.setSourceName(request.sourceName().trim());
        provider.setSourceUrl(
                normalizeOptional(request.sourceUrl()));
        provider.setVerificationStatus(
                VerificationStatus.UNVERIFIED);
        provider.setLastVerifiedAt(null);
        provider.setActive(true);

        provider = providerRepository.save(provider);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER,
                provider.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "code",
                    "name",
                    "provider_type",
                    "description",
                    "website_url",
                    "source_name",
                    "source_url",
                    "verification_status",
                    "active"
                },
                "Provider created");

        return toResponse(provider);
    }

    @Transactional
    public ProviderResponse updateProvider(
            UUID providerId,
            ProviderUpdateRequest request,
            String actorEmail) {

        User actor = getActor(actorEmail);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        String name = request.name().trim();

        String description =
                normalizeOptional(request.description());

        String websiteUrl =
                normalizeOptional(request.websiteUrl());

        String sourceName =
                request.sourceName().trim();

        String sourceUrl =
                normalizeOptional(request.sourceUrl());

        if (providerRepository.existsByNameIgnoreCaseAndIdNot(
                name,
                providerId)) {

            throw new ConflictException(
                    "Provider name already exists");
        }

        List<String> changedFields = new ArrayList<>();

        if (!provider.getName().equals(name)) {
            provider.setName(name);
            changedFields.add("name");
        }

        if (provider.getProviderType()
                != request.providerType()) {

            provider.setProviderType(
                    request.providerType());

            changedFields.add("provider_type");
        }

        if (!Objects.equals(
                provider.getDescription(),
                description)) {

            provider.setDescription(description);
            changedFields.add("description");
        }

        if (!Objects.equals(
                provider.getWebsiteUrl(),
                websiteUrl)) {

            provider.setWebsiteUrl(websiteUrl);
            changedFields.add("website_url");
        }

        if (!provider.getSourceName().equals(sourceName)) {
            provider.setSourceName(sourceName);
            changedFields.add("source_name");
        }

        if (!Objects.equals(
                provider.getSourceUrl(),
                sourceUrl)) {

            provider.setSourceUrl(sourceUrl);
            changedFields.add("source_url");
        }

        if (changedFields.isEmpty()) {
            return toResponse(provider);
        }

        provider.setVerificationStatus(
                VerificationStatus.UNVERIFIED);

        provider.setLastVerifiedAt(null);

        if (!changedFields.contains(
                "verification_status")) {

            changedFields.add("verification_status");
        }

        if (!changedFields.contains(
                "last_verified_at")) {

            changedFields.add("last_verified_at");
        }

        provider = providerRepository.save(provider);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER,
                provider.getId(),
                AuditActionType.UPDATE,
                changedFields.toArray(String[]::new),
                "Provider updated; verification reset");

        return toResponse(provider);
    }

    @Transactional
    public ProviderVerificationRecordResponse verifyProvider(
            UUID providerId,
            ProviderVerificationRequest request,
            String actorEmail) {

        User actor = getActor(actorEmail);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        String sourceName = request.sourceName().trim();
        String sourceUrl =
                normalizeOptional(request.sourceUrl());
        String sourceVersion =
                normalizeOptional(request.sourceVersion());
        String reviewNotes =
                normalizeOptional(request.reviewNotes());

        if (request.result() != VerificationResult.VERIFIED
                && reviewNotes == null) {

            throw new BadRequestException(
                    "Review notes are required when verification is rejected or revoked");
        }

        List<String> changedFields = new ArrayList<>();

        addIfChanged(
                changedFields,
                "source_name",
                provider.getSourceName(),
                sourceName);

        addIfChanged(
                changedFields,
                "source_url",
                provider.getSourceUrl(),
                sourceUrl);

        VerificationStatus resultingStatus =
                request.result() == VerificationResult.VERIFIED
                        ? VerificationStatus.VERIFIED
                        : VerificationStatus.UNVERIFIED;

        LocalDate resultingVerifiedDate =
                request.result() == VerificationResult.VERIFIED
                        ? LocalDate.now()
                        : null;

        addIfChanged(
                changedFields,
                "verification_status",
                provider.getVerificationStatus(),
                resultingStatus);

        addIfChanged(
                changedFields,
                "last_verified_at",
                provider.getLastVerifiedAt(),
                resultingVerifiedDate);

        changedFields.add("verification_result");

        if (sourceVersion != null) {
            changedFields.add("source_version");
        }

        if (reviewNotes != null) {
            changedFields.add("review_notes");
        }

        provider.setSourceName(sourceName);
        provider.setSourceUrl(sourceUrl);
        provider.setVerificationStatus(resultingStatus);
        provider.setLastVerifiedAt(resultingVerifiedDate);

        provider = providerRepository.save(provider);

        VerificationRecord record = new VerificationRecord();

        record.setVerifierUser(actor);
        record.setProvider(provider);
        record.setVerificationResult(request.result());
        record.setSourceName(sourceName);
        record.setSourceUrl(sourceUrl);
        record.setSourceVersion(sourceVersion);
        record.setReviewNotes(reviewNotes);
        record.setPerformedAt(Instant.now());

        record = verificationRecordRepository.save(record);

        AuditActionType actionType =
                switch (request.result()) {
                    case VERIFIED -> AuditActionType.VERIFY;
                    case REJECTED ->
                            AuditActionType.REJECT_VERIFICATION;
                    case REVOKED ->
                            AuditActionType.REVOKE_VERIFICATION;
                };

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER,
                provider.getId(),
                actionType,
                changedFields.toArray(String[]::new),
                "Provider verification recorded: "
                        + request.result().name());

        return toVerificationResponse(record);
    }

    @Transactional(readOnly = true)
    public List<ProviderVerificationRecordResponse>
            getVerificationHistory(UUID providerId) {

        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException(
                    "Provider not found");
        }

        return verificationRecordRepository
                .findByProvider_IdOrderByPerformedAtDesc(providerId)
                .stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getAllProvidersForAdmin() {

        return providerRepository
                .findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse getProviderForAdmin(
            UUID providerId) {

        Provider provider = providerRepository
                .findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        return toResponse(provider);
    }

    private User getActor(String actorEmail) {

        return userRepository
                .findByEmail(actorEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated admin account was not found"));
    }

    private String normalizeOptional(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void addIfChanged(
            List<String> changedFields,
            String fieldName,
            Object oldValue,
            Object newValue) {

        if (!Objects.equals(oldValue, newValue)) {
            changedFields.add(fieldName);
        }
    }

    private ProviderVerificationRecordResponse
            toVerificationResponse(VerificationRecord record) {

        return new ProviderVerificationRecordResponse(
                record.getId(),
                record.getProvider().getId(),
                record.getVerificationResult(),
                record.getSourceName(),
                record.getSourceUrl(),
                record.getSourceVersion(),
                record.getReviewNotes(),
                record.getPerformedAt());
    }

    private ProviderResponse toResponse(
            Provider provider) {

        return new ProviderResponse(
                provider.getId(),
                provider.getCode(),
                provider.getName(),
                provider.getProviderType(),
                provider.getDescription(),
                provider.getWebsiteUrl(),
                provider.getSourceName(),
                provider.getSourceUrl(),
                provider.getVerificationStatus(),
                provider.getLastVerifiedAt(),
                provider.isActive(),
                provider.getCreatedAt(),
                provider.getUpdatedAt());
    }
}