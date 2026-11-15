package com.chubb.claims.claim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfficerRequest(
        @NotBlank @Size(max = 120) String officer
) {
}
