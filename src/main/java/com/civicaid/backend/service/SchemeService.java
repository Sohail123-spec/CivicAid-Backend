package com.civicaid.backend.service;

import com.civicaid.backend.dto.SchemeCreateRequest;
import com.civicaid.backend.dto.SchemeResponse;
import com.civicaid.backend.dto.SchemeUpdateRequest;
import com.civicaid.backend.entity.AuditActionType;
import com.civicaid.backend.entity.AuditTargetType;
import com.civicaid.backend.entity.JurisdictionLevel;
import com.civicaid.backend.entity.Scheme;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.entity.VerificationStatus;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.exception.ResourceNotFoundException;
import com.civicaid.backend.repository.SchemeRepository;
import com.civicaid.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.civicaid.backend.dto.SchemeVerificationRequest;
import com.civicaid.backend.dto.SchemeVerificationResponse;
import com.civicaid.backend.entity.VerificationRecord;
import com.civicaid.backend.entity.VerificationResult;
import com.civicaid.backend.repository.VerificationRecordRepository;
import java.time.Instant;

@Service

public class SchemeService {

    private static final int VERIFICATION_FRESHNESS_DAYS = 90;

    private final SchemeRepository schemeRepository;

    private final UserRepository userRepository;

    private final AuditLogService auditLogService;

    private final VerificationRecordRepository verificationRecordRepository;

    public SchemeService(

        SchemeRepository schemeRepository,

        UserRepository userRepository,

        AuditLogService auditLogService,

        VerificationRecordRepository verificationRecordRepository) {

    this.schemeRepository = schemeRepository;

    this.userRepository = userRepository;

    this.auditLogService = auditLogService;

    this.verificationRecordRepository = verificationRecordRepository;

}

    @Transactional(readOnly = true)

    public List<SchemeResponse> getPublicSchemes(String stateCode) {

        LocalDate today = LocalDate.now();

        LocalDate cutoff = today.minusDays(VERIFICATION_FRESHNESS_DAYS);

        List<Scheme> schemes;

        if (stateCode == null || stateCode.isBlank()) {

            schemes = schemeRepository.findPublicNationalSchemes(

                    VerificationStatus.VERIFIED,

                    cutoff,

                    today,

                    JurisdictionLevel.NATIONAL);

        } else {

            String normalizedStateCode = stateCode.trim().toUpperCase();

            if (!normalizedStateCode.matches("^[A-Z]{2}$")) {

                throw new BadRequestException(

                        "State code must contain two uppercase letters");

            }

            schemes = schemeRepository.findPublicSchemesForState(

                    VerificationStatus.VERIFIED,

                    cutoff,

                    today,

                    JurisdictionLevel.NATIONAL,

                    JurisdictionLevel.STATE,

                    normalizedStateCode);

        }

        return schemes.stream()

                .map((Scheme scheme) -> toResponse(scheme))

                .toList();

    }

    @Transactional(readOnly = true)

    public SchemeResponse getPublicScheme(UUID schemeId, String stateCode) {

        Scheme scheme = schemeRepository.findById(schemeId)

                .orElseThrow(() ->

                        new ResourceNotFoundException("Scheme not found"));

        LocalDate today = LocalDate.now();

        LocalDate cutoff = today.minusDays(VERIFICATION_FRESHNESS_DAYS);

        boolean stateMatches;

        if (scheme.getJurisdictionLevel() == JurisdictionLevel.NATIONAL) {

            stateMatches = true;

        } else if (stateCode == null || stateCode.isBlank()) {

            stateMatches = false;

        } else {

            String normalizedStateCode = stateCode.trim().toUpperCase();

            if (!normalizedStateCode.matches("^[A-Z]{2}$")) {

                throw new BadRequestException(

                        "State code must contain two uppercase letters");

            }

            stateMatches = normalizedStateCode.equals(

                    scheme.getApplicableStateCode());

        }

        boolean currentlyEffective =

                (scheme.getEffectiveFrom() == null

                        || !scheme.getEffectiveFrom().isAfter(today))

                && (scheme.getEffectiveTo() == null

                        || !scheme.getEffectiveTo().isBefore(today));

        boolean currentlyVerified = 
        
                scheme.getVerificationStatus() == VerificationStatus.VERIFIED

                && scheme.getLastVerifiedAt() != null

                && !scheme.getLastVerifiedAt().isBefore(cutoff)

                && !scheme.getLastVerifiedAt().isAfter(today);

        if (!scheme.isActive()

                || !currentlyVerified

                || !currentlyEffective

                || !stateMatches) {

            throw new ResourceNotFoundException("Scheme not found");

        }

        return toResponse(scheme);

    }

