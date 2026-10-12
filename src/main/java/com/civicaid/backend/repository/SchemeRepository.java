
package com.civicaid.backend.repository;

import com.civicaid.backend.entity.JurisdictionLevel;
import com.civicaid.backend.entity.Scheme;
import com.civicaid.backend.entity.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchemeRepository extends JpaRepository<Scheme, UUID> {

    boolean existsByCodeIgnoreCase(String code);
    
    boolean existsByNameIgnoreCaseAndJurisdictionLevelAndApplicableStateCode(
            String name,
            JurisdictionLevel jurisdictionLevel,
            String applicableStateCode);
    Optional<Scheme> findByCodeIgnoreCase(String code);

    @Query("""
            SELECT s
            FROM Scheme s
            WHERE s.active = true
              AND s.verificationStatus = :verified
              AND s.lastVerifiedAt IS NOT NULL
              AND s.lastVerifiedAt >= :cutoff
              AND s.lastVerifiedAt <= :today
              AND (s.effectiveFrom IS NULL OR s.effectiveFrom <= :today)
              AND (s.effectiveTo IS NULL OR s.effectiveTo >= :today)
              AND (
                    s.jurisdictionLevel = :national
                    OR (
                        s.jurisdictionLevel = :state
                        AND s.applicableStateCode = :stateCode
                    )
              )
            ORDER BY s.name ASC
            """)
    List<Scheme> findPublicSchemesForState(
            @Param("verified") VerificationStatus verified,
            @Param("cutoff") LocalDate cutoff,
            @Param("today") LocalDate today,
            @Param("national") JurisdictionLevel national,
            @Param("state") JurisdictionLevel state,
            @Param("stateCode") String stateCode);

    @Query("""
            SELECT s
            FROM Scheme s
            WHERE s.active = true
              AND s.verificationStatus = :verified
              AND s.lastVerifiedAt IS NOT NULL
              AND s.lastVerifiedAt >= :cutoff
              AND s.lastVerifiedAt <= :today
              AND (s.effectiveFrom IS NULL OR s.effectiveFrom <= :today)
              AND (s.effectiveTo IS NULL OR s.effectiveTo >= :today)
              AND s.jurisdictionLevel = :national
            ORDER BY s.name ASC
            """)
    List<Scheme> findPublicNationalSchemes(
            @Param("verified") VerificationStatus verified,
            @Param("cutoff") LocalDate cutoff,
            @Param("today") LocalDate today,
            @Param("national") JurisdictionLevel national);
}