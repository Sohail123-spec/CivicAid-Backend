
package com.civicaid.backend.repository;

import com.civicaid.backend.entity.Provider;
import com.civicaid.backend.entity.ProviderType;
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
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Provider> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    List<Provider> findAllByOrderByNameAsc();

    List<Provider> findByProviderTypeOrderByNameAsc(ProviderType providerType);

    /**
     * Returns providers eligible for public discovery.
     *
     * A provider must be active, verified, have a verification date,
     * and have been verified within the configured freshness window.
     * Future verification dates are excluded as a defensive check.
     */
    @Query("""
            SELECT p
            FROM Provider p
            WHERE p.active = true
              AND p.verificationStatus = :verifiedStatus
              AND p.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt <= :today
            ORDER BY p.name ASC
            """)
    List<Provider> findPubliclyDiscoverableProviders(
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today);

    /**
     * Returns publicly discoverable providers of a specific type.
     */
    @Query("""
            SELECT p
            FROM Provider p
            WHERE p.active = true
              AND p.verificationStatus = :verifiedStatus
              AND p.lastVerifiedAt IS NOT NULL
              AND p.lastVerifiedAt >= :freshnessCutoff
              AND p.lastVerifiedAt <= :today
              AND p.providerType = :providerType
            ORDER BY p.name ASC
            """)
    List<Provider> findPubliclyDiscoverableProvidersByType(
            @Param("verifiedStatus") VerificationStatus verifiedStatus,
            @Param("freshnessCutoff") LocalDate freshnessCutoff,
            @Param("today") LocalDate today,
            @Param("providerType") ProviderType providerType);
}
