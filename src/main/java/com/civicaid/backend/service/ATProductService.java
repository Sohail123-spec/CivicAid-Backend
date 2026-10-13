
package com.civicaid.backend.service;

import com.civicaid.backend.dto.ATProductCreateRequest;
import com.civicaid.backend.dto.ATProductResponse;
import com.civicaid.backend.dto.ATProductUpdateRequest;
import com.civicaid.backend.dto.ATProductVerificationRecordResponse;
import com.civicaid.backend.dto.ATProductVerificationRequest;
import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.ATProduct;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATCategoryRepository;
import com.civicaid.backend.repository.ATProductRepository;
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
public class ATProductService {

    private static final int VERIFICATION_FRESHNESS_DAYS = 180;

    private final ATProductRepository atProductRepository;
    private final ATCategoryRepository atCategoryRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ATProductService(
            ATProductRepository atProductRepository,
            ATCategoryRepository atCategoryRepository,
            VerificationRecordRepository verificationRecordRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.atProductRepository = atProductRepository;
        this.atCategoryRepository = atCategoryRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    
@Transactional(readOnly = true)
public List<ATProductResponse> getPublicProducts() {
    return atProductRepository
            .findByActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqualOrderByDisplayOrderAscNameAsc(
                    VerificationStatus.VERIFIED,
                    freshnessCutoff())
            .stream()
            .filter(product -> isPublicCategory(
                    product.getAtCategory().getId()))
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ATProductResponse> getAllProductsForAdmin() {
    return atProductRepository.findAll().stream()
            .sorted(java.util.Comparator.comparingInt(ATProduct::getDisplayOrder)
                    .thenComparing(ATProduct::getName, String.CASE_INSENSITIVE_ORDER))
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public ATProductResponse getPublicProduct(UUID productId) {
    ATProduct product = atProductRepository
            .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                    productId,
                    VerificationStatus.VERIFIED,
                    freshnessCutoff())
            .orElseThrow(() ->
                    new ResourceNotFoundException("AT product not found"));

    requirePublicCategory(product.getAtCategory().getId());

    return toResponse(product);
}

private boolean isPublicCategory(UUID categoryId) {
    return atCategoryRepository
            .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                    categoryId,
                    VerificationStatus.VERIFIED,
                    freshnessCutoff())
            .isPresent();
}

    @Transactional
    public ATProductResponse createProduct(
            UUID categoryId,
            ATProductCreateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ATCategory category = atCategoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("AT category not found"));

        String code = request.code().trim();
        String name = request.name().trim();

        if (atProductRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("AT product code already exists");
        }

        if (atProductRepository.existsByNameIgnoreCaseAndAtCategory_Id(
                name, categoryId)) {
            throw new ConflictException(
                    "AT product name already exists in this category");
        }

        ATProduct product = new ATProduct();
        product.setAtCategory(category);
        product.setCode(code);
        product.setName(name);
        product.setDescription(request.description().trim());
        product.setConsiderations(normalizeOptional(request.considerations()));
        product.setDisplayOrder(request.displayOrder());
        product.setSourceName(request.sourceName().trim());
        product.setSourceUrl(normalizeOptional(request.sourceUrl()));
        product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        product.setLastVerifiedAt(null);
        product.setActive(true);

        product = atProductRepository.save(product);

        auditLogService.record(
                actor,
                AuditTargetType.AT_PRODUCT,
                product.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "at_category_id", "code", "name", "description",
                    "considerations", "display_order", "source_name",
                    "source_url", "verification_status", "active"
                },
                "AT product created");

