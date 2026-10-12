package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.*;

@Entity
@Table(name = "providers")
public class Provider {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") @Size(max = 64)
    @Column(nullable = false, unique = true, length = 64) private String code;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) private String name;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "provider_type", nullable = false, length = 32) private ProviderType providerType;
    @Column(columnDefinition = "TEXT") private String description;
    @Pattern(regexp = "^https://\\S+$") @Size(max = 2048) @Column(name = "website_url", length = 2048) private String websiteUrl;
    @NotBlank @Size(max = 200) @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Pattern(regexp = "^https://\\S+$") @Size(max = 2048) @Column(name = "source_url", length = 2048) private String sourceUrl;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 16) private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    @PastOrPresent @Column(name = "last_verified_at") private LocalDate lastVerifiedAt;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public Provider() { }
    public UUID getId() { return id; } public String getCode() { return code; } public void setCode(String value) { code = value; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public ProviderType getProviderType() { return providerType; } public void setProviderType(ProviderType value) { providerType = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public String getWebsiteUrl() { return websiteUrl; } public void setWebsiteUrl(String value) { websiteUrl = value; }
    public String getSourceName() { return sourceName; } public void setSourceName(String value) { sourceName = value; }
    public String getSourceUrl() { return sourceUrl; } public void setSourceUrl(String value) { sourceUrl = value; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; } public void setVerificationStatus(VerificationStatus value) { verificationStatus = value; }
    public LocalDate getLastVerifiedAt() { return lastVerifiedAt; } public void setLastVerifiedAt(LocalDate value) { lastVerifiedAt = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
