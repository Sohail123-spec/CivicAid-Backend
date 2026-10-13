package com.civicaid.backend.repository;

import com.civicaid.backend.entity.SchemeCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeCategoryRepository extends JpaRepository<SchemeCategory, UUID> {

    List<SchemeCategory> findByScheme_IdAndActiveTrue(UUID schemeId);

    List<SchemeCategory> findByScheme_Id(UUID schemeId);

    Optional<SchemeCategory> findByScheme_IdAndAtCategory_Id(
            UUID schemeId, UUID atCategoryId);

    boolean existsByScheme_IdAndAtCategory_Id(
            UUID schemeId, UUID atCategoryId);
}
