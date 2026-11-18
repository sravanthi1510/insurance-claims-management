package com.chubb.claims.claim.entity;

import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.Market;
import com.chubb.claims.exception.InvalidClaimTransitionException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "INSURANCE_CLAIM")
public class Claim {

    @Id
    @Column(name = "CLAIM_ID", nullable = false, unique = true, updatable = false, length = 16)
    private String claimId;

    @Column(name = "CLAIM_REFERENCE",
            nullable = false, unique = true, length = 40)
    private String claimReference;

    @Column(name = "CLAIMANT_NAME",
            nullable = false, length = 120)
    private String claimantName;

    @Column(name = "CLAIMANT_EMAIL",
            nullable = false, length = 254)
    private String claimantEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET",
            nullable = false, length = 2)
    private Market market;

    @Column(name = "INCIDENT_DATE", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "INCIDENT_DESCRIPTION",
            nullable = false, length = 2000)
    private String incidentDescription;

    @Column(name = "ESTIMATED_LOSS",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedLoss;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLAIM_STATUS",
            nullable = false, length = 32)
    private ClaimStatus status;

    @Column(name = "ASSIGNED_OFFICER", length = 120)
    private String assignedOfficer;

    @Column(name = "ASSIGNED_OFFICER_EMAIL", length = 254)
    private String assignedOfficerEmail;

    @Column(name = "INFORMATION_REQUESTED", length = 2000)
    private String informationRequested;

    @Column(name = "ADDITIONAL_INFORMATION", length = 4000)
    private String additionalInformation;

    @Column(name = "DECISION_REASON", length = 2000)
    private String decisionReason;

    @Column(name = "SUBMITTED_AT",
            nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "VERSION")
    private long version;

    protected Claim() {
    }

    private Claim(
            String claimantName,
            String claimantEmail,
            Market market,
            LocalDate incidentDate,
            String incidentDescription,
            BigDecimal estimatedLoss) {

        this.claimId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        this.claimReference = "CLM-" + claimId;

        this.claimantName = claimantName;
        this.claimantEmail = claimantEmail;
        this.market = market;
        this.incidentDate = incidentDate;
        this.incidentDescription = incidentDescription;
        this.estimatedLoss = estimatedLoss;
        this.status = ClaimStatus.SUBMITTED;
    }

    public static Claim submit(
            String claimantName,
            String claimantEmail,
            Market market,
            LocalDate incidentDate,
            String incidentDescription,
            BigDecimal estimatedLoss) {

        return new Claim(
                claimantName,
                claimantEmail,
                market,
                incidentDate,
                incidentDescription,
                estimatedLoss);
    }

    public void startReview(String officer) {
        startReview(officer, null);
    }

    public void startReview(String officer, String officerEmail) {
        requireStatus(ClaimStatus.SUBMITTED, "start review");
        this.assignedOfficer = requireText(officer, "Assigned officer");
        this.assignedOfficerEmail = officerEmail;
        this.status = ClaimStatus.UNDER_REVIEW;
    }

    public void requestInformation(String request) {
        requireStatus(ClaimStatus.UNDER_REVIEW, "request information");
        this.informationRequested =
                requireText(request, "Information request");
        this.status = ClaimStatus.INFORMATION_REQUIRED;
    }

    public void provideAdditionalInformation(String information) {
        requireStatus(
                ClaimStatus.INFORMATION_REQUIRED,
                "provide additional information");

        this.additionalInformation =
                requireText(information, "Additional information");

        this.informationRequested = null;
        this.status = ClaimStatus.UNDER_REVIEW;
    }

    public void settle(String reason) {
        decide(ClaimStatus.SETTLED, reason);
    }

    public void reject(String reason) {
        decide(ClaimStatus.REJECTED, reason);
    }

    private void decide(ClaimStatus decision, String reason) {
        requireStatus(ClaimStatus.UNDER_REVIEW,
                decision.name().toLowerCase());

        this.decisionReason =
                requireText(reason, "Decision reason");

        this.status = decision;
    }

    private void requireStatus(
            ClaimStatus expected, String action) {

        if (status != expected) {
            throw new InvalidClaimTransitionException(
                    "Cannot " + action + " a claim in "
                            + status + " status; expected " + expected);
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank");
        }
        return value.trim();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        if (submittedAt == null) {
            submittedAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // Existing getters can remain unchanged.

    public String getClaimReference() {
        return claimReference;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public void setClaimReference(String claimReference) {
        this.claimReference = claimReference;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getClaimantEmail() {
        return claimantEmail;
    }

    public void setClaimantEmail(String claimantEmail) {
        this.claimantEmail = claimantEmail;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
    }

    public String getIncidentDescription() {
        return incidentDescription;
    }

    public void setIncidentDescription(String incidentDescription) {
        this.incidentDescription = incidentDescription;
    }

    public BigDecimal getEstimatedLoss() {
        return estimatedLoss;
    }

    public void setEstimatedLoss(BigDecimal estimatedLoss) {
        this.estimatedLoss = estimatedLoss;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getAssignedOfficer() {
        return assignedOfficer;
    }

    public String getAssignedOfficerEmail() {
        return assignedOfficerEmail;
    }

    public void setAssignedOfficer(String assignedOfficer) {
        this.assignedOfficer = assignedOfficer;
    }

    public void setAssignedOfficerEmail(String assignedOfficerEmail) {
        this.assignedOfficerEmail = assignedOfficerEmail;
    }

    public String getInformationRequested() {
        return informationRequested;
    }

    public void setInformationRequested(String informationRequested) {
        this.informationRequested = informationRequested;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
