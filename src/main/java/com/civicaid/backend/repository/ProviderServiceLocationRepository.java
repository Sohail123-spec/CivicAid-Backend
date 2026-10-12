
package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ProviderServiceLocation;
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
public interface ProviderServiceLocationRepository
        extends JpaRepository<ProviderServiceLocation, UUID> {

    List<ProviderServiceLocation>
            findByProviderService_IdOrderByProviderLocation_CityAsc(
                    UUID providerServiceId);

    List<ProviderServiceLocation>
            findByProviderLocation_IdOrderByProviderService_NameAsc(
                    UUID providerLocationId);

    List<ProviderServiceLocation>
            findAllByOrderByProviderLocation_CityAsc();

    Optional<ProviderServiceLocation>
            findByProviderService_IdAndProviderLocation_Id(
                    UUID providerServiceId,
                    UUID providerLocationId);

    boolean existsByProviderService_IdAndProviderLocation_Id(
            UUID providerServiceId,
            UUID providerLocationId);

    @Query("""
            SELECT psl
            FROM ProviderServiceLocation psl
            JOIN psl.providerService ps
            JOIN ps.provider p
            JOIN psl.providerLocation pl
            WHERE psl.active = true
              AND ps.active = true
              AND p.active = true
              AND pl.active = true
              AND psl.verificationStatus = :verifiedStatus
              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND pl.verificationStatus = :verifiedStatus
              AND psl.lastVerifiedAt IS NOT NULL
              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND pl.lastVerifiedAt IS NOT NULL
              AND psl.lastVerifiedAt >= :freshnessCutoff
              AND ps.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND pl.lastVerifiedAt >= :freshnessCutoff
              AND psl.lastVerifiedAt <= :today
              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND pl.lastVerifiedAt <= :today
            ORDER BY pl.city ASC, pl.locality ASC, ps.name ASC
            """)
    List<ProviderServiceLocation> findPubliclyDiscoverableServiceLocations(
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today);

    @Query("""
            SELECT psl
            FROM ProviderServiceLocation psl
            JOIN psl.providerService ps
            JOIN ps.provider p
            JOIN psl.providerLocation pl
            WHERE p.id = :providerId
              AND psl.active = true
              AND ps.active = true
              AND p.active = true
              AND pl.active = true
              AND psl.verificationStatus = :verifiedStatus
              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND pl.verificationStatus = :verifiedStatus
              AND psl.lastVerifiedAt IS NOT NULL
              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND pl.lastVerifiedAt IS NOT NULL
              AND psl.lastVerifiedAt >= :freshnessCutoff
              AND ps.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND pl.lastVerifiedAt >= :freshnessCutoff
              AND psl.lastVerifiedAt <= :today
              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND pl.lastVerifiedAt <= :today
            ORDER BY pl.city ASC, pl.locality ASC, ps.name ASC
            """)
    List<ProviderServiceLocation>
            findPubliclyDiscoverableServiceLocationsByProvider(
                    @Param("providerId") UUID providerId,
                    @Param("verifiedStatus") VerificationStatus verifiedStatus,
                    @Param("freshnessCutoff") LocalDate freshnessCutoff,
                    @Param("today") LocalDate today);

    @Query("""
            SELECT psl
            FROM ProviderServiceLocation psl
            JOIN psl.providerService ps
            JOIN ps.provider p
            JOIN psl.providerLocation pl
            WHERE pl.id = :locationId
              AND psl.active = true
              AND ps.active = true
              AND p.active = true
              AND pl.active = true
              AND psl.verificationStatus = :verifiedStatus
              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND pl.verificationStatus = :verifiedStatus
              AND psl.lastVerifiedAt IS NOT NULL
              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND pl.lastVerifiedAt IS NOT NULL
              AND psl.lastVerifiedAt >= :freshnessCutoff
              AND ps.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND pl.lastVerifiedAt >= :freshnessCutoff
              AND psl.lastVerifiedAt <= :today
              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND pl.lastVerifiedAt <= :today
            ORDER BY ps.name ASC
            """)
    List<ProviderServiceLocation>
            findPubliclyDiscoverableServiceLocationsByLocation(
                    @Param("locationId") UUID locationId,
                    @Param("verifiedStatus") VerificationStatus verifiedStatus,
                    @Param("freshnessCutoff") LocalDate freshnessCutoff,
                    @Param("today") LocalDate today);
}
