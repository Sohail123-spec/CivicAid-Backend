package com.civicaid.backend.repository;

import com.civicaid.backend.entity.VerificationRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRecordRepository
        extends JpaRepository<VerificationRecord, UUID> {

    List<VerificationRecord> findByAtCategory_IdOrderByPerformedAtDesc(
            UUID categoryId);

    List<VerificationRecord> findByAtProduct_IdOrderByPerformedAtDesc(
            UUID productId);

    List<VerificationRecord> findByScheme_IdOrderByPerformedAtDesc(
            UUID schemeId);

    List<VerificationRecord> findByProvider_IdOrderByPerformedAtDesc(
            UUID providerId);

    List<VerificationRecord> findByProviderLocation_IdOrderByPerformedAtDesc(
            UUID providerLocationId);

    List<VerificationRecord> findByProviderService_IdOrderByPerformedAtDesc(
            UUID providerServiceId);

    List<VerificationRecord> findByProviderServiceLocation_IdOrderByPerformedAtDesc(
            UUID providerServiceLocationId);
}