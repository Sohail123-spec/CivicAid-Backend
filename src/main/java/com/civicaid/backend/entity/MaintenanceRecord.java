package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lifecycle_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_maintenance_records_lifecycle_record"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LifecycleRecord lifecycleRecord;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 16)
    private MaintenanceType maintenanceType;

    @NotNull
    @PastOrPresent
    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_service_location_id",
            foreignKey = @ForeignKey(
                    name = "fk_maintenance_records_provider_service_location"))
    private ProviderServiceLocation providerServiceLocation;

    @Size(max = 1000)
    @Column(length = 1000)
    private String details;

    @Column(name = "next_maintenance_due_on")
    private LocalDate nextMaintenanceDueOn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MaintenanceRecord() {
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

    public MaintenanceType getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(MaintenanceType value) {
        maintenanceType = value;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate value) {
        maintenanceDate = value;
    }

    public ProviderServiceLocation getProviderServiceLocation() {
        return providerServiceLocation;
    }

    public void setProviderServiceLocation(ProviderServiceLocation value) {
        providerServiceLocation = value;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String value) {
        details = value;
    }

    public LocalDate getNextMaintenanceDueOn() {
        return nextMaintenanceDueOn;
    }

    public void setNextMaintenanceDueOn(LocalDate value) {
        nextMaintenanceDueOn = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}