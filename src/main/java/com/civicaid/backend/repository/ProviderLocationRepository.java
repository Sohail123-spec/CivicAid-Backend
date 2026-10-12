
package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ProviderLocation;
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
public interface ProviderLocationRepository
        extends JpaRepository<ProviderLocation, UUID> {

    List<ProviderLocation> findByProvider_IdOrderByCityAscLocalityAsc(
            UUID providerId);

    List<ProviderLocation> findAllByOrderByCityAscLocalityAsc();

    Optional<ProviderLocation> findByIdAndProvider_Id(
            UUID locationId, UUID providerId);

    boolean existsByProvider_IdAndId(
            UUID providerId, UUID locationId);

    /**
     * Finds locations eligible for public discovery.
     *
     * Both the location and its parent provider must be active and verified.
     * Verification dates must fall within the supplied freshness window.
     */
    @Query("""
            SELECT pl
            FROM ProviderLocation pl
            JOIN pl.provider p
            WHERE pl.active = true
              AND p.active = true
              AND pl.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND pl.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND pl.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND pl.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
            ORDER BY pl.city ASC, pl.locality ASC
            """)
    List<ProviderLocation> findPubliclyDiscoverableLocations(
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today);

    /**
     * Finds publicly discoverable locations belonging to one provider.
     */
    @Query("""
            SELECT pl
            FROM ProviderLocation pl
            JOIN pl.provider p
            WHERE p.id = :providerId
              AND pl.active = true
              AND p.active = true
              AND pl.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND pl.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND pl.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND pl.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
            ORDER BY pl.city ASC, pl.locality ASC
            """)
    List<ProviderLocation> findPubliclyDiscoverableLocationsByProvider(
            @Param("providerId") UUID providerId,
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today);
}
