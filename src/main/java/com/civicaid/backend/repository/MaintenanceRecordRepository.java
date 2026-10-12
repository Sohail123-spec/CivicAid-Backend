package com.civicaid.backend.repository;

import com.civicaid.backend.entity.MaintenanceRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceRecordRepository
        extends JpaRepository<MaintenanceRecord, UUID> {

    List<MaintenanceRecord>
            findByLifecycleRecord_User_IdAndLifecycleRecord_IdOrderByMaintenanceDateDescCreatedAtDesc(
                    UUID userId,
                    UUID lifecycleRecordId);

    Optional<MaintenanceRecord>
            findByIdAndLifecycleRecord_User_Id(
                    UUID maintenanceRecordId,
                    UUID userId);

    boolean existsByIdAndLifecycleRecord_User_Id(
            UUID maintenanceRecordId,
            UUID userId);
}