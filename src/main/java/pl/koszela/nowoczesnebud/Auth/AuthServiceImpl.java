package pl.koszela.nowoczesnebud.Auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.DTO.AuthResponse;
import pl.koszela.nowoczesnebud.DTO.ForgotPasswordRequest;
import pl.koszela.nowoczesnebud.DTO.GoogleLoginRequest;
import pl.koszela.nowoczesnebud.DTO.LoginRequest;
import pl.koszela.nowoczesnebud.DTO.RegisterRequest;
import pl.koszela.nowoczesnebud.DTO.ResetPasswordRequest;
import pl.koszela.nowoczesnebud.DTO.ResendVerificationCodeRequest;
import pl.koszela.nowoczesnebud.DTO.UserProfileResponse;
import pl.koszela.nowoczesnebud.DTO.VerifyEmailRequest;
import pl.koszela.nowoczesnebud.Model.AppUser;
import pl.koszela.nowoczesnebud.Model.RefreshToken;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;
import pl.koszela.nowoczesnebud.Repository.RefreshTokenRepository;
import pl.koszela.nowoczesnebud.Security.JwtService;
import pl.koszela.nowoczesnebud.Security.UserDetailsServiceImpl;
import pl.koszela.nowoczesnebud.Exception.UnauthorizedRefreshTokenException;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthServiceImpl(AppUserRepository appUserRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           UserDetailsServiceImpl userDetailsService,
                           JwtService jwtService,
                           GoogleTokenVerifierService googleTokenVerifierService,
                           EmailVerificationService emailVerificationService,
                           PasswordResetService passwordResetService) {
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    @Override
    @Transactional
    public AppUser register(RegisterRequest request) {
        String normalizedUsername = request.getUsername().trim();
        String normalizedEmail = normalizeLoginIdentifier(request.getEmail());

        if (appUserRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Login jest już zajęty");
        }
        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email jest już zajęty");
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setEmailVerified(false);
        AppUser savedUser = appUserRepository.save(user);
        emailVerificationService.sendRegistrationCode(savedUser);
        return savedUser;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String loginIdentifier = normalizeLoginIdentifier(request.getEmail());
        AppUser user = findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new BadCredentialsException("Niepoprawny email lub hasło"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadCredentialsException("Konto jest nieaktywne");
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Najpierw zweryfikuj email kodem z wiadomości.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
            );
        } catch (Exception ex) {
            throw new BadCredentialsException("Niepoprawny email lub hasło");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser authenticatedUser = appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));
        return issueTokensForUser(authenticatedUser, Boolean.TRUE.equals(request.getRememberMe()));
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        UserProfileResponse googleProfile = googleTokenVerifierService.verifyAndExtractProfile(request.getIdToken());
        AppUser user = appUserRepository.findByEmailIgnoreCase(googleProfile.getEmail())
                .orElseGet(() -> createGoogleUser(googleProfile));
        return issueTokensForUser(user, Boolean.TRUE.equals(request.getRememberMe()));
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
    }

    @Override
    @Transactional
    public void resendVerificationCode(ResendVerificationCodeRequest request) {
        emailVerificationService.resendCode(request.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        passwordResetService.sendResetCode(request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedRefreshTokenException("Brak refresh tokenu");
        }

        RefreshToken tokenEntity = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new UnauthorizedRefreshTokenException("Nieprawidłowy refresh token"));

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenEntity.setRevoked(true);
            refreshTokenRepository.save(tokenEntity);
            throw new UnauthorizedRefreshTokenException("Refresh token wygasł");
        }

        AppUser user = tokenEntity.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return new AuthResponse(
                newAccessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                user.getUsername(),
                user.getEmail(),
                null,
                0
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    public long getRefreshTokenExpirationMs() {
        return jwtService.getRefreshTokenExpirationMs();
    }

    private AuthResponse issueTokensForUser(AppUser user, boolean rememberMe) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        long refreshTtlMs = rememberMe
                ? jwtService.getRememberRefreshTokenExpirationMs()
                : jwtService.getRefreshTokenExpirationMs();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails, refreshTtlMs);

        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);

        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken(refreshToken);
        tokenEntity.setUser(user);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTtlMs / 1000));
        tokenEntity.setRevoked(false);
        refreshTokenRepository.save(tokenEntity);

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                user.getUsername(),
                user.getEmail(),
                refreshToken,
                refreshTtlMs
        );
    }

    private AppUser createGoogleUser(UserProfileResponse googleProfile) {
        String baseUsername = sanitizeUsername(googleProfile.getUsername());
        String uniqueUsername = buildUniqueUsername(baseUsername);

        AppUser user = new AppUser();
        user.setUsername(uniqueUsername);
        user.setEmail(googleProfile.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setEmailVerified(true);
        return appUserRepository.save(user);
    }

    private String sanitizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "googleuser";
        }
        String sanitized = username.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (!sanitized.isBlank()) {
            return sanitized;
        }
        return "googleuser";
    }

    private String buildUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int suffix = 1;
        while (candidate.isBlank() || appUserRepository.existsByUsername(candidate)) {
            candidate = baseUsername + suffix;
            suffix++;
        }
        return candidate;
    }

    private java.util.Optional<AppUser> findByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return java.util.Optional.empty();
        }
        return appUserRepository.findByEmailIgnoreCase(identifier)
                .or(() -> appUserRepository.findByUsernameIgnoreCase(identifier));
    }

    private String normalizeLoginIdentifier(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
