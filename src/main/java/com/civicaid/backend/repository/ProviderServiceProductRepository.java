package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ProviderServiceProduct;
import com.civicaid.backend.entity.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderServiceProductRepository
        extends JpaRepository<ProviderServiceProduct, UUID> {

    boolean existsByProviderService_IdAndAtProduct_Id(
            UUID providerServiceId,
            UUID atProductId);

    Optional<ProviderServiceProduct>
            findByProviderService_IdAndAtProduct_Id(
                    UUID providerServiceId,
                    UUID atProductId);

    List<ProviderServiceProduct>
            findByProviderService_IdOrderByAtProduct_NameAsc(
                    UUID providerServiceId);

    List<ProviderServiceProduct>
            findByAtProduct_IdOrderByProviderService_NameAsc(
                    UUID atProductId);

    @Query("""
            SELECT psp
            FROM ProviderServiceProduct psp
            JOIN psp.providerService ps
            JOIN ps.provider p
            JOIN psp.atProduct product
            JOIN product.atCategory category
            WHERE psp.active = true
              AND ps.active = true
              AND p.active = true
              AND product.active = true
              AND category.active = true

              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND product.verificationStatus = :verifiedStatus
              AND category.verificationStatus = :verifiedStatus

              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND product.lastVerifiedAt IS NOT NULL
              AND category.lastVerifiedAt IS NOT NULL

              AND ps.lastVerifiedAt >= :providerServiceFreshnessCutoff
              AND p.lastVerifiedAt >= :providerFreshnessCutoff
              AND product.lastVerifiedAt >= :productFreshnessCutoff
              AND category.lastVerifiedAt >= :categoryFreshnessCutoff

              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND product.lastVerifiedAt <= :today
              AND category.lastVerifiedAt <= :today

              AND EXISTS (
                    SELECT 1
                    FROM ProviderServiceCategory psc
                    WHERE psc.providerService.id = ps.id
                      AND psc.atCategory.id = category.id
                      AND psc.active = true
              )

            ORDER BY product.name ASC, ps.name ASC
            """)
    List<ProviderServiceProduct>
            findPubliclyDiscoverableProductMappings(
                    @Param("verifiedStatus")
                    VerificationStatus verifiedStatus,
                    @Param("providerServiceFreshnessCutoff")
                    LocalDate providerServiceFreshnessCutoff,
                    @Param("providerFreshnessCutoff")
                    LocalDate providerFreshnessCutoff,
                    @Param("productFreshnessCutoff")
                    LocalDate productFreshnessCutoff,
                    @Param("categoryFreshnessCutoff")
                    LocalDate categoryFreshnessCutoff,
                    @Param("today")
                    LocalDate today);

    @Query("""
            SELECT psp
            FROM ProviderServiceProduct psp
            JOIN psp.providerService ps
            JOIN ps.provider p
            JOIN psp.atProduct product
            JOIN product.atCategory category
            WHERE psp.active = true
              AND ps.active = true
              AND p.active = true
              AND product.active = true
              AND category.active = true

              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND product.verificationStatus = :verifiedStatus
              AND category.verificationStatus = :verifiedStatus

              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND product.lastVerifiedAt IS NOT NULL
              AND category.lastVerifiedAt IS NOT NULL

              AND ps.lastVerifiedAt >= :providerServiceFreshnessCutoff
              AND p.lastVerifiedAt >= :providerFreshnessCutoff
              AND product.lastVerifiedAt >= :productFreshnessCutoff
              AND category.lastVerifiedAt >= :categoryFreshnessCutoff

              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND product.lastVerifiedAt <= :today
              AND category.lastVerifiedAt <= :today

              AND product.id = :productId

              AND EXISTS (
                    SELECT 1
                    FROM ProviderServiceCategory psc
                    WHERE psc.providerService.id = ps.id
                      AND psc.atCategory.id = category.id
                      AND psc.active = true
              )

            ORDER BY ps.name ASC
            """)
    List<ProviderServiceProduct>
            findPubliclyDiscoverableProductMappingsByProduct(
                    @Param("productId")
                    UUID productId,
                    @Param("verifiedStatus")
                    VerificationStatus verifiedStatus,
                    @Param("providerServiceFreshnessCutoff")
                    LocalDate providerServiceFreshnessCutoff,
                    @Param("providerFreshnessCutoff")
                    LocalDate providerFreshnessCutoff,
                    @Param("productFreshnessCutoff")
                    LocalDate productFreshnessCutoff,
                    @Param("categoryFreshnessCutoff")
                    LocalDate categoryFreshnessCutoff,
                    @Param("today")
                    LocalDate today);
}