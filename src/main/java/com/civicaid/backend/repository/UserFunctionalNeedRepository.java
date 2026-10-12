package com.civicaid.backend.repository;

import com.civicaid.backend.entity.UserFunctionalNeed;
import com.civicaid.backend.entity.UserFunctionalNeedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFunctionalNeedRepository
        extends JpaRepository<UserFunctionalNeed, UserFunctionalNeedId> {

    @EntityGraph(attributePaths = "functionalNeed")
    List<UserFunctionalNeed> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}