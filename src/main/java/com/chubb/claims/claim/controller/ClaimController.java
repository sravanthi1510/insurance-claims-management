package com.chubb.claims.claim.controller;

import com.chubb.claims.claim.api.mapper.ClaimMapper;
import com.chubb.claims.claim.api.dto.AdditionalInformationRequest;
import com.chubb.claims.claim.api.dto.ClaimResponse;
import com.chubb.claims.claim.api.dto.ClaimTrackingResponse;
import com.chubb.claims.claim.api.dto.OfficerRequest;
import com.chubb.claims.claim.api.dto.ReasonRequest;
import com.chubb.claims.claim.api.dto.SubmitClaimRequest;
import com.chubb.claims.claim.entity.Claim;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.service.ClaimService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ClaimResponse> submit(@Valid @RequestBody SubmitClaimRequest request) {
        log.info("Submitting claim for {}", request.claimantName());
        Claim claim = claimService.submit(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(claim.getClaimId())
                        .toUri())
                .body(claimMapper.toResponse(claim));
    }

    @GetMapping
    public List<ClaimResponse> list(@RequestParam(required = false) ClaimStatus status) {
        log.info("Listing claims with status: {}", status);
        return claimService.list(status).stream().map(claimMapper::toResponse).toList();
    }

    @GetMapping("/{claimId}")
    public ClaimResponse getById(@PathVariable String claimId) {
        log.info("Fetching claim with ID: {}", claimId);
        return claimMapper.toResponse(claimService.getById(claimId));
    }

    @GetMapping("/reference/{reference}")
    public ClaimTrackingResponse getByReference(@PathVariable String reference) {
        log.info("Fetching claim with reference: {}", reference);
        return claimMapper.toTrackingResponse(claimService.getByReference(reference));
    }

    @PostMapping("/{id}/review")
    public ClaimResponse startReview(@PathVariable String id, @Valid @RequestBody OfficerRequest request) {
        log.info("Starting review for claim with ID: {}", id);
        return claimMapper.toResponse(claimService.startReview(id, request.officer()));
    }

    @PostMapping("/{id}/information-requests")
    public ClaimResponse requestInformation(@PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        log.info("Requesting information for claim with ID: {}", id);
        return claimMapper.toResponse(claimService.requestInformation(id, request.reason()));
    }

    @PostMapping("/{id}/additional-information")
    public ClaimResponse provideAdditionalInformation(
            @PathVariable String id,
            @Valid @RequestBody AdditionalInformationRequest request
    ) {
        log.info("Providing additional information for claim with ID: {}", id);
        return claimMapper.toResponse(claimService.provideAdditionalInformation(id, request.information()));
    }

    @PostMapping("/{id}/settlement")
    public ClaimResponse settle(@PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        log.info("Settling claim with ID: {}", id);
        return claimMapper.toResponse(claimService.settle(id, request.reason()));
    }

    @PostMapping("/{id}/rejection")
    public ClaimResponse reject(@PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        log.info("Rejecting claim with ID: {}", id);
        return claimMapper.toResponse(claimService.reject(id, request.reason()));
    }
}
