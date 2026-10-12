package com.civicaid.backend.service;

import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditLog;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.repository.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(
            User actor,
            AuditTargetType targetType,
            UUID targetId,
            AuditActionType actionType,
            String[] changedFields,
            String summary) {
        AuditLog log = new AuditLog();
        log.setActorUser(actor);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActionType(actionType);
        log.setChangedFields(changedFields);
        log.setChangeSummary(summary);
        log.setOccurredAt(Instant.now());
        auditLogRepository.save(log);
    }
}
