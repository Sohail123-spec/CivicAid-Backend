package com.civicaid.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "schemes")
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
    @Size(max = 64)
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 200)
    @Column(name = "authority_name", nullable = false, length = 200)
    private String authorityName;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(name = "benefits_summary", nullable = false, columnDefinition = "TEXT")
    private String benefitsSummary;

    @NotBlank
    @Column(name = "eligibility_summary", nullable = false, columnDefinition = "TEXT")
    private String eligibilitySummary;

    @Column(name = "required_documents_summary", columnDefinition = "TEXT")
    private String requiredDocumentsSummary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "application_method", nullable = false, length = 16)
    private ApplicationMethod applicationMethod;

    @NotBlank
    @Column(name = "application_instructions", nullable = false, columnDefinition = "TEXT")
    private String applicationInstructions;

    @NotBlank
    @Pattern(regexp = "^https://\\S+$")
    @Size(max = 2048)
    @Column(name = "official_source_url", nullable = false, length = 2048)
    private String officialSourceUrl;

    @Pattern(regexp = "^https://\\S+$")
    @Size(max = 2048)
    @Column(name = "official_application_url", length = 2048)
    private String officialApplicationUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction_level", nullable = false, length = 16)
    private JurisdictionLevel jurisdictionLevel;

    @Pattern(regexp = "^[A-Z]{2}$")
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "applicable_state_code", columnDefinition = "CHAR(2)")
    private String applicableStateCode;

    @Size(max = 100)
    @Column(name = "source_version", length = 100)
    private String sourceVersion;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 16)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @PastOrPresent
    @Column(name = "last_verified_at")
    private LocalDate lastVerifiedAt;

    @NotNull
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Scheme() {
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String authorityName) { this.authorityName = authorityName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBenefitsSummary() { return benefitsSummary; }
    public void setBenefitsSummary(String benefitsSummary) { this.benefitsSummary = benefitsSummary; }
    public String getEligibilitySummary() { return eligibilitySummary; }
    public void setEligibilitySummary(String eligibilitySummary) { this.eligibilitySummary = eligibilitySummary; }
    public String getRequiredDocumentsSummary() { return requiredDocumentsSummary; }
    public void setRequiredDocumentsSummary(String requiredDocumentsSummary) { this.requiredDocumentsSummary = requiredDocumentsSummary; }
    public ApplicationMethod getApplicationMethod() { return applicationMethod; }
    public void setApplicationMethod(ApplicationMethod applicationMethod) { this.applicationMethod = applicationMethod; }
    public String getApplicationInstructions() { return applicationInstructions; }
    public void setApplicationInstructions(String applicationInstructions) { this.applicationInstructions = applicationInstructions; }
    public String getOfficialSourceUrl() { return officialSourceUrl; }
    public void setOfficialSourceUrl(String officialSourceUrl) { this.officialSourceUrl = officialSourceUrl; }
    public String getOfficialApplicationUrl() { return officialApplicationUrl; }
    public void setOfficialApplicationUrl(String officialApplicationUrl) { this.officialApplicationUrl = officialApplicationUrl; }
    public JurisdictionLevel getJurisdictionLevel() { return jurisdictionLevel; }
    public void setJurisdictionLevel(JurisdictionLevel jurisdictionLevel) { this.jurisdictionLevel = jurisdictionLevel; }
    public String getApplicableStateCode() { return applicableStateCode; }
    public void setApplicableStateCode(String applicableStateCode) { this.applicableStateCode = applicableStateCode; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public LocalDate getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDate lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
