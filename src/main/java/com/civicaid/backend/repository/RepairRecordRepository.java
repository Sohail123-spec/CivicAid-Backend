package com.civicaid.backend.repository;

import com.civicaid.backend.entity.RepairRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairRecordRepository
        extends JpaRepository<RepairRecord, UUID> {

    List<RepairRecord>
            findByLifecycleRecord_User_IdAndLifecycleRecord_IdOrderByRepairDateDescCreatedAtDesc(
                    UUID userId,
                    UUID lifecycleRecordId);

    Optional<RepairRecord>
            findByIdAndLifecycleRecord_User_Id(
                    UUID repairRecordId,
                    UUID userId);

    boolean existsByIdAndLifecycleRecord_User_Id(
            UUID repairRecordId,
            UUID userId);
}