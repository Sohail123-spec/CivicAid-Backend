package com.civicaid.backend.repository;

import com.civicaid.backend.entity.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    boolean existsByActorUser_Id(UUID actorUserId);
}
