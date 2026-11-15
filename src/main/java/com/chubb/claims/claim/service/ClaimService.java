package com.chubb.claims.claim.service;

import com.chubb.claims.claim.api.dto.SubmitClaimRequest;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.Market;
import com.chubb.claims.claim.entity.Claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ClaimService {

    Claim submit(SubmitClaimRequest request);

    Claim getById(String id);

    Claim getByReference(String reference);

    List<Claim> list(ClaimStatus status);

    Claim startReview(String id, String officer);

    Claim requestInformation(String id, String request);

    Claim provideAdditionalInformation(String id, String information);

    Claim settle(String id, String reason);

    Claim reject(String id, String reason);
}