        return toResponse(product);
    }

    @Transactional
    public ATProductResponse updateProduct(
            UUID productId,
            ATProductUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ATProduct product = atProductRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("AT product not found"));

        UUID categoryId = product.getAtCategory().getId();
        String name = request.name().trim();

        if (atProductRepository
                .existsByNameIgnoreCaseAndAtCategory_IdAndIdNot(
                        name, categoryId, productId)) {
            throw new ConflictException(
                    "AT product name already exists in this category");
        }

        String description = request.description().trim();
        String considerations = normalizeOptional(request.considerations());
        String sourceName = request.sourceName().trim();
        String sourceUrl = normalizeOptional(request.sourceUrl());

        List<String> changedFields = new ArrayList<>();

        boolean substantiveChange =
                !Objects.equals(product.getName(), name)
                || !Objects.equals(product.getDescription(), description)
                || !Objects.equals(
                        product.getConsiderations(), considerations)
                || !Objects.equals(product.getSourceName(), sourceName)
                || !Objects.equals(product.getSourceUrl(), sourceUrl);

        addIfChanged(changedFields, "name", product.getName(), name);
        addIfChanged(
                changedFields, "description",
                product.getDescription(), description);
        addIfChanged(
                changedFields, "considerations",
                product.getConsiderations(), considerations);
        addIfChanged(
                changedFields, "display_order",
                product.getDisplayOrder(), request.displayOrder());
        addIfChanged(
                changedFields, "source_name",
                product.getSourceName(), sourceName);
        addIfChanged(
                changedFields, "source_url",
                product.getSourceUrl(), sourceUrl);

        if (substantiveChange) {
            addIfChanged(
                    changedFields, "verification_status",
                    product.getVerificationStatus(),
                    VerificationStatus.UNVERIFIED);
            addIfChanged(
                    changedFields, "last_verified_at",
                    product.getLastVerifiedAt(), null);

            product.setVerificationStatus(VerificationStatus.UNVERIFIED);
            product.setLastVerifiedAt(null);
        }

        product.setName(name);
        product.setDescription(description);
        product.setConsiderations(considerations);
        product.setDisplayOrder(request.displayOrder());
        product.setSourceName(sourceName);
        product.setSourceUrl(sourceUrl);

        product = atProductRepository.save(product);

        if (!changedFields.isEmpty()) {
            auditLogService.record(
                    actor,
                    AuditTargetType.AT_PRODUCT,
                    product.getId(),
                    AuditActionType.UPDATE,
                    changedFields.toArray(String[]::new),
                    "AT product updated");
        }

        return toResponse(product);
    }
    
    @Transactional
    public ATProductResponse setProductActive(
            UUID productId,
            boolean active,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ATProduct product = atProductRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("AT product not found"));

        // Do not create duplicate audit entries for a no-op request.
        if (product.isActive() == active) {
            return toResponse(product);
        }

        product.setActive(active);
        product = atProductRepository.save(product);

        auditLogService.record(
                actor,
                AuditTargetType.AT_PRODUCT,
                product.getId(),
                active ? AuditActionType.ACTIVATE : AuditActionType.ARCHIVE,
                new String[] {"active"},
                active ? "AT product activated" : "AT product archived");

        return toResponse(product);
    }
    @Transactional
    public ATProductVerificationRecordResponse verifyProduct(
            UUID productId,
            ATProductVerificationRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ATProduct product = atProductRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("AT product not found"));

        String reviewNotes = normalizeOptional(request.reviewNotes());

        if (request.result() != VerificationResult.VERIFIED
                && (reviewNotes == null || reviewNotes.isBlank())) {
            throw new BadRequestException(
                    "Review notes are required when verification is rejected or revoked");
        }

        String sourceName = request.sourceName().trim();
        String sourceUrl = normalizeOptional(request.sourceUrl());

        List<String> changedFields = new ArrayList<>();

        addIfChanged(
                changedFields, "source_name",
                product.getSourceName(), sourceName);
        addIfChanged(
                changedFields, "source_url",
                product.getSourceUrl(), sourceUrl);

        VerificationStatus resultingStatus =
                request.result() == VerificationResult.VERIFIED
                        ? VerificationStatus.VERIFIED
                        : VerificationStatus.UNVERIFIED;

        LocalDate resultingVerifiedDate =
                request.result() == VerificationResult.VERIFIED
                        ? LocalDate.now()
                        : null;

        addIfChanged(
                changedFields, "verification_status",
                product.getVerificationStatus(), resultingStatus);
        addIfChanged(
                changedFields, "last_verified_at",
                product.getLastVerifiedAt(), resultingVerifiedDate);

        changedFields.add("verification_result");

        if (normalizeOptional(request.sourceVersion()) != null) {
            changedFields.add("source_version");
        }
        if (reviewNotes != null) {
            changedFields.add("review_notes");
        }

        product.setSourceName(sourceName);
        product.setSourceUrl(sourceUrl);
        product.setVerificationStatus(resultingStatus);
        product.setLastVerifiedAt(resultingVerifiedDate);
        product = atProductRepository.save(product);

        VerificationRecord record = new VerificationRecord();
        record.setVerifierUser(actor);
        record.setAtProduct(product);
        record.setVerificationResult(request.result());
        record.setSourceName(sourceName);
        record.setSourceUrl(sourceUrl);
        record.setSourceVersion(
                normalizeOptional(request.sourceVersion()));
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
                AuditTargetType.AT_PRODUCT,
                product.getId(),
                actionType,
                changedFields.toArray(String[]::new),
                "AT product verification recorded: "
                        + request.result().name());

        return toVerificationResponse(record);
    }

    @Transactional(readOnly = true)
    public List<ATProductVerificationRecordResponse> getVerificationHistory(
            UUID productId) {

        if (!atProductRepository.existsById(productId)) {
            throw new ResourceNotFoundException("AT product not found");
        }

        return verificationRecordRepository
                .findByAtProduct_IdOrderByPerformedAtDesc(productId)
                .stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    private void requirePublicCategory(UUID categoryId) {
        atCategoryRepository
                .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                        categoryId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff())
                .orElseThrow(() ->
                        new ResourceNotFoundException("AT category not found"));
    }

    private User requireActor(String actorEmail) {
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated admin account was not found"));
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

    private ATProductResponse toResponse(ATProduct product) {
        return new ATProductResponse(
                product.getId(),
                product.getAtCategory().getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getConsiderations(),
                product.getDisplayOrder(),
                product.getSourceName(),
                product.getSourceUrl(),
                product.getVerificationStatus(),
                product.getLastVerifiedAt(),
                product.isActive());
    }

    private ATProductVerificationRecordResponse toVerificationResponse(
            VerificationRecord record) {
        return new ATProductVerificationRecordResponse(
                record.getId(),
                record.getAtProduct().getId(),
                record.getVerificationResult(),
                record.getSourceName(),
                record.getSourceUrl(),
                record.getSourceVersion(),
                record.getReviewNotes(),
                record.getPerformedAt());
    }
}