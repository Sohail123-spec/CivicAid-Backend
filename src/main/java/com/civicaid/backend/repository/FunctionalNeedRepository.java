package com.civicaid.backend.repository;

import com.civicaid.backend.entity.FunctionalNeed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunctionalNeedRepository extends JpaRepository<FunctionalNeed, UUID> {

    Optional<FunctionalNeed> findByCodeIgnoreCase(String code);

    List<FunctionalNeed> findAllByCodeInIgnoreCase(List<String> codes);

    List<FunctionalNeed> findByActiveTrueAndSelectableTrueOrderByDisplayOrderAsc();
}
