package com.chubb.claims.claim.repository;

import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, String> {

    Optional<Claim> findByClaimReference(String claimReference);

    List<Claim> findAllByClaimantEmailIgnoreCase(String claimantEmail);

    List<Claim> findAllByClaimantEmailIgnoreCaseAndStatus(String claimantEmail, ClaimStatus status);

    @Query("""
            select claim from Claim claim
            where claim.assignedOfficerEmail = :email
               or (claim.status = :status and claim.assignedOfficer is null)
            """)
    List<Claim> findAssignedOrUnassignedSubmissions(
            @Param("email") String email,
            @Param("status") ClaimStatus status
    );
}
