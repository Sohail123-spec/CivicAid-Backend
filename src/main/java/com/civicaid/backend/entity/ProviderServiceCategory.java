package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.*;

@Entity
@Table(name = "provider_service_categories", uniqueConstraints = @UniqueConstraint(name = "uq_provider_service_categories_service_category", columnNames = {"provider_service_id", "at_category_id"}))
public class ProviderServiceCategory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "provider_service_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_service_categories_service")) private ProviderService providerService;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "at_category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_service_categories_at_category")) private ATCategory atCategory;
    @NotBlank @Size(max = 500) @Column(nullable = false, length = 500) private String rationale;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public ProviderServiceCategory() { }
    public UUID getId() { return id; } public ProviderService getProviderService() { return providerService; } public void setProviderService(ProviderService value) { providerService = value; }
    public ATCategory getAtCategory() { return atCategory; } public void setAtCategory(ATCategory value) { atCategory = value; }
    public String getRationale() { return rationale; } public void setRationale(String value) { rationale = value; } public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
