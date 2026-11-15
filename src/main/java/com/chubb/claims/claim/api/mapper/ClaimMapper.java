package com.chubb.claims.claim.api.mapper;

import com.chubb.claims.claim.api.dto.ClaimResponse;
import com.chubb.claims.claim.api.dto.ClaimTrackingResponse;
import com.chubb.claims.claim.entity.Claim;
import org.springframework.stereotype.Component;

@Component
public class ClaimMapper {

    public ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getClaimId(),
                claim.getClaimReference(),
                claim.getClaimantName(),
                claim.getClaimantEmail(),
                claim.getMarket(),
                claim.getIncidentDate(),
                claim.getIncidentDescription(),
                claim.getEstimatedLoss(),
                claim.getStatus(),
                claim.getAssignedOfficer(),
                claim.getInformationRequested(),
                claim.getAdditionalInformation(),
                claim.getDecisionReason(),
                claim.getSubmittedAt(),
                claim.getUpdatedAt()
        );
    }

    public ClaimTrackingResponse toTrackingResponse(Claim claim) {
        return new ClaimTrackingResponse(
                claim.getClaimReference(),
                claim.getStatus(),
                claim.getInformationRequested(),
                claim.getDecisionReason(),
                claim.getSubmittedAt(),
                claim.getUpdatedAt()
        );
    }
}
