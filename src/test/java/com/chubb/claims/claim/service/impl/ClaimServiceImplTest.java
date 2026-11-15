package com.chubb.claims.claim.service.impl;

import com.chubb.claims.claim.api.dto.SubmitClaimRequest;
import com.chubb.claims.claim.domain.Market;
import com.chubb.claims.claim.entity.Claim;
import com.chubb.claims.claim.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ClaimServiceImpl claimService;

    @Test
    void submitPersistsClaimWithSixteenCharacterId() {
        when(claimRepository.save(any(Claim.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SubmitClaimRequest request = new SubmitClaimRequest(
                "Jamie Claimant",
                "jamie@example.com",
                Market.AU,
                LocalDate.of(2026, 1, 10),
                "Vehicle damage",
                new BigDecimal("1250.00"));

        Claim submitted = claimService.submit(request);

        assertEquals(16, submitted.getClaimId().length());
        assertTrue(submitted.getClaimId().matches("[0-9A-F]{16}"));
        verify(claimRepository).save(submitted);
    }
}
