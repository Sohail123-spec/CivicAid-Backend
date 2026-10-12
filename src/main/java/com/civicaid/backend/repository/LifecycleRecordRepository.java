package com.civicaid.backend.repository;

import com.civicaid.backend.entity.LifecycleRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LifecycleRecordRepository
        extends JpaRepository<LifecycleRecord, UUID> {

    List<LifecycleRecord> findByUser_IdOrderByAcquiredOnDescCreatedAtDesc(
            UUID userId);

    Optional<LifecycleRecord> findByIdAndUser_Id(
            UUID lifecycleRecordId,
            UUID userId);

    boolean existsByIdAndUser_Id(
            UUID lifecycleRecordId,
            UUID userId);

    boolean existsByReplacesLifecycleRecord_Id(
            UUID lifecycleRecordId);
}
