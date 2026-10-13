package com.civicaid.backend.repository;

import com.civicaid.backend.entity.SchemeProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeProductRepository extends JpaRepository<SchemeProduct, UUID> {

    List<SchemeProduct> findByScheme_IdAndActiveTrue(UUID schemeId);

    List<SchemeProduct> findByScheme_Id(UUID schemeId);

    Optional<SchemeProduct> findByScheme_IdAndAtProduct_Id(
            UUID schemeId, UUID atProductId);

    boolean existsByScheme_IdAndAtProduct_Id(
            UUID schemeId, UUID atProductId);
}
