package com.civicaid.backend.repository;

import com.civicaid.backend.entity.NeedCategoryMapping;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NeedCategoryMappingRepository
        extends JpaRepository<NeedCategoryMapping, UUID> {

    List<NeedCategoryMapping>
            findByFunctionalNeed_IdOrderByAtCategory_NameAsc(UUID functionalNeedId);

    List<NeedCategoryMapping>
            findByAtCategory_IdOrderByFunctionalNeed_LabelAsc(UUID atCategoryId);

    Optional<NeedCategoryMapping>
            findByFunctionalNeed_IdAndAtCategory_Id(
                    UUID functionalNeedId,
                    UUID atCategoryId);

    boolean existsByFunctionalNeed_IdAndAtCategory_Id(
            UUID functionalNeedId,
            UUID atCategoryId);

    @Query("""
            SELECT mapping
            FROM NeedCategoryMapping mapping
            JOIN FETCH mapping.functionalNeed functionalNeed
            JOIN FETCH mapping.atCategory category
            WHERE mapping.active = true
              AND functionalNeed.active = true
              AND functionalNeed.selectable = true
              AND category.active = true
              AND category.verificationStatus = com.civicaid.backend.entity.VerificationStatus.VERIFIED
              AND category.lastVerifiedAt IS NOT NULL
              AND category.lastVerifiedAt >= :categoryFreshnessCutoff
              AND category.lastVerifiedAt <= CURRENT_DATE
            ORDER BY functionalNeed.displayOrder ASC,
                     functionalNeed.label ASC,
                     category.displayOrder ASC,
                     category.name ASC
            """)
    List<NeedCategoryMapping> findPublicEligibleMappings(
            @Param("categoryFreshnessCutoff") LocalDate categoryFreshnessCutoff);

    @Query("""
            SELECT mapping
            FROM NeedCategoryMapping mapping
            JOIN FETCH mapping.functionalNeed functionalNeed
            JOIN FETCH mapping.atCategory category
            WHERE mapping.active = true
              AND functionalNeed.id = :functionalNeedId
              AND functionalNeed.active = true
              AND functionalNeed.selectable = true
              AND category.active = true
              AND category.verificationStatus = com.civicaid.backend.entity.VerificationStatus.VERIFIED
              AND category.lastVerifiedAt IS NOT NULL
              AND category.lastVerifiedAt >= :categoryFreshnessCutoff
              AND category.lastVerifiedAt <= CURRENT_DATE
            ORDER BY category.displayOrder ASC,
                     category.name ASC
            """)
    List<NeedCategoryMapping> findPublicEligibleMappingsByFunctionalNeed(
            @Param("functionalNeedId") UUID functionalNeedId,
            @Param("categoryFreshnessCutoff") LocalDate categoryFreshnessCutoff);

    @Query("""
            SELECT mapping
            FROM NeedCategoryMapping mapping
            JOIN FETCH mapping.functionalNeed functionalNeed
            JOIN FETCH mapping.atCategory category
            WHERE mapping.active = true
              AND category.id = :atCategoryId
              AND functionalNeed.active = true
              AND functionalNeed.selectable = true
              AND category.active = true
              AND category.verificationStatus = com.civicaid.backend.entity.VerificationStatus.VERIFIED
              AND category.lastVerifiedAt IS NOT NULL
              AND category.lastVerifiedAt >= :categoryFreshnessCutoff
              AND category.lastVerifiedAt <= CURRENT_DATE
            ORDER BY functionalNeed.displayOrder ASC,
                     functionalNeed.label ASC
            """)
    List<NeedCategoryMapping> findPublicEligibleMappingsByCategory(
            @Param("atCategoryId") UUID atCategoryId,
            @Param("categoryFreshnessCutoff") LocalDate categoryFreshnessCutoff);
}