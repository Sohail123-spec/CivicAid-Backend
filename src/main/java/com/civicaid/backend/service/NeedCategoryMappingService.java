package com.civicaid.backend.service;

import com.civicaid.backend.dto.NeedCategoryMappingCreateRequest;
import com.civicaid.backend.dto.NeedCategoryMappingResponse;
import com.civicaid.backend.dto.NeedCategoryMappingUpdateRequest;
import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.FunctionalNeed;
import com.civicaid.backend.entity.NeedCategoryMapping;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATCategoryRepository;
import com.civicaid.backend.repository.FunctionalNeedRepository;
import com.civicaid.backend.repository.NeedCategoryMappingRepository;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.entity.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NeedCategoryMappingService {

    private static final int AT_CATEGORY_VERIFICATION_FRESHNESS_DAYS = 180;

    private final NeedCategoryMappingRepository
            needCategoryMappingRepository;

    private final FunctionalNeedRepository functionalNeedRepository;

    private final ATCategoryRepository atCategoryRepository;

    private final UserRepository userRepository;

    private final AuditLogService auditLogService;

    public NeedCategoryMappingService(
            NeedCategoryMappingRepository needCategoryMappingRepository,
            FunctionalNeedRepository functionalNeedRepository,
            ATCategoryRepository atCategoryRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.needCategoryMappingRepository =
                needCategoryMappingRepository;

        this.functionalNeedRepository =
                functionalNeedRepository;

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
    public List<NeedCategoryMappingResponse> getPublicMappings() {

        return needCategoryMappingRepository
                .findPublicEligibleMappings(
                        categoryFreshnessCutoff())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NeedCategoryMappingResponse> getPublicMappingsByFunctionalNeed(
            UUID functionalNeedId) {

        return needCategoryMappingRepository
                .findPublicEligibleMappingsByFunctionalNeed(
                        functionalNeedId,
                        categoryFreshnessCutoff())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NeedCategoryMappingResponse> getPublicMappingsByCategory(
            UUID atCategoryId) {

        return needCategoryMappingRepository
                .findPublicEligibleMappingsByCategory(
                        atCategoryId,
                        categoryFreshnessCutoff())
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
    public List<NeedCategoryMappingResponse> getAdminMappings() {

        return needCategoryMappingRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NeedCategoryMappingResponse> getAdminMappingsByFunctionalNeed(
            UUID functionalNeedId) {

        if (!functionalNeedRepository.existsById(functionalNeedId)) {
            throw new ResourceNotFoundException(
                    "Functional need not found");
        }

        return needCategoryMappingRepository
                .findByFunctionalNeed_IdOrderByAtCategory_NameAsc(
                        functionalNeedId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NeedCategoryMappingResponse> getAdminMappingsByCategory(
            UUID atCategoryId) {

        if (!atCategoryRepository.existsById(atCategoryId)) {
            throw new ResourceNotFoundException(
                    "AT category not found");
        }

        return needCategoryMappingRepository
                .findByAtCategory_IdOrderByFunctionalNeed_LabelAsc(
                        atCategoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NeedCategoryMappingResponse getAdminMapping(
            UUID mappingId) {

        return toResponse(requireMapping(mappingId));
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN CREATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public NeedCategoryMappingResponse createMapping(
            NeedCategoryMappingCreateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        FunctionalNeed functionalNeed =
                functionalNeedRepository
                        .findById(request.functionalNeedId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Functional need not found"));

        ATCategory atCategory =
                atCategoryRepository
                        .findById(request.atCategoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "AT category not found"));

        if (needCategoryMappingRepository
                .existsByFunctionalNeed_IdAndAtCategory_Id(
                        request.functionalNeedId(),
                        request.atCategoryId())) {

            throw new ConflictException(
                    "Need category mapping already exists");
        }

        NeedCategoryMapping mapping =
                new NeedCategoryMapping();

        mapping.setFunctionalNeed(functionalNeed);
        mapping.setAtCategory(atCategory);
        mapping.setRationale(request.rationale().trim());
        mapping.setActive(true);

        mapping =
                needCategoryMappingRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.NEED_CATEGORY_MAPPING,
                mapping.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "functional_need_id",
                    "at_category_id",
                    "rationale",
                    "active"
                },
                "Need category mapping created");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN UPDATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public NeedCategoryMappingResponse updateMapping(
            UUID mappingId,
            NeedCategoryMappingUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        NeedCategoryMapping mapping =
                requireMapping(mappingId);

        String rationale =
                request.rationale().trim();

        if (Objects.equals(
                mapping.getRationale(),
                rationale)) {

            return toResponse(mapping);
        }

        mapping.setRationale(rationale);

        mapping =
                needCategoryMappingRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.NEED_CATEGORY_MAPPING,
                mapping.getId(),
                AuditActionType.UPDATE,
                new String[] {"rationale"},
                "Need category mapping updated");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN ACTIVATE / ARCHIVE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public NeedCategoryMappingResponse setMappingActive(
            UUID mappingId,
            boolean active,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        NeedCategoryMapping mapping =
                requireMapping(mappingId);

        /*
         * Activation/archive is intentionally idempotent.
         * Repeating the same state change does not create
         * another audit event.
         */
        if (mapping.isActive() == active) {
            return toResponse(mapping);
        }

        mapping.setActive(active);

        mapping =
                needCategoryMappingRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.NEED_CATEGORY_MAPPING,
                mapping.getId(),
                active
                        ? AuditActionType.ACTIVATE
                        : AuditActionType.ARCHIVE,
                new String[] {"active"},
                active
                        ? "Need category mapping activated"
                        : "Need category mapping archived");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * HELPERS
     * -------------------------------------------------------------------------
     */

    private NeedCategoryMapping requireMapping(
            UUID mappingId) {

        return needCategoryMappingRepository
                .findById(mappingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Need category mapping not found"));
    }

    private User requireActor(String actorEmail) {

        return userRepository
                .findByEmail(actorEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    private LocalDate categoryFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        AT_CATEGORY_VERIFICATION_FRESHNESS_DAYS);
    }

    private NeedCategoryMappingResponse toResponse(
            NeedCategoryMapping mapping) {

        FunctionalNeed functionalNeed =
                mapping.getFunctionalNeed();

        ATCategory atCategory =
                mapping.getAtCategory();

        return new NeedCategoryMappingResponse(
                mapping.getId(),
                functionalNeed.getId(),
                functionalNeed.getLabel(),
                atCategory.getId(),
                atCategory.getCode(),
                atCategory.getName(),
                mapping.getRationale(),
                mapping.isActive(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt());
    }
}