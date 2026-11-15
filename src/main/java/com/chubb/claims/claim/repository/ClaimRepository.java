package com.chubb.claims.claim.repository;

import com.chubb.claims.claim.entity.Claim;
import com.chubb.claims.claim.domain.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface ClaimRepository extends JpaRepository<Claim, String> {

    Optional<Claim> findByClaimReference(String claimReference);

    List<Claim> findAllByStatus(ClaimStatus status);
}
