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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        String refreshToken = authResponse.getRefreshToken();
        ResponseCookie refreshCookie = buildRefreshCookie(refreshToken, authResponse.getRefreshExpiresInMs());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponse);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse authResponse = authService.googleLogin(request);
        String refreshToken = authResponse.getRefreshToken();
        ResponseCookie refreshCookie = buildRefreshCookie(refreshToken, authResponse.getRefreshExpiresInMs());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        authService.logout(refreshToken);

        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
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

    private ResponseCookie buildRefreshCookie(String refreshToken, long refreshExpiresInMs) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(refreshExpiresInMs / 1000)
                .sameSite("Strict")
                .build();
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
}
