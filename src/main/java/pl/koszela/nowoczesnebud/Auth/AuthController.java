package pl.koszela.nowoczesnebud.Auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.DTO.AuthResponse;
import pl.koszela.nowoczesnebud.DTO.ForgotPasswordRequest;
import pl.koszela.nowoczesnebud.DTO.GoogleLoginRequest;
import pl.koszela.nowoczesnebud.DTO.LoginRequest;
import pl.koszela.nowoczesnebud.DTO.MessageResponse;
import pl.koszela.nowoczesnebud.DTO.RegisterRequest;
import pl.koszela.nowoczesnebud.DTO.ResetPasswordRequest;
import pl.koszela.nowoczesnebud.DTO.ResendVerificationCodeRequest;
import pl.koszela.nowoczesnebud.DTO.UserProfileResponse;
import pl.koszela.nowoczesnebud.DTO.VerifyEmailRequest;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.auth.cookie-samesite:Strict}")
    private String cookieSameSite;

    @Value("${app.auth.allowed-origins:http://localhost:4200}")
    private String allowedOriginsRaw;

    private Set<String> allowedOrigins = Collections.emptySet();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostConstruct
    public void initSecurityProperties() {
        this.cookieSameSite = normalizeSameSite(cookieSameSite);
        this.allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new MessageResponse("Rejestracja zakończona. Sprawdź email i wpisz kod weryfikacyjny."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(new MessageResponse("Email został zweryfikowany. Możesz się zalogować."));
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<MessageResponse> resendVerificationCode(@Valid @RequestBody ResendVerificationCodeRequest request) {
        authService.resendVerificationCode(request);
        return ResponseEntity.ok(new MessageResponse("Wysłano nowy kod weryfikacyjny."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("Jeśli konto istnieje, wysłaliśmy kod resetujący na email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Hasło zostało zresetowane. Możesz się zalogować."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.login(request);
        String refreshToken = authResponse.getRefreshToken();
        ResponseCookie refreshCookie = buildRefreshCookie(refreshToken, authResponse.getRefreshExpiresInMs(), httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponse);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.googleLogin(request);
        String refreshToken = authResponse.getRefreshToken();
        ResponseCookie refreshCookie = buildRefreshCookie(refreshToken, authResponse.getRefreshExpiresInMs(), httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request) {
        if (!isRequestFromAllowedOrigin(request)) {
            return ResponseEntity.status(403).build();
        }
        String refreshToken = extractRefreshTokenFromCookie(request);
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        if (!isRequestFromAllowedOrigin(request)) {
            return ResponseEntity.status(403).body(new MessageResponse("Niedozwolone pochodzenie żądania"));
        }

        String refreshToken = extractRefreshTokenFromCookie(request);
        authService.logout(refreshToken);

        CookieSettings cookieSettings = resolveCookieSettings(request);
        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSettings.secure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSettings.sameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(new MessageResponse("Wylogowano pomyślnie"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse profile = authService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, long refreshExpiresInMs, HttpServletRequest request) {
        CookieSettings cookieSettings = resolveCookieSettings(request);
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSettings.secure)
                .path("/")
                .maxAge(refreshExpiresInMs / 1000)
                .sameSite(cookieSettings.sameSite)
                .build();
    }

    private CookieSettings resolveCookieSettings(HttpServletRequest request) {
        boolean secureRequest = isSecureRequest(request);
        String origin = request.getHeader("Origin");
        boolean hasHttpsOrigin = origin != null && origin.toLowerCase(Locale.ROOT).startsWith("https://");
        boolean crossOriginRequest = hasHttpsOrigin && isCrossOriginRequest(request, origin);

        if (secureRequest && crossOriginRequest) {
            return new CookieSettings(true, "None");
        }

        if ("None".equals(cookieSameSite)) {
            return new CookieSettings(true, "None");
        }

        return new CookieSettings(cookieSecure, cookieSameSite);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return "https".equalsIgnoreCase(forwardedProto.trim());
        }
        return request.isSecure();
    }

    private boolean isCrossOriginRequest(HttpServletRequest request, String origin) {
        String requestOrigin = extractRequestOrigin(request);
        if (requestOrigin == null) {
            return true;
        }
        return !requestOrigin.equalsIgnoreCase(origin.trim());
    }

    private String extractRequestOrigin(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            if (serverName == null || serverName.isBlank()) {
                return null;
            }
            boolean defaultHttpPort = "http".equalsIgnoreCase(scheme) && serverPort == 80;
            boolean defaultHttpsPort = "https".equalsIgnoreCase(scheme) && serverPort == 443;
            if (serverPort <= 0 || defaultHttpPort || defaultHttpsPort) {
                host = serverName;
            } else {
                host = serverName + ":" + serverPort;
            }
        } else {
            host = host.split(",")[0].trim();
        }

        if (scheme == null || scheme.isBlank() || host.isBlank()) {
            return null;
        }

        return scheme + "://" + host;
    }

    private boolean isRequestFromAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return isOriginAllowed(origin);
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            String refererOrigin = extractOriginFromReferer(referer);
            return refererOrigin != null && isOriginAllowed(refererOrigin);
        }

        return false;
    }

    private boolean isOriginAllowed(String origin) {
        return allowedOrigins.contains(origin.trim().toLowerCase(Locale.ROOT));
    }

    private String extractOriginFromReferer(String referer) {
        try {
            URI uri = URI.create(referer);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }

            int port = uri.getPort();
            boolean defaultHttpPort = "http".equalsIgnoreCase(scheme) && port == 80;
            boolean defaultHttpsPort = "https".equalsIgnoreCase(scheme) && port == 443;
            if (port == -1 || defaultHttpPort || defaultHttpsPort) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeSameSite(String value) {
        if (value == null || value.isBlank()) {
            return "Strict";
        }
        if ("strict".equalsIgnoreCase(value)) {
            return "Strict";
        }
        if ("lax".equalsIgnoreCase(value)) {
            return "Lax";
        }
        if ("none".equalsIgnoreCase(value)) {
            return "None";
        }
        return "Strict";
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (javax.servlet.http.Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static final class CookieSettings {
        private final boolean secure;
        private final String sameSite;

        private CookieSettings(boolean secure, String sameSite) {
            this.secure = secure;
            this.sameSite = sameSite;
        }
    }
}
