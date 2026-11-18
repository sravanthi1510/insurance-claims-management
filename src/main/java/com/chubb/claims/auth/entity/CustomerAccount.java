package com.chubb.claims.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user", catalog = "customer_db")
public class CustomerAccount extends AccountEntity {

    protected CustomerAccount() {
    }

    public CustomerAccount(String email, String fullName, String passwordHash) {
        super(email, fullName, passwordHash);
    }
}
