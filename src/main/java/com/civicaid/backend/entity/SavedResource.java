package com.civicaid.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "saved_resources")
public class SavedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_saved_resources_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "at_category_id",
            foreignKey = @ForeignKey(name = "fk_saved_resources_at_category"))
    private ATCategory atCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "at_product_id",
            foreignKey = @ForeignKey(name = "fk_saved_resources_at_product"))
    private ATProduct atProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "scheme_id",
            foreignKey = @ForeignKey(name = "fk_saved_resources_scheme"))
    private Scheme scheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_id",
            foreignKey = @ForeignKey(name = "fk_saved_resources_provider"))
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_location_id",
            foreignKey = @ForeignKey(name = "fk_saved_resources_provider_location"))
    private ProviderLocation providerLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_service_location_id",
            foreignKey = @ForeignKey(
                    name = "fk_saved_resources_provider_service_location"))
    private ProviderServiceLocation providerServiceLocation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SavedResource() {
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User value) {
        user = value;
    }

    public ATCategory getAtCategory() {
        return atCategory;
    }

    public void setAtCategory(ATCategory value) {
        atCategory = value;
    }

    public ATProduct getAtProduct() {
        return atProduct;
    }

    public void setAtProduct(ATProduct value) {
        atProduct = value;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public void setScheme(Scheme value) {
        scheme = value;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider value) {
        provider = value;
    }

    public ProviderLocation getProviderLocation() {
        return providerLocation;
    }

    public void setProviderLocation(ProviderLocation value) {
        providerLocation = value;
    }

    public ProviderServiceLocation getProviderServiceLocation() {
        return providerServiceLocation;
    }

    public void setProviderServiceLocation(
            ProviderServiceLocation value) {
        providerServiceLocation = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}