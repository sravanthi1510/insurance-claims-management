package com.chubb.claims.claim.api.dto;

import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.Market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
public record ClaimResponse(
        String claimId,
        String claimReference,
        String claimantName,
        String claimantEmail,
        Market market,
        LocalDate incidentDate,
        String incidentDescription,
        BigDecimal estimatedLoss,
        ClaimStatus status,
        String assignedOfficer,
        String informationRequested,
        String additionalInformation,
        String decisionReason,
        Instant submittedAt,
        Instant updatedAt
) {
}
