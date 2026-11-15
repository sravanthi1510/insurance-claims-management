package com.chubb.claims.claim.service;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(String identifier) {
        super("Claim not found: " + identifier);
    }
}
