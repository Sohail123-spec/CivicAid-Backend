package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.*;

@Entity
@Table(name = "provider_service_locations", uniqueConstraints = @UniqueConstraint(name = "uq_provider_service_locations_service_location", columnNames = {"provider_service_id", "provider_location_id"}))
public class ProviderServiceLocation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "provider_service_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_service_locations_service")) private ProviderService providerService;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "provider_location_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_service_locations_location")) private ProviderLocation providerLocation;
    @NotBlank @Size(max = 200) @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Pattern(regexp = "^https://\\S+$") @Size(max = 2048) @Column(name = "source_url", length = 2048) private String sourceUrl;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 16) private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    @PastOrPresent @Column(name = "last_verified_at") private LocalDate lastVerifiedAt;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public ProviderServiceLocation() { }
    public UUID getId() { return id; } public ProviderService getProviderService() { return providerService; } public void setProviderService(ProviderService value) { providerService = value; }
    public ProviderLocation getProviderLocation() { return providerLocation; } public void setProviderLocation(ProviderLocation value) { providerLocation = value; }
    public String getSourceName() { return sourceName; } public void setSourceName(String value) { sourceName = value; }
    public String getSourceUrl() { return sourceUrl; } public void setSourceUrl(String value) { sourceUrl = value; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; } public void setVerificationStatus(VerificationStatus value) { verificationStatus = value; }
    public LocalDate getLastVerifiedAt() { return lastVerifiedAt; } public void setLastVerifiedAt(LocalDate value) { lastVerifiedAt = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
