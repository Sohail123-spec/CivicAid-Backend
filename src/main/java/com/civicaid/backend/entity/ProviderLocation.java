package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.*;

@Entity
@Table(name = "provider_locations")
public class ProviderLocation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "provider_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_locations_provider")) private Provider provider;
    @NotBlank @Size(max = 250) @Column(name = "address_line_1", nullable = false, length = 250) private String addressLine1;
    @Size(max = 250) @Column(name = "address_line_2", length = 250) private String addressLine2;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100) private String locality;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100) private String city;
    @NotBlank @Pattern(regexp = "^[A-Z]{2}$") @JdbcTypeCode(Types.CHAR) @Column(name = "state_code", nullable = false, columnDefinition = "CHAR(2)") private String stateCode;
    @Pattern(regexp = "^[0-9]{6}$") @JdbcTypeCode(Types.CHAR) @Column(name = "postal_code", columnDefinition = "CHAR(6)") private String postalCode;
    @NotBlank @Pattern(regexp = "^IN$") @JdbcTypeCode(Types.CHAR) @Column(name = "country_code", nullable = false, columnDefinition = "CHAR(2)") private String countryCode = "IN";
    @DecimalMin("6.000000") @DecimalMax("38.000000") @Column(precision = 9, scale = 6) private BigDecimal latitude;
    @DecimalMin("68.000000") @DecimalMax("98.000000") @Column(precision = 9, scale = 6) private BigDecimal longitude;
    @Size(max = 30) @Column(name = "public_phone", length = 30) private String publicPhone;
    @Email @Size(max = 254) @Column(name = "public_email", length = 254) private String publicEmail;
    @Size(max = 1000) @Column(name = "accessibility_notes", length = 1000) private String accessibilityNotes;
    @NotBlank @Size(max = 200) @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Pattern(regexp = "^https://\\S+$") @Size(max = 2048) @Column(name = "source_url", length = 2048) private String sourceUrl;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 16) private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    @PastOrPresent @Column(name = "last_verified_at") private LocalDate lastVerifiedAt;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public ProviderLocation() { }
    public UUID getId() { return id; } public Provider getProvider() { return provider; } public void setProvider(Provider value) { provider = value; }
    public String getAddressLine1() { return addressLine1; } public void setAddressLine1(String value) { addressLine1 = value; }
    public String getAddressLine2() { return addressLine2; } public void setAddressLine2(String value) { addressLine2 = value; }
    public String getLocality() { return locality; } public void setLocality(String value) { locality = value; }
    public String getCity() { return city; } public void setCity(String value) { city = value; }
    public String getStateCode() { return stateCode; } public void setStateCode(String value) { stateCode = value; }
    public String getPostalCode() { return postalCode; } public void setPostalCode(String value) { postalCode = value; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String value) { countryCode = value; }
    public BigDecimal getLatitude() { return latitude; } public void setLatitude(BigDecimal value) { latitude = value; }
    public BigDecimal getLongitude() { return longitude; } public void setLongitude(BigDecimal value) { longitude = value; }
    public String getPublicPhone() { return publicPhone; } public void setPublicPhone(String value) { publicPhone = value; }
    public String getPublicEmail() { return publicEmail; } public void setPublicEmail(String value) { publicEmail = value; }
    public String getAccessibilityNotes() { return accessibilityNotes; } public void setAccessibilityNotes(String value) { accessibilityNotes = value; }
    public String getSourceName() { return sourceName; } public void setSourceName(String value) { sourceName = value; }
    public String getSourceUrl() { return sourceUrl; } public void setSourceUrl(String value) { sourceUrl = value; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; } public void setVerificationStatus(VerificationStatus value) { verificationStatus = value; }
    public LocalDate getLastVerifiedAt() { return lastVerifiedAt; } public void setLastVerifiedAt(LocalDate value) { lastVerifiedAt = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
