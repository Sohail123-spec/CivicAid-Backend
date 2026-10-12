package com.civicaid.backend.service;

import com.civicaid.backend.dto.ProviderServiceProductCreateRequest;
import com.civicaid.backend.dto.ProviderServiceProductResponse;
import com.civicaid.backend.dto.ProviderServiceProductUpdateRequest;
import com.civicaid.backend.entity.ATProduct;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceCategory;
import com.civicaid.backend.entity.ProviderServiceProduct;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.ATProductRepository;
import com.civicaid.backend.repository.ProviderServiceCategoryRepository;
import com.civicaid.backend.repository.ProviderServiceProductRepository;
import com.civicaid.backend.repository.ProviderServiceRepository;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderServiceProductService {

    private static final int PROVIDER_VERIFICATION_FRESHNESS_DAYS = 90;

    private static final int PROVIDER_SERVICE_VERIFICATION_FRESHNESS_DAYS = 90;

    private static final int AT_PRODUCT_VERIFICATION_FRESHNESS_DAYS = 180;

    private static final int AT_CATEGORY_VERIFICATION_FRESHNESS_DAYS = 180;

    private final ProviderServiceProductRepository
            providerServiceProductRepository;

    private final ProviderServiceRepository providerServiceRepository;

    private final ATProductRepository atProductRepository;

    private final ProviderServiceCategoryRepository
            providerServiceCategoryRepository;

    private final UserRepository userRepository;

    private final AuditLogService auditLogService;

    public ProviderServiceProductService(
            ProviderServiceProductRepository providerServiceProductRepository,
            ProviderServiceRepository providerServiceRepository,
            ATProductRepository atProductRepository,
            ProviderServiceCategoryRepository providerServiceCategoryRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.providerServiceProductRepository =
                providerServiceProductRepository;

        this.providerServiceRepository =
                providerServiceRepository;

        this.atProductRepository =
                atProductRepository;

        this.providerServiceCategoryRepository =
                providerServiceCategoryRepository;

        this.userRepository = userRepository;

        this.auditLogService = auditLogService;
    }

    /*
     * -------------------------------------------------------------------------
     * PUBLIC DISCOVERY
     * -------------------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<ProviderServiceProductResponse> getPublicMappings() {

        return providerServiceProductRepository
                .findPubliclyDiscoverableProductMappings(
                        VerificationStatus.VERIFIED,
                        providerServiceFreshnessCutoff(),
                        providerFreshnessCutoff(),
                        productFreshnessCutoff(),
                        categoryFreshnessCutoff(),
                        LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceProductResponse> getPublicMappingsByProduct(
            UUID productId) {

        return providerServiceProductRepository
                .findPubliclyDiscoverableProductMappingsByProduct(
                        productId,
                        VerificationStatus.VERIFIED,
                        providerServiceFreshnessCutoff(),
                        providerFreshnessCutoff(),
                        productFreshnessCutoff(),
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
    public List<ProviderServiceProductResponse> getAdminMappings() {

        return providerServiceProductRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceProductResponse> getAdminMappingsByService(
            UUID providerServiceId) {

        if (!providerServiceRepository.existsById(providerServiceId)) {
            throw new ResourceNotFoundException(
                    "Provider service not found");
        }

        return providerServiceProductRepository
                .findByProviderService_IdOrderByAtProduct_NameAsc(
                        providerServiceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceProductResponse> getAdminMappingsByProduct(
            UUID productId) {

        if (!atProductRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "AT product not found");
        }

        return providerServiceProductRepository
                .findByAtProduct_IdOrderByProviderService_NameAsc(
                        productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderServiceProductResponse getAdminMapping(
            UUID mappingId) {

        return toResponse(requireMapping(mappingId));
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN CREATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceProductResponse createMapping(
            UUID providerServiceId,
            ProviderServiceProductCreateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderService providerService =
                providerServiceRepository.findById(providerServiceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider service not found"));

        ATProduct atProduct =
                atProductRepository.findById(request.atProductId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "AT product not found"));

        if (providerServiceProductRepository
                .existsByProviderService_IdAndAtProduct_Id(
                        providerServiceId,
                        request.atProductId())) {

            throw new ConflictException(
                    "Provider service product mapping already exists");
        }

        UUID categoryId =
                atProduct.getAtCategory().getId();

        ProviderServiceCategory categoryMapping =
                providerServiceCategoryRepository
                        .findByProviderService_IdAndAtCategory_Id(
                                providerServiceId,
                                categoryId)
                        .orElseThrow(() ->
                                new ConflictException(
                                        "An active provider service category mapping is required before creating a product mapping"));

        if (!categoryMapping.isActive()) {
            throw new ConflictException(
                    "An active provider service category mapping is required before creating a product mapping");
        }

        ProviderServiceProduct mapping =
                new ProviderServiceProduct();

        mapping.setProviderService(providerService);
        mapping.setAtProduct(atProduct);
        mapping.setRationale(request.rationale().trim());
        mapping.setActive(true);

        mapping =
                providerServiceProductRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_PRODUCT,
                mapping.getId(),
                AuditActionType.CREATE,
                new String[] {
                    "provider_service_id",
                    "at_product_id",
                    "rationale",
                    "active"
                },
                "Provider service product mapping created");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN UPDATE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceProductResponse updateMapping(
            UUID mappingId,
            ProviderServiceProductUpdateRequest request,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderServiceProduct mapping =
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
                providerServiceProductRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_PRODUCT,
                mapping.getId(),
                AuditActionType.UPDATE,
                new String[] {"rationale"},
                "Provider service product mapping updated");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * ADMIN ACTIVATE / ARCHIVE
     * -------------------------------------------------------------------------
     */

    @Transactional
    public ProviderServiceProductResponse setMappingActive(
            UUID mappingId,
            boolean active,
            String actorEmail) {

        User actor = requireActor(actorEmail);

        ProviderServiceProduct mapping =
                requireMapping(mappingId);

        if (mapping.isActive() == active) {
            return toResponse(mapping);
        }

        /*
         * A product-specific mapping must continue to have
         * an active category mapping for its product's parent category.
         */
        if (active) {

            UUID categoryId =
                    mapping.getAtProduct()
                            .getAtCategory()
                            .getId();

            ProviderServiceCategory categoryMapping =
                    providerServiceCategoryRepository
                            .findByProviderService_IdAndAtCategory_Id(
                                    mapping.getProviderService().getId(),
                                    categoryId)
                            .orElseThrow(() ->
                                    new ConflictException(
                                            "An active provider service category mapping is required before activating a product mapping"));

            if (!categoryMapping.isActive()) {
                throw new ConflictException(
                        "An active provider service category mapping is required before activating a product mapping");
            }
        }

        mapping.setActive(active);

        mapping =
                providerServiceProductRepository.save(mapping);

        auditLogService.record(
                actor,
                AuditTargetType.PROVIDER_SERVICE_PRODUCT,
                mapping.getId(),
                active
                        ? AuditActionType.ACTIVATE
                        : AuditActionType.ARCHIVE,
                new String[] {"active"},
                active
                        ? "Provider service product mapping activated"
                        : "Provider service product mapping archived");

        return toResponse(mapping);
    }

    /*
     * -------------------------------------------------------------------------
     * HELPERS
     * -------------------------------------------------------------------------
     */

    private ProviderServiceProduct requireMapping(
            UUID mappingId) {

        return providerServiceProductRepository
                .findById(mappingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider service product mapping not found"));
    }

    private User requireActor(String actorEmail) {

        return userRepository
                .findByEmail(actorEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    private LocalDate providerFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        PROVIDER_VERIFICATION_FRESHNESS_DAYS);
    }

    private LocalDate providerServiceFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        PROVIDER_SERVICE_VERIFICATION_FRESHNESS_DAYS);
    }

    private LocalDate productFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        AT_PRODUCT_VERIFICATION_FRESHNESS_DAYS);
    }

    private LocalDate categoryFreshnessCutoff() {

        return LocalDate.now()
                .minusDays(
                        AT_CATEGORY_VERIFICATION_FRESHNESS_DAYS);
    }

    private ProviderServiceProductResponse toResponse(
            ProviderServiceProduct mapping) {

        ProviderService providerService =
                mapping.getProviderService();

        ATProduct atProduct =
                mapping.getAtProduct();

        return new ProviderServiceProductResponse(
                mapping.getId(),
                providerService.getProvider().getId(),
                providerService.getProvider().getName(),
                providerService.getId(),
                providerService.getCode(),
                providerService.getName(),
                atProduct.getId(),
                atProduct.getCode(),
                atProduct.getName(),
                atProduct.getAtCategory().getId(),
                atProduct.getAtCategory().getName(),
                mapping.getRationale(),
                mapping.isActive(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt());
    }
}