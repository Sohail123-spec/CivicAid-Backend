package com.civicaid.backend.service;

import com.civicaid.backend.dto.ATCategoryCreateRequest;
import com.civicaid.backend.dto.ATCategoryResponse;
import com.civicaid.backend.dto.ATCategoryUpdateRequest;
import com.civicaid.backend.dto.ATCategoryVerificationRequest;
import com.civicaid.backend.dto.VerificationRecordResponse;
import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATCategoryRepository;
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
public class ATCategoryService {

    private static final long VERIFICATION_FRESHNESS_DAYS = 180;

    private final ATCategoryRepository atCategoryRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ATCategoryService(
            ATCategoryRepository atCategoryRepository,
            VerificationRecordRepository verificationRecordRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.atCategoryRepository = atCategoryRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ATCategoryResponse> getPublicCategories() {
        return atCategoryRepository
                .findByActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqualOrderByDisplayOrderAscNameAsc(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ATCategoryResponse> getAllCategoriesForAdmin() {
        return atCategoryRepository.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(ATCategory::getDisplayOrder)
                        .thenComparing(ATCategory::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ATCategoryResponse getPublicCategory(UUID categoryId) {
        ATCategory category = atCategoryRepository
                .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                        categoryId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff())
                .orElseThrow(() -> new ResourceNotFoundException("AT category not found"));

        return toResponse(category);
    }

    @Transactional
    public ATCategoryResponse createCategory(ATCategoryCreateRequest request, String actorEmail) {
        User actor = requireActor(actorEmail);
        String code = request.code().trim();
        String name = request.name().trim();

        if (atCategoryRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("AT category code already exists");
        }
        if (atCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("AT category name already exists");
        }

        ATCategory category = new ATCategory();
        category.setCode(code);
        category.setName(name);
        category.setDescription(normalizeOptional(request.description()));
        category.setDisplayOrder(request.displayOrder());
        category.setSourceName(request.sourceName().trim());
        category.setSourceUrl(normalizeOptional(request.sourceUrl()));
        category.setActive(request.active());
        category.setVerificationStatus(VerificationStatus.UNVERIFIED);
        category.setLastVerifiedAt(null);

        category = atCategoryRepository.save(category);
        auditLogService.record(
                actor,
                AuditTargetType.AT_CATEGORY,
                category.getId(),
                AuditActionType.CREATE,
                new String[] {"code", "name", "description", "display_order", "source_name", "source_url", "active", "verification_status"},
                "AT category created");
        return toResponse(category);
    }

    @Transactional
    public ATCategoryResponse updateCategory(
            UUID categoryId,
            ATCategoryUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);
        ATCategory category = atCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("AT category not found"));

        String name = request.name().trim();
        if (atCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw new ConflictException("AT category name already exists");
        }

        String description = normalizeOptional(request.description());
        String sourceName = request.sourceName().trim();
        String sourceUrl = normalizeOptional(request.sourceUrl());
        List<String> changedFields = new ArrayList<>();

        boolean substantiveChange = !Objects.equals(category.getName(), name)
                || !Objects.equals(category.getDescription(), description)
                || !Objects.equals(category.getSourceName(), sourceName)
                || !Objects.equals(category.getSourceUrl(), sourceUrl);

        addIfChanged(changedFields, "name", category.getName(), name);
        addIfChanged(changedFields, "description", category.getDescription(), description);
        addIfChanged(changedFields, "display_order", category.getDisplayOrder(), request.displayOrder());
        addIfChanged(changedFields, "source_name", category.getSourceName(), sourceName);
        addIfChanged(changedFields, "source_url", category.getSourceUrl(), sourceUrl);
        addIfChanged(changedFields, "active", category.isActive(), request.active());

        if (substantiveChange) {
            addIfChanged(changedFields, "verification_status", category.getVerificationStatus(), VerificationStatus.UNVERIFIED);
            addIfChanged(changedFields, "last_verified_at", category.getLastVerifiedAt(), null);
            category.setVerificationStatus(VerificationStatus.UNVERIFIED);
            category.setLastVerifiedAt(null);
        }

        category.setName(name);
        category.setDescription(description);
        category.setDisplayOrder(request.displayOrder());
        category.setSourceName(sourceName);
        category.setSourceUrl(sourceUrl);
        category.setActive(request.active());
        category = atCategoryRepository.save(category);

        if (!changedFields.isEmpty()) {
            AuditActionType actionType = AuditActionType.UPDATE;
            if (changedFields.size() == 1 && changedFields.contains("active")) {
                actionType = request.active() ? AuditActionType.ACTIVATE : AuditActionType.ARCHIVE;
            }
            auditLogService.record(
                    actor,
                    AuditTargetType.AT_CATEGORY,
                    category.getId(),
                    actionType,
                    changedFields.toArray(String[]::new),
                    "AT category updated");
        }

        return toResponse(category);
    }

    @Transactional
    public VerificationRecordResponse verifyCategory(
            UUID categoryId,
            ATCategoryVerificationRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);
        ATCategory category = atCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("AT category not found"));

        String reviewNotes = normalizeOptional(request.reviewNotes());
        if (request.result() != VerificationResult.VERIFIED
                && (reviewNotes == null || reviewNotes.isBlank())) {
            throw new BadRequestException("Review notes are required when verification is rejected or revoked");
        }

        String verifiedSourceName = request.sourceName().trim();
        String verifiedSourceUrl = normalizeOptional(request.sourceUrl());
        List<String> changedFields = new ArrayList<>();
        addIfChanged(changedFields, "source_name", category.getSourceName(), verifiedSourceName);
        addIfChanged(changedFields, "source_url", category.getSourceUrl(), verifiedSourceUrl);
        VerificationStatus resultingStatus = request.result() == VerificationResult.VERIFIED
                ? VerificationStatus.VERIFIED
                : VerificationStatus.UNVERIFIED;
        LocalDate resultingVerifiedDate = request.result() == VerificationResult.VERIFIED
                ? LocalDate.now()
                : null;
        addIfChanged(changedFields, "verification_status", category.getVerificationStatus(), resultingStatus);
        addIfChanged(changedFields, "last_verified_at", category.getLastVerifiedAt(), resultingVerifiedDate);
        changedFields.add("verification_result");
        if (request.sourceVersion() != null && !request.sourceVersion().isBlank()) {
            changedFields.add("source_version");
        }
        if (reviewNotes != null) {
            changedFields.add("review_notes");
        }

        category.setSourceName(verifiedSourceName);
        category.setSourceUrl(verifiedSourceUrl);
        category.setVerificationStatus(resultingStatus);
        category.setLastVerifiedAt(resultingVerifiedDate);
        category = atCategoryRepository.save(category);

        VerificationRecord record = new VerificationRecord();
        record.setVerifierUser(actor);
        record.setAtCategory(category);
        record.setVerificationResult(request.result());
        record.setSourceName(verifiedSourceName);
        record.setSourceUrl(verifiedSourceUrl);
        record.setSourceVersion(normalizeOptional(request.sourceVersion()));
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
                AuditTargetType.AT_CATEGORY,
                category.getId(),
                actionType,
                changedFields.toArray(String[]::new),
                "AT category verification recorded: " + request.result().name());

        return toVerificationResponse(record);
    }

    @Transactional(readOnly = true)
    public List<VerificationRecordResponse> getVerificationHistory(UUID categoryId) {
        if (!atCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("AT category not found");
        }
        return verificationRecordRepository.findByAtCategory_IdOrderByPerformedAtDesc(categoryId)
                .stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    private User requireActor(String actorEmail) {
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated admin account was not found"));
    }

    private void addIfChanged(List<String> fields, String field, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            fields.add(field);
        }
    }

    private LocalDate freshnessCutoff() {
        return LocalDate.now().minusDays(VERIFICATION_FRESHNESS_DAYS);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ATCategoryResponse toResponse(ATCategory category) {
        return new ATCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getSourceName(),
                category.getSourceUrl(),
                category.getVerificationStatus(),
                category.getLastVerifiedAt(),
                category.isActive());
    }

    private VerificationRecordResponse toVerificationResponse(VerificationRecord record) {
        return new VerificationRecordResponse(
                record.getId(),
                record.getAtCategory().getId(),
                record.getVerificationResult(),
                record.getSourceName(),
                record.getSourceUrl(),
                record.getSourceVersion(),
                record.getReviewNotes(),
                record.getPerformedAt());
    }
}
