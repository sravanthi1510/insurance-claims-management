package com.chubb.claims.auth.service;

import com.chubb.claims.auth.domain.AccountRole;
import com.chubb.claims.auth.domain.ClaimsUserPrincipal;
import com.chubb.claims.auth.dto.AuthRequest;
import com.chubb.claims.auth.dto.RegisterRequest;
import com.chubb.claims.auth.entity.AgentAccount;
import com.chubb.claims.auth.entity.CustomerAccount;
import com.chubb.claims.auth.repository.AgentAccountRepository;
import com.chubb.claims.auth.repository.CustomerAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CustomerAccountRepository customerAccountRepository;
    private final AgentAccountRepository agentAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String agentInviteCode;

    public AuthService(
            CustomerAccountRepository customerAccountRepository,
            AgentAccountRepository agentAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.agent-registration-code:}") String agentInviteCode
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.agentAccountRepository = agentAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.agentInviteCode = agentInviteCode;
    }

    @Transactional
    public ClaimsUserPrincipal register(RegisterRequest request) {
        if (request.role() == AccountRole.AGENT
                && (agentInviteCode.isBlank() || !agentInviteCode.equals(request.agentInviteCode()))) {
            throw new IllegalArgumentException("A valid agent invitation code is required");
        }

        String email = normalizeEmail(request.email());
        boolean accountExists = customerAccountRepository.existsByEmailIgnoreCase(email)
                || agentAccountRepository.existsByEmailIgnoreCase(email);
        if (accountExists) {
            throw new AccountAlreadyExistsException();
        }

        String fullName = request.fullName().trim();
        String passwordHash = passwordEncoder.encode(request.password());

        if (request.role() == AccountRole.CUSTOMER) {
            customerAccountRepository.save(new CustomerAccount(email, fullName, passwordHash));
        } else {
            agentAccountRepository.save(new AgentAccount(email, fullName, passwordHash));
        }
        return new ClaimsUserPrincipal(email, fullName, request.role());
    }

    @Transactional(readOnly = true)
    public ClaimsUserPrincipal authenticate(AuthRequest request) {
        String email = normalizeEmail(request.email());

        if (request.role() == AccountRole.CUSTOMER) {
            CustomerAccount account = customerAccountRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(InvalidCredentialsException::new);
            verifyPassword(request.password(), account.getPasswordHash());
            return new ClaimsUserPrincipal(account.getEmail(), account.getFullName(), request.role());
        }
        AgentAccount account = agentAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);
        verifyPassword(request.password(), account.getPasswordHash());
        return new ClaimsUserPrincipal(account.getEmail(), account.getFullName(), request.role());
    }

    private void verifyPassword(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new InvalidCredentialsException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
