package com.chubb.claims.auth.dto;

import com.chubb.claims.auth.domain.AccountRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull AccountRole role
) {
}
