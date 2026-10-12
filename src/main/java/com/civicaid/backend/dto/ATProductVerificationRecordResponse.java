
package com.civicaid.backend.dto;

import com.civicaid.backend.entity.VerificationResult;
import java.time.Instant;
import java.util.UUID;

public record ATProductVerificationRecordResponse(
        UUID id,
        UUID productId,
        VerificationResult verificationResult,
        String sourceName,
        String sourceUrl,
        String sourceVersion,
        String reviewNotes,
        Instant performedAt
) {
}