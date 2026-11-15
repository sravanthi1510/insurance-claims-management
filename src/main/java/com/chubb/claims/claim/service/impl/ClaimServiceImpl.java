package com.chubb.claims.claim.service.impl;

import com.chubb.claims.claim.api.dto.SubmitClaimRequest;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.Market;
import com.chubb.claims.claim.entity.Claim;
import com.chubb.claims.claim.repository.ClaimRepository;
import com.chubb.claims.claim.service.ClaimNotFoundException;
import com.chubb.claims.claim.service.ClaimService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;

    public ClaimServiceImpl(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Override
    @Transactional
    public Claim submit(SubmitClaimRequest request) {
        return claimRepository.save(Claim.submit(
                request.claimantName(),
                request.claimantEmail(),
                request.market(),
                request.incidentDate(),
                request.incidentDescription(),
                request.estimatedLoss()
        ));
    }

    @Override
    public Claim getById(String id) {
        log.info("Fetching claim with ID: {}", claimRepository.findById(id));
        return claimRepository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Claim getByReference(String reference) {
        return claimRepository.findByClaimReference(reference)
                .orElseThrow(() -> new ClaimNotFoundException(reference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Claim> list(ClaimStatus status) {
        return status == null ? claimRepository.findAll() : claimRepository.findAllByStatus(status);
    }

    @Override
    @Transactional
    public Claim startReview(String id, String officer) {
        Claim claim = getById(id);
        claim.startReview(officer);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional
    public Claim requestInformation(String id, String request) {
        Claim claim = getById(id);
        claim.requestInformation(request);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional
    public Claim provideAdditionalInformation(String id, String information) {
        Claim claim = getById(id);
        claim.provideAdditionalInformation(information);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional
    public Claim settle(String id, String reason) {
        Claim claim = getById(id);
        claim.settle(reason);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional
    public Claim reject(String id, String reason) {
        Claim claim = getById(id);
        claim.reject(reason);
        return claimRepository.save(claim);
    }
}
