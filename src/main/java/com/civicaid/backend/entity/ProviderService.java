package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.*;

@Entity
@Table(name = "provider_services", uniqueConstraints = @UniqueConstraint(name = "uq_provider_services_provider_code", columnNames = {"provider_id", "code"}))
public class ProviderService {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "provider_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_services_provider")) private Provider provider;
    @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") @Size(max = 64) @Column(nullable = false, length = 64) private String code;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) private String name;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "service_type", nullable = false, length = 40) private ProviderServiceType serviceType;
    @NotBlank @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @NotBlank @Size(max = 200) @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Pattern(regexp = "^https://\\S+$") @Size(max = 2048) @Column(name = "source_url", length = 2048) private String sourceUrl;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 16) private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    @PastOrPresent @Column(name = "last_verified_at") private LocalDate lastVerifiedAt;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public ProviderService() { }
    public UUID getId() { return id; } public Provider getProvider() { return provider; } public void setProvider(Provider value) { provider = value; }
    public String getCode() { return code; } public void setCode(String value) { code = value; } public String getName() { return name; } public void setName(String value) { name = value; }
    public ProviderServiceType getServiceType() { return serviceType; } public void setServiceType(ProviderServiceType value) { serviceType = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public String getSourceName() { return sourceName; } public void setSourceName(String value) { sourceName = value; }
    public String getSourceUrl() { return sourceUrl; } public void setSourceUrl(String value) { sourceUrl = value; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; } public void setVerificationStatus(VerificationStatus value) { verificationStatus = value; }
    public LocalDate getLastVerifiedAt() { return lastVerifiedAt; } public void setLastVerifiedAt(LocalDate value) { lastVerifiedAt = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
