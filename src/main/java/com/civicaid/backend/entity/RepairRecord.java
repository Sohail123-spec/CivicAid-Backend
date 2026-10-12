package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "repair_records")
public class RepairRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lifecycle_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_repair_records_lifecycle_record"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LifecycleRecord lifecycleRecord;

    @NotNull
    @PastOrPresent
    @Column(name = "repair_date", nullable = false)
    private LocalDate repairDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_service_location_id",
            foreignKey = @ForeignKey(
                    name = "fk_repair_records_provider_service_location"))
    private ProviderServiceLocation providerServiceLocation;

    @NotBlank
    @Size(max = 1000)
    @Column(
            name = "issue_description",
            nullable = false,
            length = 1000)
    private String issueDescription;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "repair_outcome_status",
            nullable = false,
            length = 24)
    private RepairOutcomeStatus repairOutcomeStatus;

    @Size(max = 1000)
    @Column(name = "work_summary", length = 1000)
    private String workSummary;

    @DecimalMin("0.00")
    @Column(
            name = "cost_inr",
            precision = 12,
            scale = 2)
    private BigDecimal costInr;

    @Column(name = "warranty_used")
    private Boolean warrantyUsed;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false)
    private Instant updatedAt;

    public RepairRecord() {
    }

    public UUID getId() {
        return id;
    }

    public LifecycleRecord getLifecycleRecord() {
        return lifecycleRecord;
    }

    public void setLifecycleRecord(LifecycleRecord value) {
        lifecycleRecord = value;
    }

    public LocalDate getRepairDate() {
        return repairDate;
    }

    public void setRepairDate(LocalDate value) {
        repairDate = value;
    }

    public ProviderServiceLocation getProviderServiceLocation() {
        return providerServiceLocation;
    }

    public void setProviderServiceLocation(
            ProviderServiceLocation value) {

        providerServiceLocation = value;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String value) {
        issueDescription = value;
    }

    public RepairOutcomeStatus getRepairOutcomeStatus() {
        return repairOutcomeStatus;
    }

    public void setRepairOutcomeStatus(
            RepairOutcomeStatus value) {

        repairOutcomeStatus = value;
    }

    public String getWorkSummary() {
        return workSummary;
    }

    public void setWorkSummary(String value) {
        workSummary = value;
    }

    public BigDecimal getCostInr() {
        return costInr;
    }

    public void setCostInr(BigDecimal value) {
        costInr = value;
    }

    public Boolean getWarrantyUsed() {
        return warrantyUsed;
    }

    public void setWarrantyUsed(Boolean value) {
        warrantyUsed = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}