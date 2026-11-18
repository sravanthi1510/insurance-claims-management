package com.chubb.claims.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user", catalog = "agent_db")
public class AgentAccount extends AccountEntity {

    protected AgentAccount() {
    }

    public AgentAccount(String email, String fullName, String passwordHash) {
        super(email, fullName, passwordHash);
    }
}