    @Transactional(readOnly = true)

    public List<SchemeResponse> getAllSchemesForAdmin() {

        return schemeRepository.findAll()

                .stream()

                .map(this::toResponse)

                .toList();

    }

    @Transactional(readOnly = true)

    public SchemeResponse getSchemeForAdmin(UUID schemeId) {

        return toResponse(requireScheme(schemeId));

    }

    @Transactional

    public SchemeResponse createScheme(

            SchemeCreateRequest request,

            String actorEmail) {

        User actor = requireActor(actorEmail);

        String code = request.code().trim();

        String name = request.name().trim();

        String stateCode = normalizeOptional(request.applicableStateCode());

        validateJurisdiction(request.jurisdictionLevel(), stateCode);

        validateEffectiveDates(

                request.effectiveFrom(), request.effectiveTo());

        if (schemeRepository.existsByCodeIgnoreCase(code)) {

            throw new ConflictException("Scheme code already exists");

        }

        if (schemeRepository

                .existsByNameIgnoreCaseAndJurisdictionLevelAndApplicableStateCode(

                        name, request.jurisdictionLevel(), stateCode)) {

            throw new ConflictException(

                    "Scheme name already exists in this jurisdiction");

        }

        Scheme scheme = new Scheme();

        scheme.setCode(code);

        applyRequestFields(

                scheme,

                request.name(),

                request.authorityName(),

                request.description(),

                request.benefitsSummary(),

                request.eligibilitySummary(),

                request.requiredDocumentsSummary(),

                request.applicationMethod(),

                request.applicationInstructions(),

                request.officialSourceUrl(),

                request.officialApplicationUrl(),

                request.jurisdictionLevel(),

                stateCode,

                request.sourceVersion(),

                request.effectiveFrom(),

                request.effectiveTo());

        scheme.setVerificationStatus(VerificationStatus.UNVERIFIED);

        scheme.setLastVerifiedAt(null);

        scheme.setActive(true);

        scheme = schemeRepository.save(scheme);

        auditLogService.record(

                actor,

                AuditTargetType.SCHEME,

                scheme.getId(),

                AuditActionType.CREATE,

                new String[] {

                    "code", "name", "authority_name", "description",

                    "benefits_summary", "eligibility_summary",

                    "required_documents_summary", "application_method",

                    "application_instructions", "official_source_url",

                    "official_application_url", "jurisdiction_level",

                    "applicable_state_code", "source_version",

                    "effective_from", "effective_to",

                    "verification_status", "active"

                },

                "Scheme created");

        return toResponse(scheme);

    }

    @Transactional

