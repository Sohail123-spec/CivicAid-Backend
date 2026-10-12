package com.civicaid.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "verification_records")
public class VerificationRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "verifier_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_verification_records_verifier")) private User verifierUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "at_category_id", foreignKey = @ForeignKey(name = "fk_verification_records_at_category")) private ATCategory atCategory;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "at_product_id", foreignKey = @ForeignKey(name = "fk_verification_records_at_product")) private ATProduct atProduct;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scheme_id", foreignKey = @ForeignKey(name = "fk_verification_records_scheme")) private Scheme scheme;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", foreignKey = @ForeignKey(name = "fk_verification_records_provider")) private Provider provider;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_location_id", foreignKey = @ForeignKey(name = "fk_verification_records_provider_location")) private ProviderLocation providerLocation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_service_id", foreignKey = @ForeignKey(name = "fk_verification_records_provider_service")) private ProviderService providerService;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_service_location_id", foreignKey = @ForeignKey(name = "fk_verification_records_provider_service_location")) private ProviderServiceLocation providerServiceLocation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scheme_product_id", foreignKey = @ForeignKey(name = "fk_verification_records_scheme_product")) private SchemeProduct schemeProduct;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_service_product_id", foreignKey = @ForeignKey(name = "fk_verification_records_provider_service_product")) private ProviderServiceProduct providerServiceProduct;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "verification_result", nullable = false, length = 16) private VerificationResult verificationResult;
    @NotBlank @Size(max = 200) @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Pattern(regexp = "^https://\\S+$") @Size(max = 2048) @Column(name = "source_url", length = 2048) private String sourceUrl;
    @Size(max = 100) @Column(name = "source_version", length = 100) private String sourceVersion;
    @Size(max = 1000) @Column(name = "review_notes", length = 1000) private String reviewNotes;
    @Column(name = "performed_at", nullable = false, updatable = false) private Instant performedAt;
    public VerificationRecord() { }
    public UUID getId() { return id; } public User getVerifierUser() { return verifierUser; } public void setVerifierUser(User value) { verifierUser = value; }
    public ATCategory getAtCategory() { return atCategory; } public void setAtCategory(ATCategory value) { atCategory = value; }
    public ATProduct getAtProduct() { return atProduct; } public void setAtProduct(ATProduct value) { atProduct = value; }
    public Scheme getScheme() { return scheme; } public void setScheme(Scheme value) { scheme = value; }
    public Provider getProvider() { return provider; } public void setProvider(Provider value) { provider = value; }
    public ProviderLocation getProviderLocation() { return providerLocation; } public void setProviderLocation(ProviderLocation value) { providerLocation = value; }
    public ProviderService getProviderService() { return providerService; } public void setProviderService(ProviderService value) { providerService = value; }
    public ProviderServiceLocation getProviderServiceLocation() { return providerServiceLocation; } public void setProviderServiceLocation(ProviderServiceLocation value) { providerServiceLocation = value; }
    public SchemeProduct getSchemeProduct() { return schemeProduct; } public void setSchemeProduct(SchemeProduct value) { schemeProduct = value; }
    public ProviderServiceProduct getProviderServiceProduct() { return providerServiceProduct; } public void setProviderServiceProduct(ProviderServiceProduct value) { providerServiceProduct = value; }
    public VerificationResult getVerificationResult() { return verificationResult; } public void setVerificationResult(VerificationResult value) { verificationResult = value; }
    public String getSourceName() { return sourceName; } public void setSourceName(String value) { sourceName = value; }
    public String getSourceUrl() { return sourceUrl; } public void setSourceUrl(String value) { sourceUrl = value; }
    public String getSourceVersion() { return sourceVersion; } public void setSourceVersion(String value) { sourceVersion = value; }
    public String getReviewNotes() { return reviewNotes; } public void setReviewNotes(String value) { reviewNotes = value; }
    public Instant getPerformedAt() { return performedAt; } public void setPerformedAt(Instant value) { performedAt = value; }
}
