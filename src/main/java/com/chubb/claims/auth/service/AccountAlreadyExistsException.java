package com.chubb.claims.auth.service;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException() {
        super("An account with this email already exists. Please sign in instead.");
    }
}
