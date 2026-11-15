package com.chubb.claims.claim.api.dto;

import com.chubb.claims.claim.domain.Market;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitClaimRequest(
        @NotBlank @Size(max = 120) String claimantName,
        @NotBlank @Email @Size(max = 254) String claimantEmail,
        @NotNull Market market,
        @NotNull @PastOrPresent LocalDate incidentDate,
        @NotBlank @Size(max = 2000) String incidentDescription,
        @NotNull @DecimalMin(value = "0.01") BigDecimal estimatedLoss
) {
}
