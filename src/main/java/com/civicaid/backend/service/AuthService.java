package com.civicaid.backend.service;

import com.civicaid.backend.dto.AuthResponse;
import com.civicaid.backend.dto.LoginRequest;
import com.civicaid.backend.dto.RegisterRequest;
import com.civicaid.backend.dto.UserProfileResponse;
import com.civicaid.backend.dto.UserProfileUpdateRequest;
import com.civicaid.backend.entity.AccountStatus;
import com.civicaid.backend.entity.Role;
import com.civicaid.backend.entity.RoleName;
import com.civicaid.backend.entity.User;
import com.civicaid.backend.exception.BadRequestException;
import com.civicaid.backend.exception.ConflictException;
import com.civicaid.backend.repository.AuditLogRepository;
import com.civicaid.backend.repository.LifecycleRecordRepository;
import com.civicaid.backend.repository.RoleRepository;
import com.civicaid.backend.repository.SavedResourceRepository;
import com.civicaid.backend.repository.UserRepository;
import com.civicaid.backend.repository.VerificationRecordRepository;
import com.civicaid.backend.security.AuthenticatedUser;
import com.civicaid.backend.security.JwtService;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final LifecycleRecordRepository lifecycleRecordRepository;
    private final SavedResourceRepository savedResourceRepository;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            VerificationRecordRepository verificationRecordRepository,
            AuditLogRepository auditLogRepository,
            LifecycleRecordRepository lifecycleRecordRepository,
            SavedResourceRepository savedResourceRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.verificationRecordRepository = verificationRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.savedResourceRepository = savedResourceRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();

        user.setEmail(normalizedEmail);
        user.setDisplayName(
                request.displayName() == null
                        ? null
                        : request.displayName().trim());
        user.setPasswordHash(
                passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.ACTIVE);

        Role role = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.USER);
                    return roleRepository.save(newRole);
                });

        user.setRoles(java.util.Set.of(role));

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved);

        return new AuthResponse(
                token,
                new AuthResponse.UserSummary(
                        saved.getEmail(),
                        saved.getDisplayName()));
    }

    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    request.password()));

            AuthenticatedUser authenticatedUser =
                    (AuthenticatedUser) authentication.getPrincipal();

            User user = authenticatedUser.getUser();

            if (user.getAccountStatus() == AccountStatus.DISABLED) {
                throw new BadRequestException("Account is disabled");
            }

            String token = jwtService.generateToken(user);

            return new AuthResponse(
                    token,
                    new AuthResponse.UserSummary(
                            user.getEmail(),
                            user.getDisplayName()));

        } catch (BadCredentialsException ex) {
            throw ex;
        }
    }

    public UserProfileResponse getCurrentUserProfile(
            AuthenticatedUser principal) {

        User user = principal.getUser();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAccountStatus().name());
    }

    /**
     * Updates the authenticated user's display name.
     * Email and account status cannot be changed by the user.
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(
            AuthenticatedUser principal,
            UserProfileUpdateRequest request) {

        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new BadRequestException("User account not found"));

        user.setDisplayName(request.displayName().trim());

        User saved = userRepository.save(user);

        return new UserProfileResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getDisplayName(),
                saved.getAccountStatus().name());
    }

    /**
     * Permanently deletes the authenticated user's account and all personally
     * owned data (functional needs, saved resources, lifecycle/maintenance/repair
     * records).
     *
     * Rules.md §5 + §9: deletion is refused if the user is referenced in any
     * verification record or audit log, because those records are immutable
     * history and the FK is ON DELETE RESTRICT.
     */
    @Transactional
    public void deleteCurrentUserAccount(AuthenticatedUser principal) {

        UUID userId = principal.getUser().getId();

        // Check immutable history references — cannot delete if referenced.
        if (verificationRecordRepository.existsByVerifierUser_Id(userId)) {
            throw new ConflictException(
                    "Your account cannot be deleted because it is referenced in "
                    + "verification records. Contact an administrator to proceed.");
        }

        if (auditLogRepository.existsByActorUser_Id(userId)) {
            throw new ConflictException(
                    "Your account cannot be deleted because it is referenced in "
                    + "audit history. Contact an administrator to proceed.");
        }

        // Clear self-referencing lifecycle replacement links before deleting
        // lifecycle records to avoid ON DELETE RESTRICT violations from
        // fk_lifecycle_records_replaces.
        lifecycleRecordRepository.clearReplacementsByUserId(userId);

        // Delete lifecycle records (maintenance + repair records cascade via FK).
        lifecycleRecordRepository.deleteByUser_Id(userId);

        // Delete saved resources.
        savedResourceRepository.deleteByUser_Id(userId);

        // Delete the user row (user_functional_needs and user_roles cascade).
        userRepository.deleteById(userId);
    }

    private String normalizeEmail(String email) {

        if (email == null) {
            throw new BadRequestException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}