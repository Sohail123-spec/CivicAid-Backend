package com.civicaid.backend.service;

import com.civicaid.backend.dto.ProviderServiceCreateRequest;
import com.civicaid.backend.dto.ProviderServiceResponse;
import com.civicaid.backend.dto.ProviderServiceUpdateRequest;
import com.civicaid.backend.dto.ProviderServiceVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ProviderRepository;
import com.civicaid.backend.repository.ProviderServiceRepository;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.repository.VerificationRecordRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderServiceService {

    private static final int VERIFICATION_FRESHNESS_DAYS = 90;

    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AuditLogService auditLogService;

    public ProviderServiceService(
            ProviderServiceRepository providerServiceRepository,
            ProviderRepository providerRepository,
            UserRepository userRepository,
            VerificationRecordRepository verificationRecordRepository,
            AuditLogService auditLogService) {

        this.providerServiceRepository = providerServiceRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.auditLogService = auditLogService;
    }

    /*
     * -------------------------------------------------------------------------
     * PUBLIC DISCOVERY
     * -------------------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getPublicServices() {

        return providerServiceRepository
                .findPubliclyDiscoverableServices(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(),
                        LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getPublicServicesByType(
            com.civicaid.backend.entity.ProviderServiceType serviceType) {

        return providerServiceRepository
                .findPubliclyDiscoverableServicesByType(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(),
                        LocalDate.now(),
                        serviceType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getPublicServicesByProvider(
            UUID providerId) {

        return providerServiceRepository
                .findPubliclyDiscoverableServicesByProvider(
                        providerId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(),
                        LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderServiceResponse getPublicService(UUID serviceId) {

        ProviderService service = providerServiceRepository.findById(serviceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider service not found"));

        if (!isPubliclyDiscoverable(service)) {
            throw new ResourceNotFoundException(
                    "Provider service not found");
        }

        return toResponse(service);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN READ OPERATIONS
     * -------------------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getAdminServices() {

        return providerServiceRepository
                .findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getAdminServicesByProvider(
            UUID providerId) {

        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException(
                    "Provider not found");
        }

        return providerServiceRepository
                .findByProvider_IdOrderByNameAsc(providerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderServiceResponse getAdminService(UUID serviceId) {

        return toResponse(requireService(serviceId));
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN CREATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceResponse createService(
            UUID providerId,
            ProviderServiceCreateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        String code = request.code().trim();
        String name = request.name().trim();

        if (providerServiceRepository
                .existsByProvider_IdAndCodeIgnoreCase(providerId, code)) {

            throw new ConflictException(
                    "Provider service code already exists for this provider");
        }

        if (providerServiceRepository
                .existsByProvider_IdAndNameIgnoreCase(providerId, name)) {

            throw new ConflictException(
                    "Provider service name already exists for this provider");
        }

        ProviderService service = new ProviderService();

        service.setProvider(provider);
        service.setCode(code);
        service.setName(name);
        service.setServiceType(request.serviceType());
        service.setDescription(request.description().trim());
        service.setSourceName(request.sourceName().trim());
        service.setSourceUrl(normalizeOptional(request.sourceUrl()));

        /*
         * Every newly-created service requires verification.
         */
        service.setVerificationStatus(
                VerificationStatus.UNVERIFIED);
        service.setLastVerifiedAt(null);
        service.setActive(true);

        service = providerServiceRepository.save(service);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE,
                service.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "provider_id",
                    "code",
                    "name",
                    "service_type",
                    "description",
                    "source_name",
                    "source_url",
                    "verification_status",
                    "active"
                },
                "Provider service created");

        return toResponse(service);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN UPDATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceResponse updateService(
            UUID serviceId,
            ProviderServiceUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderService service = requireService(serviceId);

        String name = request.name().trim();
        String description = request.description().trim();
        String sourceName = request.sourceName().trim();
        String sourceUrl = normalizeOptional(request.sourceUrl());

        if (providerServiceRepository
                .existsByProvider_IdAndNameIgnoreCase(
                        service.getProvider().getId(),
                        name)
                && !service.getName().equalsIgnoreCase(name)) {

            throw new ConflictException(
                    "Provider service name already exists for this provider");
        }

        List<String> changedFields = new ArrayList<>();

        addIfChanged(
                changedFields,
                "name",
                service.getName(),
                name);

        addIfChanged(
                changedFields,
                "service_type",
                service.getServiceType(),
                request.serviceType());

        addIfChanged(
                changedFields,
                "description",
                service.getDescription(),
                description);

        addIfChanged(
                changedFields,
                "source_name",
                service.getSourceName(),
                sourceName);

        addIfChanged(
                changedFields,
                "source_url",
                service.getSourceUrl(),
                sourceUrl);

        /*
         * A substantive service change invalidates the previous verification.
         */
        if (!changedFields.isEmpty()) {

            addIfChanged(
                    changedFields,
                    "verification_status",
                    service.getVerificationStatus(),
                    VerificationStatus.UNVERIFIED);

            addIfChanged(
                    changedFields,
                    "last_verified_at",
                    service.getLastVerifiedAt(),
                    null);

            service.setVerificationStatus(
                    VerificationStatus.UNVERIFIED);

            service.setLastVerifiedAt(null);
        }

        service.setName(name);
        service.setServiceType(request.serviceType());
        service.setDescription(description);
        service.setSourceName(sourceName);
        service.setSourceUrl(sourceUrl);

        /*
         * Do not generate an audit record for a true no-op update.
         */
        if (changedFields.isEmpty()) {
            return toResponse(service);
        }

        service = providerServiceRepository.save(service);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE,
                service.getId(),
                AuditActionType.UPDATE,
                changedFields.toArray(String[]::new),
                "Provider service updated");

        return toResponse(service);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN ACTIVATE / ARCHIVE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceResponse setServiceActive(
            UUID serviceId,
            boolean active,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderService service = requireService(serviceId);

        /*
         * Idempotent activation/archive.
         * No duplicate audit event for a no-op.
         */
        if (service.isActive() == active) {
            return toResponse(service);
        }

        service.setActive(active);

        service = providerServiceRepository.save(service);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE,
                service.getId(),
                active
                        ? AuditActionType.ACTIVATE
                        : AuditActionType.ARCHIVE,
                new String[] {"active"},
                active
                        ? "Provider service activated"
                        : "Provider service archived");

        return toResponse(service);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN VERIFICATION
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceResponse verifyService(
            UUID serviceId,
            ProviderServiceVerificationRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderService service = requireService(serviceId);

        String sourceName = request.sourceName().trim();
        String sourceUrl = normalizeOptional(request.sourceUrl());
        String sourceVersion = normalizeOptional(request.sourceVersion());
        String reviewNotes = normalizeOptional(request.reviewNotes());

        /*
         * A rejected or revoked verification must contain an explanation.
         */
        if (request.result() != VerificationResult.VERIFIED
                && reviewNotes == null) {

            throw new BadRequestException(
                    "Review notes are required when verification is not approved");
        }

        VerificationStatus newStatus;

        if (request.result() == VerificationResult.VERIFIED) {
            newStatus = VerificationStatus.VERIFIED;
        } else {
            newStatus = VerificationStatus.UNVERIFIED;
        }

        service.setSourceName(sourceName);
        service.setSourceUrl(sourceUrl);
        service.setVerificationStatus(newStatus);

        if (request.result() == VerificationResult.VERIFIED) {
            service.setLastVerifiedAt(LocalDate.now());
        } else {
            service.setLastVerifiedAt(null);
        }

        service = providerServiceRepository.save(service);

        /*
         * Preserve immutable verification provenance.
         */
        VerificationRecord verificationRecord =
                new VerificationRecord();

        verificationRecord.setVerifierUser(actor);
        verificationRecord.setProviderService(service);
        verificationRecord.setVerificationResult(request.result());
        verificationRecord.setSourceName(sourceName);
        verificationRecord.setSourceUrl(sourceUrl);
        verificationRecord.setSourceVersion(sourceVersion);
        verificationRecord.setReviewNotes(reviewNotes);

        verificationRecordRepository.save(verificationRecord);

        AuditActionType auditAction;

        switch (request.result()) {
            case VERIFIED ->
                    auditAction = AuditActionType.VERIFY;

            case REJECTED ->
                    auditAction = AuditActionType.REJECT_VERIFICATION;

            case REVOKED ->
                    auditAction = AuditActionType.REVOKE_VERIFICATION;

            default ->
                    throw new BadRequestException(
                            "Unsupported verification result");
        }

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE,
                service.getId(),
                auditAction,
                new String[] {
                    "verification_status",
                    "last_verified_at",
                    "source_name",
                    "source_url",
                    "source_version",
                    "review_notes"
                },
                "Provider service verification updated");

        return toResponse(service);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN VERIFICATION HISTORY
     * -------------------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<VerificationRecordResponse> getVerificationHistory(
            UUID serviceId) {

        requireService(serviceId);

        return verificationRecordRepository
                .findByProviderService_IdOrderByPerformedAtDesc(serviceId)
                .stream()
                .map(this::toVerificationRecordResponse)
                .toList();
    }

    /*
     * -------------------------------------------------------------------------
     * HELPERS
     * -------------------------------------------------------------------------
     */

    private ProviderService requireService(UUID serviceId) {

        return providerServiceRepository.findById(serviceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider service not found"));
    }

    private User requireActor(String actorEmail) {

        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    private boolean isPubliclyDiscoverable(
            ProviderService service) {

        if (!service.isActive()) {
            return false;
        }

        if (service.getVerificationStatus()
                != VerificationStatus.VERIFIED) {
            return false;
        }

        if (service.getLastVerifiedAt() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        if (service.getLastVerifiedAt().isAfter(today)) {
            return false;
        }

        if (service.getLastVerifiedAt()
                .isBefore(freshnessCutoff())) {
            return false;
        }

        Provider provider = service.getProvider();

        if (provider == null || !provider.isActive()) {
            return false;
        }

        if (provider.getVerificationStatus()
                != VerificationStatus.VERIFIED) {
            return false;
        }

        if (provider.getLastVerifiedAt() == null) {
            return false;
        }

        if (provider.getLastVerifiedAt().isAfter(today)) {
            return false;
        }

        return !provider.getLastVerifiedAt()
                .isBefore(freshnessCutoff());
    }

    private LocalDate freshnessCutoff() {

        return LocalDate.now()
                .minusDays(VERIFICATION_FRESHNESS_DAYS);
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

    private ProviderServiceResponse toResponse(
            ProviderService service) {

        return new ProviderServiceResponse(
                service.getId(),
                service.getProvider().getId(),
                service.getProvider().getName(),
                service.getCode(),
                service.getName(),
                service.getServiceType(),
                service.getDescription(),
                service.getSourceName(),
                service.getSourceUrl(),
                service.getVerificationStatus(),
                service.getLastVerifiedAt(),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }

    private VerificationRecordResponse toVerificationRecordResponse(
            VerificationRecord record) {

        return new VerificationRecordResponse(
                record.getId(),
                record.getProviderService().getId(),
                record.getVerificationResult(),
                record.getSourceName(),
                record.getSourceUrl(),
                record.getSourceVersion(),
                record.getReviewNotes(),
                record.getPerformedAt());
    }
}