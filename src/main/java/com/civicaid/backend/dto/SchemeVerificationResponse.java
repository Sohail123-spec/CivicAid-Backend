
package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationResult;
import java.time.Instant;
import java.util.UUID;

public record SchemeVerificationResponse(
        UUID id,
        UUID verifierUserId,
        VerificationResult result,
        String sourceName,
        String sourceUrl,
        String sourceVersion,
        String reviewNotes,
        Instant performedAt
) {
}