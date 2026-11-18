package com.chubb.claims.auth.controller;

import com.chubb.claims.auth.domain.ClaimsUserPrincipal;
import com.chubb.claims.auth.dto.AuthRequest;
import com.chubb.claims.auth.dto.AuthUserResponse;
import com.chubb.claims.auth.dto.RegisterRequest;
import com.chubb.claims.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        ClaimsUserPrincipal principal = authService.register(request);
        return establishSession(principal, servletRequest, response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        ClaimsUserPrincipal principal = authService.authenticate(request);
        return establishSession(principal, servletRequest, response);
    }

    @GetMapping("/session")
    public AuthUserResponse session(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ClaimsUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return AuthUserResponse.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthUserResponse> establishSession(
            ClaimsUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        return ResponseEntity.ok(AuthUserResponse.from(principal));
    }
}
