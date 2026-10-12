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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "need_category_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_need_category_mappings_need_category",
                columnNames = {"functional_need_id", "at_category_id"}))
public class NeedCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "functional_need_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_need_category_mappings_functional_need"))
    private FunctionalNeed functionalNeed;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "at_category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_need_category_mappings_at_category"))
    private ATCategory atCategory;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String rationale;

    @NotNull
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NeedCategoryMapping() {
    }

    public UUID getId() { return id; }
    public FunctionalNeed getFunctionalNeed() { return functionalNeed; }
    public void setFunctionalNeed(FunctionalNeed functionalNeed) { this.functionalNeed = functionalNeed; }
    public ATCategory getAtCategory() { return atCategory; }
    public void setAtCategory(ATCategory atCategory) { this.atCategory = atCategory; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
