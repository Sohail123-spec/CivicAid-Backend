package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_audit_logs_actor")) private User actorUser;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 40) private AuditTargetType targetType;
    @NotNull @Column(name = "target_id", nullable = false) private UUID targetId;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false, length = 24) private AuditActionType actionType;
    @JdbcTypeCode(Types.ARRAY) @Column(name = "changed_fields", columnDefinition = "TEXT ARRAY") private String[] changedFields;
    @Size(max = 1000) @Column(name = "change_summary", length = 1000) private String changeSummary;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    public AuditLog() { }
    public UUID getId() { return id; } public User getActorUser() { return actorUser; } public void setActorUser(User value) { actorUser = value; }
    public AuditTargetType getTargetType() { return targetType; } public void setTargetType(AuditTargetType value) { targetType = value; }
    public UUID getTargetId() { return targetId; } public void setTargetId(UUID value) { targetId = value; }
    public AuditActionType getActionType() { return actionType; } public void setActionType(AuditActionType value) { actionType = value; }
    public String[] getChangedFields() { return changedFields == null ? null : changedFields.clone(); }
    public void setChangedFields(String[] value) { changedFields = value == null ? null : value.clone(); }
    public String getChangeSummary() { return changeSummary; } public void setChangeSummary(String value) { changeSummary = value; }
    public Instant getOccurredAt() { return occurredAt; } public void setOccurredAt(Instant value) { occurredAt = value; }
}
