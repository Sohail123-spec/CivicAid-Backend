package com.civicaid.backend.entity;

public enum AuditActionType {
    CREATE, UPDATE, VERIFY, REJECT_VERIFICATION, REVOKE_VERIFICATION,
    ACTIVATE, ARCHIVE, ROLE_ASSIGNED, ROLE_REMOVED
}
