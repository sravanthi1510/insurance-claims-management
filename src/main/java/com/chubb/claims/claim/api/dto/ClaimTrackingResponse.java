package com.chubb.claims.claim.api.dto;

import com.chubb.claims.claim.domain.ClaimStatus;

import java.time.Instant;

public record ClaimTrackingResponse(
        String claimReference,
        ClaimStatus status,
        String informationRequested,
        String decisionReason,
        Instant submittedAt,
        Instant updatedAt
) {
}