    public SchemeResponse updateScheme(

            UUID schemeId,

            SchemeUpdateRequest request,

            String actorEmail) {

        User actor = requireActor(actorEmail);

        Scheme scheme = requireScheme(schemeId);

        String name = request.name().trim();

        String stateCode = normalizeOptional(request.applicableStateCode());

        validateJurisdiction(request.jurisdictionLevel(), stateCode);

        validateEffectiveDates(

                request.effectiveFrom(), request.effectiveTo());

        if (schemeRepository

                .existsByNameIgnoreCaseAndJurisdictionLevelAndApplicableStateCode(

                        name,

                        request.jurisdictionLevel(),

                        stateCode)

                && (!Objects.equals(scheme.getName(), name)

                    || scheme.getJurisdictionLevel()

                            != request.jurisdictionLevel()

                    || !Objects.equals(

                            normalizeOptional(scheme.getApplicableStateCode()),

                            stateCode))) {

            throw new ConflictException(

                    "Scheme name already exists in this jurisdiction");

        }

        List<String> changedFields = new ArrayList<>();

        String authorityName = request.authorityName().trim();

        String description = request.description().trim();

        String benefitsSummary = request.benefitsSummary().trim();

        String eligibilitySummary = request.eligibilitySummary().trim();

        String requiredDocumentsSummary =

                normalizeOptional(request.requiredDocumentsSummary());

        String applicationInstructions =

                request.applicationInstructions().trim();

        String officialSourceUrl = request.officialSourceUrl().trim();

        String officialApplicationUrl =

                normalizeOptional(request.officialApplicationUrl());

        String sourceVersion = normalizeOptional(request.sourceVersion());

        addIfChanged(changedFields, "name", scheme.getName(), name);

        addIfChanged(

                changedFields, "authority_name",

                scheme.getAuthorityName(), authorityName);

        addIfChanged(

                changedFields, "description",

                scheme.getDescription(), description);

        addIfChanged(

                changedFields, "benefits_summary",

                scheme.getBenefitsSummary(), benefitsSummary);

        addIfChanged(

                changedFields, "eligibility_summary",

                scheme.getEligibilitySummary(), eligibilitySummary);

        addIfChanged(

                changedFields, "required_documents_summary",

                scheme.getRequiredDocumentsSummary(),

                requiredDocumentsSummary);

        addIfChanged(

                changedFields, "application_method",

                scheme.getApplicationMethod(), request.applicationMethod());

        addIfChanged(

                changedFields, "application_instructions",

                scheme.getApplicationInstructions(),

                applicationInstructions);

        addIfChanged(

                changedFields, "official_source_url",

                scheme.getOfficialSourceUrl(), officialSourceUrl);

        addIfChanged(

                changedFields, "official_application_url",

                scheme.getOfficialApplicationUrl(), officialApplicationUrl);

        addIfChanged(

                changedFields, "jurisdiction_level",

                scheme.getJurisdictionLevel(), request.jurisdictionLevel());

        addIfChanged(

                changedFields, "applicable_state_code",

                normalizeOptional(scheme.getApplicableStateCode()), stateCode);

        addIfChanged(

                changedFields, "source_version",

                scheme.getSourceVersion(), sourceVersion);

        addIfChanged(

                changedFields, "effective_from",

                scheme.getEffectiveFrom(), request.effectiveFrom());

        addIfChanged(

                changedFields, "effective_to",

                scheme.getEffectiveTo(), request.effectiveTo());

        boolean substantiveChange = !changedFields.isEmpty();

        if (substantiveChange) {

            addIfChanged(

                    changedFields, "verification_status",

                    scheme.getVerificationStatus(),

                    VerificationStatus.UNVERIFIED);

            addIfChanged(

                    changedFields, "last_verified_at",

                    scheme.getLastVerifiedAt(), null);

            scheme.setVerificationStatus(VerificationStatus.UNVERIFIED);

            scheme.setLastVerifiedAt(null);

        }

        applyRequestFields(

                scheme,

                name,

                authorityName,

                description,

                benefitsSummary,

                eligibilitySummary,

                requiredDocumentsSummary,

                request.applicationMethod(),

                applicationInstructions,

                officialSourceUrl,

                officialApplicationUrl,

                request.jurisdictionLevel(),

                stateCode,

                sourceVersion,

                request.effectiveFrom(),

                request.effectiveTo());

        scheme = schemeRepository.save(scheme);

        if (!changedFields.isEmpty()) {

            auditLogService.record(

                    actor,

                    AuditTargetType.SCHEME,

                    scheme.getId(),

                    AuditActionType.UPDATE,

                    changedFields.toArray(String[]::new),

                    "Scheme updated");

        }

        return toResponse(scheme);

    }

    @Transactional

    public SchemeResponse setSchemeActive(

            UUID schemeId,

            boolean active,

            String actorEmail) {

        User actor = requireActor(actorEmail);

        Scheme scheme = requireScheme(schemeId);

        if (scheme.isActive() == active) {

            return toResponse(scheme);

        }

        scheme.setActive(active);

        scheme = schemeRepository.save(scheme);

        auditLogService.record(

                actor,

                AuditTargetType.SCHEME,

                scheme.getId(),

                active ? AuditActionType.ACTIVATE : AuditActionType.ARCHIVE,

                new String[] {"active"},

                active ? "Scheme activated" : "Scheme archived");

        return toResponse(scheme);

    }

