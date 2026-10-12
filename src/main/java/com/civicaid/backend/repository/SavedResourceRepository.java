package com.civicaid.backend.repository;

import com.civicaid.backend.entity.SavedResource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedResourceRepository
        extends JpaRepository<SavedResource, UUID> {

    List<SavedResource> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<SavedResource> findByIdAndUser_Id(UUID savedResourceId, UUID userId);

    boolean existsByUser_IdAndAtCategory_Id(
            UUID userId,
            UUID atCategoryId);

    boolean existsByUser_IdAndAtProduct_Id(
            UUID userId,
            UUID atProductId);

    boolean existsByUser_IdAndScheme_Id(
            UUID userId,
            UUID schemeId);

    boolean existsByUser_IdAndProvider_Id(
            UUID userId,
            UUID providerId);

    boolean existsByUser_IdAndProviderLocation_Id(
            UUID userId,
            UUID providerLocationId);

    boolean existsByUser_IdAndProviderServiceLocation_Id(
            UUID userId,
            UUID providerServiceLocationId);
}