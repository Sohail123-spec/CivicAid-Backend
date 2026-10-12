
package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ProviderService;
import com.civicaid.backend.entity.ProviderServiceType;
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
public interface ProviderServiceRepository
        extends JpaRepository<ProviderService, UUID> {

    boolean existsByProvider_IdAndCodeIgnoreCase(
            UUID providerId, String code);

    boolean existsByProvider_IdAndNameIgnoreCase(
            UUID providerId, String name);

    Optional<ProviderService> findByProvider_IdAndCodeIgnoreCase(
            UUID providerId, String code);

    List<ProviderService> findByProvider_IdOrderByNameAsc(
            UUID providerId);

    List<ProviderService> findAllByOrderByNameAsc();

    List<ProviderService> findByServiceTypeOrderByNameAsc(
            ProviderServiceType serviceType);

    /**
     * Returns services eligible for public discovery.
     *
     * Both the service and its parent provider must be active and verified.
     * Both verification dates must be within the supplied freshness window.
     */
    @Query("""
            SELECT ps
            FROM ProviderService ps
            JOIN ps.provider p
            WHERE ps.active = true
              AND p.active = true
              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND ps.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
            ORDER BY ps.name ASC
            """)
    List<ProviderService> findPubliclyDiscoverableServices(
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today);

    /**
     * Returns publicly discoverable services of a specific type.
     */
    @Query("""
            SELECT ps
            FROM ProviderService ps
            JOIN ps.provider p
            WHERE ps.active = true
              AND p.active = true
              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND ps.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
              AND ps.serviceType = :serviceType
            ORDER BY ps.name ASC
            """)
    List<ProviderService> findPubliclyDiscoverableServicesByType(
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today,
            @Param("serviceType") ProviderServiceType serviceType);

    /**
     * Returns publicly discoverable services belonging to one provider.
     */
    @Query("""
            SELECT ps
            FROM ProviderService ps
            JOIN ps.provider p
            WHERE p.id = :providerId
              AND ps.active = true
              AND p.active = true
              AND ps.verificationStatus = :verifiedStatus
              AND p.verificationStatus = :verifiedStatus
              AND ps.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt IS NOT NULL
              AND ps.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND ps.lastVerifiedAt <= :today
              AND p.lastVerifiedAt <= :today
            ORDER BY ps.name ASC
            """)
    List<ProviderService> findPubliclyDiscoverableServicesByProvider(
            @Param("providerId") UUID providerId,
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today);
}
