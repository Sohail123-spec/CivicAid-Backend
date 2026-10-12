package com.civicaid.backend.service;

import com.civicaid.backend.dto.ProviderServiceCategoryCreateRequest;
import com.civicaid.backend.dto.ProviderServiceCategoryResponse;
import com.civicaid.backend.dto.ProviderServiceCategoryUpdateRequest;
import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceCategory;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATCategoryRepository;
import com.civicaid.backend.repository.ProviderServiceCategoryRepository;
import com.civicaid.backend.repository.ProviderServiceRepository;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderServiceCategoryService {

    private static final int PROVIDER_SERVICE_VERIFICATION_FRESHNESS_DAYS = 90;

    private static final int AT_CATEGORY_VERIFICATION_FRESHNESS_DAYS = 180;

    private final ProviderServiceCategoryRepository
            providerServiceCategoryRepository;

    private final ProviderServiceRepository providerServiceRepository;

    private final ATCategoryRepository atCategoryRepository;

    private final UserRepository userRepository;

    private final AuditLogService auditLogService;

    public ProviderServiceCategoryService(
            ProviderServiceCategoryRepository providerServiceCategoryRepository,
            ProviderServiceRepository providerServiceRepository,
            ATCategoryRepository atCategoryRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.providerServiceCategoryRepository =
                providerServiceCategoryRepository;

        this.providerServiceRepository =
                providerServiceRepository;

        this.atCategoryRepository =
                atCategoryRepository;

        this.userRepository = userRepository;

        this.auditLogService = auditLogService;
    }

    /*
     * -------------------------------------------------------------------------
     * PUBLIC DISCOVERY
     * -------------------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<ProviderServiceCategoryResponse> getPublicMappings() {

        return providerServiceCategoryRepository
                .findPubliclyDiscoverableCategoryMappings(
                        VerificationStatus.VERIFIED,
                        providerServiceFreshnessCutoff(),
                        categoryFreshnessCutoff(),
                        LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceCategoryResponse> getPublicMappingsByCategory(
            UUID categoryId) {

        return providerServiceCategoryRepository
                .findPubliclyDiscoverableCategoryMappingsByCategory(
                        categoryId,
                        VerificationStatus.VERIFIED,
                        providerServiceFreshnessCutoff(),
                        categoryFreshnessCutoff(),
                        LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN READ OPERATIONS
     * -------------------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<ProviderServiceCategoryResponse> getAdminMappings() {

        return providerServiceCategoryRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceCategoryResponse> getAdminMappingsByService(
            UUID providerServiceId) {

        if (!providerServiceRepository.existsById(providerServiceId)) {
            throw new ResourceNotFoundException(
                    "Provider service not found");
        }

        return providerServiceCategoryRepository
                .findByProviderService_IdOrderByAtCategory_NameAsc(
                        providerServiceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceCategoryResponse> getAdminMappingsByCategory(
            UUID categoryId) {

        if (!atCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(
                    "AT category not found");
        }

        return providerServiceCategoryRepository
                .findByAtCategory_IdOrderByProviderService_NameAsc(
                        categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderServiceCategoryResponse getAdminMapping(
            UUID mappingId) {

        return toResponse(requireMapping(mappingId));
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN CREATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceCategoryResponse createMapping(
            UUID providerServiceId,
            ProviderServiceCategoryCreateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderService providerService =
                providerServiceRepository.findById(providerServiceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider service not found"));

        ATCategory atCategory =
                atCategoryRepository.findById(request.atCategoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "AT category not found"));

        if (providerServiceCategoryRepository
                .existsByProviderService_IdAndAtCategory_Id(
                        providerServiceId,
                        request.atCategoryId())) {

            throw new ConflictException(
                    "Provider service category mapping already exists");
        }

        ProviderServiceCategory mapping =
                new ProviderServiceCategory();

        mapping.setProviderService(providerService);
        mapping.setAtCategory(atCategory);
        mapping.setRationale(request.rationale().trim());
        mapping.setActive(true);

        mapping =
                providerServiceCategoryRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_CATEGORY,
                mapping.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "provider_service_id",
                    "at_category_id",
                    "rationale",
                    "active"
                },
                "Provider service category mapping created");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN UPDATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceCategoryResponse updateMapping(
            UUID mappingId,
            ProviderServiceCategoryUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderServiceCategory mapping =
                requireMapping(mappingId);

        String rationale = request.rationale().trim();

        if (Objects.equals(mapping.getRationale(), rationale)) {
            return toResponse(mapping);
        }

        mapping.setRationale(rationale);

        mapping =
                providerServiceCategoryRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_CATEGORY,
                mapping.getId(),
                AuditActionType.UPDATE,
                new String[] {"rationale"},
                "Provider service category mapping updated");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN ACTIVATE / ARCHIVE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceCategoryResponse setMappingActive(
            UUID mappingId,
            boolean active,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderServiceCategory mapping =
                requireMapping(mappingId);

        /*
         * Idempotent activation/archive.
         *
         * No duplicate audit event is generated for a no-op.
         */
        if (mapping.isActive() == active) {
            return toResponse(mapping);
        }

        mapping.setActive(active);

        mapping =
                providerServiceCategoryRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_CATEGORY,
                mapping.getId(),
                active
                        ? AuditActionType.ACTIVATE
                        : AuditActionType.ARCHIVE,
                new String[] {"active"},
                active
                        ? "Provider service category mapping activated"
                        : "Provider service category mapping archived");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * HELPERS
     * -------------------------------------------------------------------------
     */

    private ProviderServiceCategory requireMapping(UUID mappingId) {

        return providerServiceCategoryRepository
                .findById(mappingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider service category mapping not found"));
    }

    private User requireActor(String actorEmail) {

        return userRepository
                .findByEmail(actorEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    private LocalDate providerServiceFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        PROVIDER_SERVICE_VERIFICATION_FRESHNESS_DAYS);
    }

    private LocalDate categoryFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        AT_CATEGORY_VERIFICATION_FRESHNESS_DAYS);
    }

    private ProviderServiceCategoryResponse toResponse(
            ProviderServiceCategory mapping) {

        ProviderService providerService =
                mapping.getProviderService();

        ATCategory atCategory =
                mapping.getAtCategory();

        return new ProviderServiceCategoryResponse(
                mapping.getId(),
                providerService.getProvider().getId(),
                providerService.getProvider().getName(),
                providerService.getId(),
                providerService.getCode(),
                providerService.getName(),
                atCategory.getId(),
                atCategory.getName(),
                mapping.getRationale(),
                mapping.isActive(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt());
    }
}