    @Transactional

    public SchemeVerificationResponse verifyScheme(

            UUID schemeId,

            SchemeVerificationRequest request,

            String actorEmail) {

        User actor = requireActor(actorEmail);

        Scheme scheme = requireScheme(schemeId);

        String sourceName = request.sourceName().trim();

        String sourceUrl = normalizeOptional(request.sourceUrl());

        String sourceVersion = normalizeOptional(request.sourceVersion());

        String reviewNotes = normalizeOptional(request.reviewNotes());

        if (request.result() != VerificationResult.VERIFIED

                && reviewNotes == null) {

            throw new BadRequestException(

                    "Review notes are required when verification is rejected or revoked");

        }

        List<String> changedFields = new ArrayList<>();

        VerificationStatus resultingStatus =

                request.result() == VerificationResult.VERIFIED

                        ? VerificationStatus.VERIFIED

                        : VerificationStatus.UNVERIFIED;

        LocalDate resultingVerifiedDate =

                request.result() == VerificationResult.VERIFIED

                        ? LocalDate.now()

                        : null;

        addIfChanged(

                changedFields,

                "verification_status",

                scheme.getVerificationStatus(),

                resultingStatus);

        addIfChanged(

                changedFields,

                "last_verified_at",

                scheme.getLastVerifiedAt(),

                resultingVerifiedDate);

        if (sourceUrl != null) {

            addIfChanged(

                    changedFields,

                    "official_source_url",

                    scheme.getOfficialSourceUrl(),

                    sourceUrl);

            scheme.setOfficialSourceUrl(sourceUrl);

        }

        if (sourceVersion != null) {

            addIfChanged(

                    changedFields,

                    "source_version",

                    scheme.getSourceVersion(),

                    sourceVersion);

            scheme.setSourceVersion(sourceVersion);

        }

        changedFields.add("verification_result");

        if (sourceVersion != null) {

            changedFields.add("verification_source_version");

        }

        if (reviewNotes != null) {

            changedFields.add("review_notes");

        }

        scheme.setVerificationStatus(resultingStatus);

        scheme.setLastVerifiedAt(resultingVerifiedDate);

        scheme = schemeRepository.save(scheme);

        VerificationRecord record = new VerificationRecord();

        record.setVerifierUser(actor);

        record.setScheme(scheme);

        record.setVerificationResult(request.result());

        record.setSourceName(sourceName);

        record.setSourceUrl(sourceUrl);

        record.setSourceVersion(sourceVersion);

        record.setReviewNotes(reviewNotes);

        record.setPerformedAt(Instant.now());

        record = verificationRecordRepository.save(record);

        AuditActionType actionType = switch (request.result()) {

            case VERIFIED -> AuditActionType.VERIFY;

            case REJECTED -> AuditActionType.REJECT_VERIFICATION;

            case REVOKED -> AuditActionType.REVOKE_VERIFICATION;

        };

        auditLogService.record(

                actor,

                AuditTargetType.SCHEME,

                scheme.getId(),

                actionType,

                changedFields.toArray(String[]::new),

                "Scheme verification recorded: " + request.result().name());

        return toVerificationResponse(record);

    }

    @Transactional(readOnly = true)

    public List<SchemeVerificationResponse> getVerificationHistory(

            UUID schemeId) {

        if (!schemeRepository.existsById(schemeId)) {

            throw new ResourceNotFoundException("Scheme not found");

        }

        List<VerificationRecord> records =

                verificationRecordRepository.findByScheme_IdOrderByPerformedAtDesc(schemeId);

        return records.stream()

                .map((VerificationRecord record) -> toVerificationResponse(record))

                .toList();

    }

    private SchemeVerificationResponse toVerificationResponse(

            VerificationRecord record) {

        return new SchemeVerificationResponse(

                record.getId(),

                record.getVerifierUser().getId(),

                record.getVerificationResult(),

                record.getSourceName(),

                record.getSourceUrl(),

                record.getSourceVersion(),

                record.getReviewNotes(),

                record.getPerformedAt());

    }

