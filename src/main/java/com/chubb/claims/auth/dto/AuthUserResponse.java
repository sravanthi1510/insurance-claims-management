package com.chubb.claims.auth.dto;

import com.chubb.claims.auth.domain.AccountRole;
import com.chubb.claims.auth.domain.ClaimsUserPrincipal;

public record AuthUserResponse(String email, String fullName, AccountRole role) {

    public static AuthUserResponse from(ClaimsUserPrincipal principal) {
        return new AuthUserResponse(principal.email(), principal.displayName(), principal.role());
    }
}
