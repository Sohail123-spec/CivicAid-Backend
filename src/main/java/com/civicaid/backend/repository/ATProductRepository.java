package com.civicaid.backend.repository;

import com.civicaid.backend.entity.ATProduct;
import com.civicaid.backend.entity.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ATProductRepository extends JpaRepository<ATProduct, UUID> {

    Optional<ATProduct> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndAtCategory_Id(String name, UUID categoryId);

    boolean existsByNameIgnoreCaseAndAtCategory_IdAndIdNot(
            String name, UUID categoryId, UUID productId);

    List<ATProduct>
            findByAtCategory_IdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqualOrderByDisplayOrderAscNameAsc(
                    UUID categoryId,
                    VerificationStatus verificationStatus,
                    LocalDate lastVerifiedAt);
        List<ATProduct>
        findByActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqualOrderByDisplayOrderAscNameAsc(
                VerificationStatus verificationStatus,
                LocalDate lastVerifiedAt);

        Optional<ATProduct>
        findByIdAndActiveTrueAndVerificationStatusAndLastVerifiedAtGreaterThanEqual(
                UUID id,
                VerificationStatus verificationStatus,
                LocalDate lastVerifiedAt);
}