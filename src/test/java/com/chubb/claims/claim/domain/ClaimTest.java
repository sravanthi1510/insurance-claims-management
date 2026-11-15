package com.chubb.claims.claim.domain;

import com.chubb.claims.claim.entity.Claim;
import com.chubb.claims.exception.InvalidClaimTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimTest {

    @Test
    void claimCanProgressThroughInformationRequestAndSettlement() {
        Claim claim = newClaim();

        claim.startReview("Alex Officer");
        claim.requestInformation("Please provide a repair estimate");
        claim.provideAdditionalInformation("Repair estimate attached");
        claim.settle("Repairs approved");

        assertEquals(ClaimStatus.SETTLED, claim.getStatus());
        assertEquals("Alex Officer", claim.getAssignedOfficer());
        assertEquals("Repairs approved", claim.getDecisionReason());
        assertEquals(null, claim.getInformationRequested());
    }

    @Test
    void claimCannotBeSettledBeforeReview() {
        Claim claim = newClaim();

        assertThrows(InvalidClaimTransitionException.class, () -> claim.settle("Approved"));
    }

    @Test
    void generatedClaimIdContainsSixteenCharacters() {
        Claim claim = newClaim();

        assertEquals(16, claim.getClaimId().length());
        assertTrue(claim.getClaimId().matches("[0-9A-F]{16}"));
        assertEquals("CLM-" + claim.getClaimId(), claim.getClaimReference());
    }

    private Claim newClaim() {
        return Claim.submit(
                "Jamie Claimant",
                "jamie@example.com",
                Market.AU,
                LocalDate.of(2026, 1, 10),
                "Vehicle damage",
                new BigDecimal("1250.00")
        );
    }
}
