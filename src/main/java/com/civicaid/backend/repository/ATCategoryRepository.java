package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ATCategory;
import com.civicaid.backend.entity.VerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ATCategoryRepository
        extends JpaRepository<ATCategory, UUID> {

    Optional<ATCategory> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    List<ATCategory> findByActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqualOrderByDisplayOrderAscNameAsc(
            VerificationStatus verificationStatus,
            java.time.LocalDate lastVerifiedAt);

    Optional<ATCategory> findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
            UUID id,
            VerificationStatus verificationStatus,
            java.time.LocalDate lastVerifiedAt);
}
