package com.civicaid.backend.service;

import com.civicaid.backend.dto.LifecycleRecordCreateRequest;
import com.civicaid.backend.dto.LifecycleRecordResponse;
import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.ATProduct;
import com.civicaid.backend.entity.LifecycleRecord;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderLocation;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceLocation;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATCategoryRepository;
import com.civicaid.backend.repository.ATProductRepository;
import com.civicaid.backend.repository.LifecycleRecordRepository;
import com.civicaid.backend.repository.ProviderServiceLocationRepository;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LifecycleRecordService {

    private static final int AT_CONTENT_FRESHNESS_DAYS = 180;
    private static final int DIRECTORY_FRESHNESS_DAYS = 90;

    private final LifecycleRecordRepository lifecycleRecordRepository;
    private final UserRepository userRepository;
    private final ATCategoryRepository atCategoryRepository;
    private final ATProductRepository atProductRepository;
    private final ProviderServiceLocationRepository
            providerServiceLocationRepository;

    public LifecycleRecordService(
            LifecycleRecordRepository lifecycleRecordRepository,
            UserRepository userRepository,
            ATCategoryRepository atCategoryRepository,
            ATProductRepository atProductRepository,
            ProviderServiceLocationRepository
                    providerServiceLocationRepository) {

        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.userRepository = userRepository;
        this.atCategoryRepository = atCategoryRepository;
        this.atProductRepository = atProductRepository;
        this.providerServiceLocationRepository =
                providerServiceLocationRepository;
    }

    @Transactional(readOnly = true)
    public List<LifecycleRecordResponse> getMyLifecycleRecords(
            String userEmail) {

        User user = requireUser(userEmail);

        return lifecycleRecordRepository
                .findByUser_IdOrderByAcquiredOnDescCreatedAtDesc(
                        user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LifecycleRecordResponse getMyLifecycleRecord(
            UUID lifecycleRecordId,
            String userEmail) {

        User user = requireUser(userEmail);

        LifecycleRecord record =
                lifecycleRecordRepository
                        .findByIdAndUser_Id(
                                lifecycleRecordId,
                                user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Lifecycle record not found"));

        return toResponse(record);
    }

    @Transactional
    public LifecycleRecordResponse createLifecycleRecord(
            LifecycleRecordCreateRequest request,
            String userEmail) {

        User user = requireUser(userEmail);

        validateCatalogReference(request);

        LifecycleRecord predecessor =
                resolveReplacementPredecessor(
                        request.replacesLifecycleRecordId(),
                        user.getId());

        ProviderServiceLocation preferredServiceLocation =
                resolvePreferredServiceLocation(
                        request.preferredProviderServiceLocationId());

        LifecycleRecord record = new LifecycleRecord();

        record.setUser(user);
        record.setItemName(request.itemName().trim());
        record.setAcquiredOn(request.acquiredOn());
        record.setAcquisitionMethod(
                request.acquisitionMethod());
        record.setConditionAtAcquisition(
                request.conditionAtAcquisition());
        record.setLifecycleStatus(
                request.lifecycleStatus());
        record.setWarrantyExpiresOn(
                request.warrantyExpiresOn());
        record.setReplacementDueOn(
                request.replacementDueOn());
        record.setReassessmentDueOn(
                request.reassessmentDueOn());

        if (request.atProductId() != null) {
            ATProduct product =
                    requireEligibleProduct(
                            request.atProductId());

            record.setAtProduct(product);
        } else if (request.atCategoryId() != null) {
            ATCategory category =
                    requireEligibleCategory(
                            request.atCategoryId());

            record.setAtCategory(category);
        }

        record.setPreferredProviderServiceLocation(
                preferredServiceLocation);

        if (predecessor != null) {
            if (lifecycleRecordRepository
                    .existsByReplacesLifecycleRecord_Id(
                            predecessor.getId())) {

                throw new ConflictException(
                        "Lifecycle record has already been replaced");
            }

            record.setReplacesLifecycleRecord(predecessor);
        }

        LifecycleRecord savedRecord =
                lifecycleRecordRepository.save(record);

        return toResponse(savedRecord);
    }

    @Transactional
    public void deleteMyLifecycleRecord(
            UUID lifecycleRecordId,
            String userEmail) {

        User user = requireUser(userEmail);

        LifecycleRecord record =
                lifecycleRecordRepository
                        .findByIdAndUser_Id(
                                lifecycleRecordId,
                                user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Lifecycle record not found"));

        lifecycleRecordRepository.delete(record);
    }

    private void validateCatalogReference(
            LifecycleRecordCreateRequest request) {

        if (request.atProductId() != null
                && request.atCategoryId() != null) {

            throw new BadRequestException(
                    "A lifecycle record cannot reference both "
                            + "an AT product and an AT category");
        }
    }

    private ATProduct requireEligibleProduct(
            UUID productId) {

        LocalDate cutoff =
                LocalDate.now()
                        .minusDays(
                                AT_CONTENT_FRESHNESS_DAYS);

        ATProduct product =
                atProductRepository.findById(
                                productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "AT product not found"));

        if (!isEligible(
                product.isActive(),
                product.getVerificationStatus(),
                product.getLastVerifiedAt(),
                cutoff)) {

            throw new ResourceNotFoundException(
                    "AT product is not currently available");
        }

        if (product.getAtCategory() == null) {
            throw new ResourceNotFoundException(
                    "AT product category is not available");
        }

        requireEligibleCategory(
                product.getAtCategory().getId());

        return product;
    }

    private ATCategory requireEligibleCategory(
            UUID categoryId) {

        LocalDate cutoff =
                LocalDate.now()
                        .minusDays(
                                AT_CONTENT_FRESHNESS_DAYS);

        ATCategory category =
                atCategoryRepository.findById(
                                categoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "AT category not found"));

        if (!isEligible(
                category.isActive(),
                category.getVerificationStatus(),
                category.getLastVerifiedAt(),
                cutoff)) {

            throw new ResourceNotFoundException(
                    "AT category is not currently available");
        }

        return category;
    }

    private ProviderServiceLocation
            resolvePreferredServiceLocation(
                    UUID serviceLocationId) {

        if (serviceLocationId == null) {
            return null;
        }

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

        LocalDate cutoff =
                today.minusDays(
                        DIRECTORY_FRESHNESS_DAYS);

        if (!isEligible(
                serviceLocation.isActive(),
                serviceLocation.getVerificationStatus(),
                serviceLocation.getLastVerifiedAt(),
                cutoff)) {

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
                cutoff)) {

            return false;
        }

        Provider provider = service.getProvider();

        if (provider == null
                || !isEligible(
                        provider.isActive(),
                        provider.getVerificationStatus(),
                        provider.getLastVerifiedAt(),
                        cutoff)) {

            return false;
        }

        if (!isEligible(
                location.isActive(),
                location.getVerificationStatus(),
                location.getLastVerifiedAt(),
                cutoff)) {

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

    private LifecycleRecord
            resolveReplacementPredecessor(
                    UUID predecessorId,
                    UUID userId) {

        if (predecessorId == null) {
            return null;
        }

        LifecycleRecord predecessor =
                lifecycleRecordRepository
                        .findByIdAndUser_Id(
                                predecessorId,
                                userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Replacement predecessor "
                                                + "lifecycle record not found"));

        return predecessor;
    }

    private LifecycleRecordResponse toResponse(
            LifecycleRecord record) {

        UUID atProductId =
                record.getAtProduct() != null
                        ? record.getAtProduct().getId()
                        : null;

        UUID atCategoryId =
                record.getAtCategory() != null
                        ? record.getAtCategory().getId()
                        : null;

        UUID preferredProviderServiceLocationId =
                record.getPreferredProviderServiceLocation() != null
                        ? record.getPreferredProviderServiceLocation()
                                .getId()
                        : null;

        UUID replacesLifecycleRecordId =
                record.getReplacesLifecycleRecord() != null
                        ? record.getReplacesLifecycleRecord().getId()
                        : null;

        return new LifecycleRecordResponse(
                record.getId(),
                record.getItemName(),
                atProductId,
                atCategoryId,
                record.getAcquiredOn(),
                record.getAcquisitionMethod(),
                record.getConditionAtAcquisition(),
                record.getLifecycleStatus(),
                record.getWarrantyExpiresOn(),
                preferredProviderServiceLocationId,
                record.getReplacementDueOn(),
                record.getReassessmentDueOn(),
                replacesLifecycleRecordId,
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