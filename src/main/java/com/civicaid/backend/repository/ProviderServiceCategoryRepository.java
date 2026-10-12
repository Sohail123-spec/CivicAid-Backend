package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ProviderServiceCategory;
import com.civicaid.backend.entity.VerificationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderServiceCategoryRepository
        extends JpaRepository<ProviderServiceCategory, UUID> {

    List<ProviderServiceCategory>
            findByProviderService_IdOrderByAtCategory_NameAsc(
                    UUID providerServiceId);

    List<ProviderServiceCategory>
            findByAtCategory_IdOrderByProviderService_NameAsc(
                    UUID categoryId);

    Optional<ProviderServiceCategory>
            findByProviderService_IdAndAtCategory_Id(
                    UUID providerServiceId,
                    UUID categoryId);

    boolean existsByProviderService_IdAndAtCategory_Id(
            UUID providerServiceId,
            UUID categoryId);


    /**
     * Returns provider-service/category mappings that are eligible
     * for authoritative public discovery.
     *
     * Public eligibility requires:
     *
     * - mapping is active
     * - provider service is active
     * - provider is active
     * - AT category is active
     * - provider service is VERIFIED
     * - provider is VERIFIED
     * - AT category is VERIFIED
     * - provider service verification is fresh
     * - provider verification is fresh
     * - AT category verification is fresh
     * - no verification date is future-dated
     *
     * Provider and provider-service freshness are supplied separately
     * from AT-category freshness because their cadences differ:
     *
     * - Provider / ProviderService: 90 days
     * - ATCategory: 180 days
     */
    @Query("""
            SELECT psc
            FROM ProviderServiceCategory psc
            JOIN psc.providerService ps
            JOIN ps.provider p
            JOIN psc.atCategory c
            WHERE psc.active = true
              AND ps.active = true
              AND p.active = true
              AND c.active = true

              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND c.verificationStatus = :verifiedStatus

              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND c.lastVerifiedAt IS NOT NULL

              AND ps.lastVerifiedAt >= :providerServiceFreshnessCutoff
              AND p.lastVerifiedAt >= :providerServiceFreshnessCutoff
              AND c.lastVerifiedAt >= :categoryFreshnessCutoff

              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND c.lastVerifiedAt <= :today

            ORDER BY c.name ASC, ps.name ASC
            """)
    List<ProviderServiceCategory>
            findPubliclyDiscoverableCategoryMappings(
                    @Param("verifiedStatus")
                    VerificationStatus verifiedStatus,

                    @Param("providerServiceFreshnessCutoff")
                    LocalDate providerServiceFreshnessCutoff,

                    @Param("categoryFreshnessCutoff")
                    LocalDate categoryFreshnessCutoff,

                    @Param("today")
                    LocalDate today);


    /**
     * Returns publicly discoverable mappings for one AT category.
     *
     * The same public eligibility rules apply as the general
     * discovery query.
     */
    @Query("""
            SELECT psc
            FROM ProviderServiceCategory psc
            JOIN psc.providerService ps
            JOIN ps.provider p
            JOIN psc.atCategory c
            WHERE psc.active = true
              AND ps.active = true
              AND p.active = true
              AND c.active = true

              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND c.verificationStatus = :verifiedStatus

              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND c.lastVerifiedAt IS NOT NULL

              AND ps.lastVerifiedAt >= :providerServiceFreshnessCutoff
              AND p.lastVerifiedAt >= :providerServiceFreshnessCutoff
              AND c.lastVerifiedAt >= :categoryFreshnessCutoff

              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND c.lastVerifiedAt <= :today

              AND c.id = :categoryId

            ORDER BY ps.name ASC
            """)
    List<ProviderServiceCategory>
            findPubliclyDiscoverableCategoryMappingsByCategory(
                    @Param("categoryId")
                    UUID categoryId,

                    @Param("verifiedStatus")
                    VerificationStatus verifiedStatus,

                    @Param("providerServiceFreshnessCutoff")
                    LocalDate providerServiceFreshnessCutoff,

                    @Param("categoryFreshnessCutoff")
                    LocalDate categoryFreshnessCutoff,

                    @Param("today")
                    LocalDate today);
}