    private void applyRequestFields(

            Scheme scheme,

            String name,

            String authorityName,

            String description,

            String benefitsSummary,

            String eligibilitySummary,

            String requiredDocumentsSummary,

            com.civicaid.backend.entity.ApplicationMethod applicationMethod,

            String applicationInstructions,

            String officialSourceUrl,

            String officialApplicationUrl,

            JurisdictionLevel jurisdictionLevel,

            String applicableStateCode,

            String sourceVersion,

            LocalDate effectiveFrom,

            LocalDate effectiveTo) {

        scheme.setName(name.trim());

        scheme.setAuthorityName(authorityName.trim());

        scheme.setDescription(description.trim());

        scheme.setBenefitsSummary(benefitsSummary.trim());

        scheme.setEligibilitySummary(eligibilitySummary.trim());

        scheme.setRequiredDocumentsSummary(

                normalizeOptional(requiredDocumentsSummary));

        scheme.setApplicationMethod(applicationMethod);

        scheme.setApplicationInstructions(applicationInstructions.trim());

        scheme.setOfficialSourceUrl(officialSourceUrl.trim());

        scheme.setOfficialApplicationUrl(

                normalizeOptional(officialApplicationUrl));

        scheme.setJurisdictionLevel(jurisdictionLevel);

        scheme.setApplicableStateCode(

                normalizeOptional(applicableStateCode));

        scheme.setSourceVersion(normalizeOptional(sourceVersion));

        scheme.setEffectiveFrom(effectiveFrom);

        scheme.setEffectiveTo(effectiveTo);

    }

    private void validateJurisdiction(

            JurisdictionLevel jurisdictionLevel,

            String stateCode) {

        if (jurisdictionLevel == JurisdictionLevel.NATIONAL

                && stateCode != null) {

            throw new BadRequestException(

                    "National schemes must not have a state code");

        }

        if (jurisdictionLevel == JurisdictionLevel.STATE

                && (stateCode == null

                    || !stateCode.matches("^[A-Z]{2}$"))) {

            throw new BadRequestException(

                    "State schemes require a two-letter uppercase state code");

        }

    }

    private void validateEffectiveDates(

            LocalDate effectiveFrom,

            LocalDate effectiveTo) {

        if (effectiveFrom != null

                && effectiveTo != null

                && effectiveTo.isBefore(effectiveFrom)) {

            throw new BadRequestException(

                    "Effective-to date cannot be before effective-from date");

        }

    }

    private Scheme requireScheme(UUID schemeId) {

        return schemeRepository.findById(schemeId)

                .orElseThrow(() ->

                        new ResourceNotFoundException("Scheme not found"));

    }

    private User requireActor(String actorEmail) {

        return userRepository.findByEmail(actorEmail)

                .orElseThrow(() -> new IllegalStateException(

                        "Authenticated admin account was not found"));

    }

    private void addIfChanged(

            List<String> fields,

            String field,

            Object oldValue,

            Object newValue) {

        if (!Objects.equals(oldValue, newValue)) {

            fields.add(field);

        }

    }

    private String normalizeOptional(String value) {

        if (value == null) {

            return null;

        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;

    }

    private SchemeResponse toResponse(Scheme scheme) {

        return new SchemeResponse(

                scheme.getId(),

                scheme.getCode(),

                scheme.getName(),

                scheme.getAuthorityName(),

                scheme.getDescription(),

                scheme.getBenefitsSummary(),

                scheme.getEligibilitySummary(),

                scheme.getRequiredDocumentsSummary(),

                scheme.getApplicationMethod(),

                scheme.getApplicationInstructions(),

                scheme.getOfficialSourceUrl(),

                scheme.getOfficialApplicationUrl(),

                scheme.getJurisdictionLevel(),

                scheme.getApplicableStateCode(),

                scheme.getSourceVersion(),

                scheme.getEffectiveFrom(),

                scheme.getEffectiveTo(),

                scheme.getVerificationStatus(),

                scheme.getLastVerifiedAt(),

                scheme.isActive(),

                scheme.getCreatedAt(),

                scheme.getUpdatedAt());

    }

}
