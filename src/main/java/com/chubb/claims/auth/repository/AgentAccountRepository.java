package com.chubb.claims.auth.repository;

import com.chubb.claims.auth.entity.AgentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentAccountRepository extends JpaRepository<AgentAccount, Long> {

    Optional<AgentAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
