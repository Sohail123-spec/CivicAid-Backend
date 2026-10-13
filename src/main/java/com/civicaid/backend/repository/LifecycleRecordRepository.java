package com.civicaid.backend.repository;

import com.civicaid.backend.entity.LifecycleRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Clears the self-referencing replaces_lifecycle_record_id column for all
     * lifecycle records owned by the given user. This must be called before
     * deleteByUser_Id to avoid ON DELETE RESTRICT violations from the
     * fk_lifecycle_records_replaces constraint.
     */
    @Modifying
    @Query("UPDATE LifecycleRecord l SET l.replacesLifecycleRecord = NULL WHERE l.user.id = :userId")
    void clearReplacementsByUserId(@Param("userId") UUID userId);

    /**
     * Bulk-deletes all lifecycle records (and their cascaded maintenance and
     * repair records) for the given user. Call clearReplacementsByUserId first.
     */
    void deleteByUser_Id(UUID userId);
}
