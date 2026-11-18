package com.chubb.claims.claim.controller;

import com.chubb.claims.auth.domain.ClaimsUserPrincipal;
import com.chubb.claims.claim.api.dto.AdditionalInformationRequest;
import com.chubb.claims.claim.api.dto.ClaimResponse;
import com.chubb.claims.claim.api.dto.ClaimTrackingResponse;
import com.chubb.claims.claim.api.dto.ReasonRequest;
import com.chubb.claims.claim.api.dto.SubmitClaimRequest;
import com.chubb.claims.claim.api.mapper.ClaimMapper;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.entity.Claim;
import com.chubb.claims.claim.service.ClaimService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimMapper claimMapper;

    public ClaimController(ClaimService claimService, ClaimMapper claimMapper) {
        this.claimService = claimService;
        this.claimMapper = claimMapper;
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    public ResponseEntity<ClaimResponse> submit(
            @Valid @RequestBody SubmitClaimRequest request,
            Authentication authentication
    ) {
        SubmitClaimRequest ownedRequest = new SubmitClaimRequest(
                request.claimantName(),
                authentication.getName(),
                request.market(),
                request.incidentDate(),
                request.incidentDescription(),
                request.estimatedLoss()
        );
        log.info("Submitting claim for {}", ownedRequest.claimantName());
        Claim claim = claimService.submit(ownedRequest);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(claim.getClaimId())
                        .toUri())
                .body(claimMapper.toResponse(claim));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    public List<ClaimResponse> list(
            Authentication authentication,
            @RequestParam(required = false) ClaimStatus status
    ) {
        log.info("Listing claims with status: {}", status);
        List<Claim> claims;
        if (isAgent(authentication)) {
            claims = claimService.listForAgent(authentication.getName());
            if (status != null) {
                claims = claims.stream().filter(claim -> claim.getStatus() == status).toList();
            }
        } else {
            claims = claimService.listForCustomer(authentication.getName(), status);
        }
        return claims.stream().map(claimMapper::toResponse).toList();
    }

    @GetMapping("/agent/queue")
    @PreAuthorize("hasRole('AGENT')")
    public List<ClaimResponse> agentQueue(Authentication authentication) {
        return claimService.listForAgent(authentication.getName()).stream().map(claimMapper::toResponse).toList();
    }

    @GetMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    public ClaimResponse getById(@PathVariable String claimId, Authentication authentication) {
        log.info("Fetching claim with ID: {}", claimId);
        Claim claim = claimService.getById(claimId);
        assertCanView(claim, authentication);
        return claimMapper.toResponse(claim);
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    public ClaimTrackingResponse getByReference(@PathVariable String reference, Authentication authentication) {
        log.info("Fetching claim with reference: {}", reference);
        Claim claim = claimService.getByReference(reference);
        assertCanView(claim, authentication);
        return claimMapper.toTrackingResponse(claim);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('AGENT')")
    public ClaimResponse startReview(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("Starting review for claim with ID: {}", id);
        Claim claim = claimService.getById(id);
        assertAgentOwnsOrCanClaim(claim, authentication);
        return claimMapper.toResponse(claimService.startReview(
                id,
                displayName(authentication),
                authentication.getName()
        ));
    }

    @PostMapping("/{id}/information-requests")
    @PreAuthorize("hasRole('AGENT')")
    public ClaimResponse requestInformation(
            @PathVariable String id,
            @Valid @RequestBody ReasonRequest request,
            Authentication authentication
    ) {
        log.info("Requesting information for claim with ID: {}", id);
        assertAgentOwnsClaim(claimService.getById(id), authentication);
        return claimMapper.toResponse(claimService.requestInformation(id, request.reason()));
    }

    @PostMapping("/{id}/additional-information")
    public ClaimResponse provideAdditionalInformation(
            @PathVariable String id,
            @Valid @RequestBody AdditionalInformationRequest request,
            Authentication authentication
    ) {
        log.info("Providing additional information for claim with ID: {}", id);
        Claim claim = claimService.getById(id);
        assertCanView(claim, authentication);
        return claimMapper.toResponse(claimService.provideAdditionalInformation(id, request.information()));
    }

    @PostMapping("/{id}/settlement")
    @PreAuthorize("hasRole('AGENT')")
    public ClaimResponse settle(
            @PathVariable String id,
            @Valid @RequestBody ReasonRequest request,
            Authentication authentication
    ) {
        log.info("Settling claim with ID: {}", id);
        assertAgentOwnsClaim(claimService.getById(id), authentication);
        return claimMapper.toResponse(claimService.settle(id, request.reason()));
    }

    @PostMapping("/{id}/rejection")
    @PreAuthorize("hasRole('AGENT')")
    public ClaimResponse reject(
            @PathVariable String id,
            @Valid @RequestBody ReasonRequest request,
            Authentication authentication
    ) {
        log.info("Rejecting claim with ID: {}", id);
        assertAgentOwnsClaim(claimService.getById(id), authentication);
        return claimMapper.toResponse(claimService.reject(id, request.reason()));
    }

    private void assertCanView(Claim claim, Authentication authentication) {
        if (isAgent(authentication)) {
            boolean assignedToAgent = isAssignedToAgent(claim, authentication);
            boolean availableToClaim = claim.getStatus() == ClaimStatus.SUBMITTED
                    && claim.getAssignedOfficer() == null;
            if (!assignedToAgent && !availableToClaim) {
                throw new org.springframework.security.access.AccessDeniedException("This claim is not in your queue");
            }
        } else if (!claim.getClaimantEmail().equalsIgnoreCase(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this claim");
        }
    }

    private void assertAgentOwnsOrCanClaim(Claim claim, Authentication authentication) {
        String officer = displayName(authentication);
        boolean assignedToAnotherAgent = claim.getAssignedOfficerEmail() != null
                ? !claim.getAssignedOfficerEmail().equals(authentication.getName())
                : claim.getAssignedOfficer() != null && !claim.getAssignedOfficer().equals(officer);
        if (assignedToAnotherAgent) {
            throw new org.springframework.security.access.AccessDeniedException("This claim belongs to another agent");
        }
    }

    private void assertAgentOwnsClaim(Claim claim, Authentication authentication) {
        if (!isAssignedToAgent(claim, authentication)) {
            throw new org.springframework.security.access.AccessDeniedException("This claim is not assigned to you");
        }
    }

    private boolean isAssignedToAgent(Claim claim, Authentication authentication) {
        if (claim.getAssignedOfficerEmail() != null) {
            return claim.getAssignedOfficerEmail().equals(authentication.getName());
        }
        return displayName(authentication).equals(claim.getAssignedOfficer());
    }

    private boolean isAgent(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_AGENT"));
    }

    private String displayName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof ClaimsUserPrincipal principal) {
            return principal.displayName();
        }
        return authentication.getName();
    }
}
