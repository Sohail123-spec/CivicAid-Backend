package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.*;

@Entity
@Table(name = "lifecycle_records")
public class LifecycleRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lifecycle_records_user")) private User user;
    @NotBlank @Size(max = 200) @Column(name = "item_name", nullable = false, length = 200) private String itemName;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "at_product_id", foreignKey = @ForeignKey(name = "fk_lifecycle_records_at_product")) private ATProduct atProduct;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "at_category_id", foreignKey = @ForeignKey(name = "fk_lifecycle_records_at_category")) private ATCategory atCategory;
    @NotNull @PastOrPresent @Column(name = "acquired_on", nullable = false) private LocalDate acquiredOn;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "acquisition_method", nullable = false, length = 16) private AcquisitionMethod acquisitionMethod;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "condition_at_acquisition", nullable = false, length = 16) private ConditionAtAcquisition conditionAtAcquisition;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "lifecycle_status", nullable = false, length = 24) private LifecycleStatus lifecycleStatus;
    @Column(name = "warranty_expires_on") private LocalDate warrantyExpiresOn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "preferred_provider_service_location_id", foreignKey = @ForeignKey(name = "fk_lifecycle_records_preferred_service_location")) private ProviderServiceLocation preferredProviderServiceLocation;
    @Column(name = "replacement_due_on") private LocalDate replacementDueOn;
    @Column(name = "reassessment_due_on") private LocalDate reassessmentDueOn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "replaces_lifecycle_record_id", foreignKey = @ForeignKey(name = "fk_lifecycle_records_replaces")) private LifecycleRecord replacesLifecycleRecord;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public LifecycleRecord() { }
    public UUID getId() { return id; } public User getUser() { return user; } public void setUser(User value) { user = value; }
    public String getItemName() { return itemName; } public void setItemName(String value) { itemName = value; }
    public ATProduct getAtProduct() { return atProduct; } public void setAtProduct(ATProduct value) { atProduct = value; }
    public ATCategory getAtCategory() { return atCategory; } public void setAtCategory(ATCategory value) { atCategory = value; }
    public LocalDate getAcquiredOn() { return acquiredOn; } public void setAcquiredOn(LocalDate value) { acquiredOn = value; }
    public AcquisitionMethod getAcquisitionMethod() { return acquisitionMethod; } public void setAcquisitionMethod(AcquisitionMethod value) { acquisitionMethod = value; }
    public ConditionAtAcquisition getConditionAtAcquisition() { return conditionAtAcquisition; } public void setConditionAtAcquisition(ConditionAtAcquisition value) { conditionAtAcquisition = value; }
    public LifecycleStatus getLifecycleStatus() { return lifecycleStatus; } public void setLifecycleStatus(LifecycleStatus value) { lifecycleStatus = value; }
    public LocalDate getWarrantyExpiresOn() { return warrantyExpiresOn; } public void setWarrantyExpiresOn(LocalDate value) { warrantyExpiresOn = value; }
    public ProviderServiceLocation getPreferredProviderServiceLocation() { return preferredProviderServiceLocation; } public void setPreferredProviderServiceLocation(ProviderServiceLocation value) { preferredProviderServiceLocation = value; }
    public LocalDate getReplacementDueOn() { return replacementDueOn; } public void setReplacementDueOn(LocalDate value) { replacementDueOn = value; }
    public LocalDate getReassessmentDueOn() { return reassessmentDueOn; } public void setReassessmentDueOn(LocalDate value) { reassessmentDueOn = value; }
    public LifecycleRecord getReplacesLifecycleRecord() { return replacesLifecycleRecord; } public void setReplacesLifecycleRecord(LifecycleRecord value) { replacesLifecycleRecord = value; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
