package com.civicaid.backend.service;

import com.civicaid.backend.dto.SavedResourceCreateRequest;
import com.civicaid.backend.dto.SavedResourceResponse;
import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.ATProduct;
import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderLocation;
import com.civicaid.backend.entity.ProviderServiceLocation;
import com.civicaid.backend.entity.SavedResource;
import com.civicaid.backend.entity.SavedResourceType;
import com.civicaid.backend.entity.Scheme;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATCategoryRepository;
import com.civicaid.backend.repository.ATProductRepository;
import com.civicaid.backend.repository.ProviderLocationRepository;
import com.civicaid.backend.repository.ProviderRepository;
import com.civicaid.backend.repository.ProviderServiceLocationRepository;
import com.civicaid.backend.repository.SavedResourceRepository;
import com.civicaid.backend.repository.SchemeRepository;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedResourceService {

    private static final int AT_CONTENT_FRESHNESS_DAYS = 180;
    private static final int DIRECTORY_FRESHNESS_DAYS = 90;

    private final SavedResourceRepository savedResourceRepository;
    private final UserRepository userRepository;
    private final ATCategoryRepository atCategoryRepository;
    private final ATProductRepository atProductRepository;
    private final SchemeRepository schemeRepository;
    private final ProviderRepository providerRepository;
    private final ProviderLocationRepository providerLocationRepository;
    private final ProviderServiceLocationRepository providerServiceLocationRepository;

    public SavedResourceService(
            SavedResourceRepository savedResourceRepository,
            UserRepository userRepository,
            ATCategoryRepository atCategoryRepository,
            ATProductRepository atProductRepository,
            SchemeRepository schemeRepository,
            ProviderRepository providerRepository,
            ProviderLocationRepository providerLocationRepository,
            ProviderServiceLocationRepository providerServiceLocationRepository) {

        this.savedResourceRepository = savedResourceRepository;
        this.userRepository = userRepository;
        this.atCategoryRepository = atCategoryRepository;
        this.atProductRepository = atProductRepository;
        this.schemeRepository = schemeRepository;
        this.providerRepository = providerRepository;
        this.providerLocationRepository = providerLocationRepository;
        this.providerServiceLocationRepository = providerServiceLocationRepository;
    }

    @Transactional(readOnly = true)
    public List<SavedResourceResponse> getMySavedResources(String userEmail) {

        User user = requireUser(userEmail);

        return savedResourceRepository
                .findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavedResourceResponse saveResource(
            SavedResourceCreateRequest request,
            String userEmail) {

        User user = requireUser(userEmail);

        SavedResource savedResource = new SavedResource();
        savedResource.setUser(user);

        switch (request.resourceType()) {
            case AT_CATEGORY -> saveAtCategory(
                    savedResource,
                    user.getId(),
                    request.resourceId());

            case AT_PRODUCT -> saveAtProduct(
                    savedResource,
                    user.getId(),
                    request.resourceId());

            case SCHEME -> saveScheme(
                    savedResource,
                    user.getId(),
                    request.resourceId());

            case PROVIDER -> saveProvider(
                    savedResource,
                    user.getId(),
                    request.resourceId());

            case PROVIDER_LOCATION -> saveProviderLocation(
                    savedResource,
                    user.getId(),
                    request.resourceId());

            case PROVIDER_SERVICE_LOCATION -> saveProviderServiceLocation(
                    savedResource,
                    user.getId(),
                    request.resourceId());
        }

        SavedResource saved = savedResourceRepository.save(savedResource);

        return toResponse(saved);
    }

    @Transactional
    public void deleteMySavedResource(
            UUID savedResourceId,
            String userEmail) {

        User user = requireUser(userEmail);

        SavedResource savedResource = savedResourceRepository
                .findByIdAndUser_Id(savedResourceId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Saved resource not found"));

        savedResourceRepository.delete(savedResource);
    }

    private void saveAtCategory(
            SavedResource savedResource,
            UUID userId,
            UUID resourceId) {

        if (savedResourceRepository.existsByUser_IdAndAtCategory_Id(
                userId,
                resourceId)) {

            throw new ConflictException(
                    "AT category is already saved");
        }

        ATCategory category = atCategoryRepository
                .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                        resourceId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(AT_CONTENT_FRESHNESS_DAYS))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AT category is not currently available for saving"));

        savedResource.setAtCategory(category);
    }

    private void saveAtProduct(
            SavedResource savedResource,
            UUID userId,
            UUID resourceId) {

        if (savedResourceRepository.existsByUser_IdAndAtProduct_Id(
                userId,
                resourceId)) {

            throw new ConflictException(
                    "AT product is already saved");
        }

        ATProduct product = atProductRepository
                .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                        resourceId,
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(AT_CONTENT_FRESHNESS_DAYS))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AT product is not currently available for saving"));

        ATCategory category = atCategoryRepository
                .findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                        product.getAtCategory().getId(),
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(AT_CONTENT_FRESHNESS_DAYS))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AT product category is not currently available"));

        savedResource.setAtProduct(product);
    }

    private void saveScheme(
            SavedResource savedResource,
            UUID userId,
            UUID resourceId) {

        if (savedResourceRepository.existsByUser_IdAndScheme_Id(
                userId,
                resourceId)) {

            throw new ConflictException(
                    "Scheme is already saved");
        }

        Scheme scheme = schemeRepository.findById(resourceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Scheme not found"));

        LocalDate today = LocalDate.now();
        LocalDate cutoff = freshnessCutoff(DIRECTORY_FRESHNESS_DAYS);

        boolean currentlyEligible =
                scheme.isActive()
                        && scheme.getVerificationStatus()
                                == VerificationStatus.VERIFIED
                        && scheme.getLastVerifiedAt() != null
                        && !scheme.getLastVerifiedAt().isBefore(cutoff)
                        && !scheme.getLastVerifiedAt().isAfter(today)
                        && (scheme.getEffectiveFrom() == null
                                || !scheme.getEffectiveFrom().isAfter(today))
                        && (scheme.getEffectiveTo() == null
                                || !scheme.getEffectiveTo().isBefore(today));

        if (!currentlyEligible) {
            throw new ResourceNotFoundException(
                    "Scheme is not currently available for saving");
        }

        savedResource.setScheme(scheme);
    }

    private void saveProvider(
            SavedResource savedResource,
            UUID userId,
            UUID resourceId) {

        if (savedResourceRepository.existsByUser_IdAndProvider_Id(
                userId,
                resourceId)) {

            throw new ConflictException(
                    "Provider is already saved");
        }

        Provider provider = providerRepository
                .findPubliclyDiscoverableProviders(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(DIRECTORY_FRESHNESS_DAYS),
                        LocalDate.now())
                .stream()
                .filter(candidate ->
                        candidate.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider is not currently available for saving"));

        savedResource.setProvider(provider);
    }

    private void saveProviderLocation(
            SavedResource savedResource,
            UUID userId,
            UUID resourceId) {

        if (savedResourceRepository.existsByUser_IdAndProviderLocation_Id(
                userId,
                resourceId)) {

            throw new ConflictException(
                    "Provider location is already saved");
        }

        ProviderLocation location = providerLocationRepository
                .findPubliclyDiscoverableLocations(
                        VerificationStatus.VERIFIED,
                        freshnessCutoff(DIRECTORY_FRESHNESS_DAYS),
                        LocalDate.now())
                .stream()
                .filter(candidate ->
                        candidate.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider location is not currently available for saving"));

        savedResource.setProviderLocation(location);
    }

    private void saveProviderServiceLocation(
            SavedResource savedResource,
            UUID userId,
            UUID resourceId) {

        if (savedResourceRepository
                .existsByUser_IdAndProviderServiceLocation_Id(
                        userId,
                        resourceId)) {

            throw new ConflictException(
                    "Provider service location is already saved");
        }

        ProviderServiceLocation serviceLocation =
                providerServiceLocationRepository
                        .findPubliclyDiscoverableServiceLocations(
                                VerificationStatus.VERIFIED,
                                freshnessCutoff(DIRECTORY_FRESHNESS_DAYS),
                                LocalDate.now())
                        .stream()
                        .filter(candidate ->
                                candidate.getId().equals(resourceId))
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider service location is not currently available for saving"));

        savedResource.setProviderServiceLocation(serviceLocation);
    }

    private SavedResourceResponse toResponse(
            SavedResource savedResource) {

        SavedResourceType resourceType;
        UUID resourceId;

        if (savedResource.getAtCategory() != null) {
            resourceType = SavedResourceType.AT_CATEGORY;
            resourceId = savedResource.getAtCategory().getId();

        } else if (savedResource.getAtProduct() != null) {
            resourceType = SavedResourceType.AT_PRODUCT;
            resourceId = savedResource.getAtProduct().getId();

        } else if (savedResource.getScheme() != null) {
            resourceType = SavedResourceType.SCHEME;
            resourceId = savedResource.getScheme().getId();

        } else if (savedResource.getProvider() != null) {
            resourceType = SavedResourceType.PROVIDER;
            resourceId = savedResource.getProvider().getId();

        } else if (savedResource.getProviderLocation() != null) {
            resourceType = SavedResourceType.PROVIDER_LOCATION;
            resourceId = savedResource.getProviderLocation().getId();

        } else if (savedResource.getProviderServiceLocation() != null) {
            resourceType = SavedResourceType.PROVIDER_SERVICE_LOCATION;
            resourceId = savedResource
                    .getProviderServiceLocation()
                    .getId();

        } else {
            throw new IllegalStateException(
                    "Saved resource has no resource target");
        }

        return new SavedResourceResponse(
                savedResource.getId(),
                resourceType,
                resourceId,
                savedResource.getCreatedAt());
    }

    private User requireUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));
    }

    private LocalDate freshnessCutoff(int days) {
        return LocalDate.now().minusDays(days);
    }
}