package pl.koszela.nowoczesnebud.Auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.koszela.nowoczesnebud.Model.EmailVerificationCode;
import pl.koszela.nowoczesnebud.Model.PasswordResetCode;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;
import pl.koszela.nowoczesnebud.Repository.EmailVerificationCodeRepository;
import pl.koszela.nowoczesnebud.Repository.PasswordResetCodeRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetCodeRepository passwordResetCodeRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void shouldRegisterAndLoginAndSetRefreshCookie() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rejestracja zakończona. Sprawdź email i wpisz kod weryfikacyjny."));

        verifyUserEmail(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void shouldReturnUnauthorizedForProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshAccessTokenWhenValidRefreshCookieProvided() throws Exception {
        String username = "refresh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        verifyUserEmail(email);

        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");

        String refreshToken = extractCookieValue(setCookieHeader, "refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new MockCookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void shouldReturnCurrentUserProfileForValidAccessToken() throws Exception {
        String username = "me_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        verifyUserEmail(email);

        String accessToken = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(accessToken).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldBlockLoginWhenEmailNotVerified() throws Exception {
        String username = "nov_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, password))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Najpierw zweryfikuj email kodem z wiadomości."));
    }

    @Test
    void shouldVerifyEmailAndAllowLogin() throws Exception {
        String username = "verify_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        verifyUserEmail(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldFailVerificationForInvalidCode() throws Exception {
        String username = "badcode_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload(email, "111111"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niepoprawny kod weryfikacyjny."));
    }

    @Test
    void shouldFailVerificationForExpiredCode() throws Exception {
        String username = "exp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        EmailVerificationCode code = emailVerificationCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(
                        appUserRepository.findByEmailIgnoreCase(email).orElseThrow().getId())
                .orElseThrow();
        code.setCodeHash(passwordEncoder.encode("222222"));
        code.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        emailVerificationCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload(email, "222222"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kod weryfikacyjny wygasł. Wyślij nowy kod."));
    }

    @Test
    void shouldFailVerificationAfterTooManyAttempts() throws Exception {
        String username = "attempts_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String password = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, password))))
                .andExpect(status().isOk());

        EmailVerificationCode code = emailVerificationCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(
                        appUserRepository.findByEmailIgnoreCase(email).orElseThrow().getId())
                .orElseThrow();
        code.setAttempts(4);
        emailVerificationCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload(email, "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Przekroczono liczbę prób. Wyślij nowy kod."));
    }

    @Test
    void shouldResetPasswordByEmailCodeAndAllowLoginWithNewPassword() throws Exception {
        String username = "reset_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String oldPassword = "SecurePass123";
        String newPassword = "NewSecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, oldPassword))))
                .andExpect(status().isOk());
        verifyUserEmail(email);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPayload(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Jeśli konto istnieje, wysłaliśmy kod resetujący na email."));

        PasswordResetCode code = passwordResetCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(
                        appUserRepository.findByEmailIgnoreCase(email).orElseThrow().getId())
                .orElseThrow();
        code.setCodeHash(passwordEncoder.encode("654321"));
        code.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(5));
        passwordResetCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPayload(email, "654321", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hasło zostało zresetowane. Możesz się zalogować."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldFailPasswordResetWhenCodeInvalid() throws Exception {
        String username = "resetbad_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@test.pl";
        String oldPassword = "SecurePass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload(username, email, oldPassword))))
                .andExpect(status().isOk());
        verifyUserEmail(email);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPayload(email))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPayload(email, "999999", "NewSecurePass123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niepoprawny kod resetujący."));
    }

    private Map<String, Object> registerPayload(String username, String email, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        return payload;
    }

    private Map<String, Object> loginPayload(String email, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        return payload;
    }

    private Map<String, Object> verifyPayload(String email, String code) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("code", code);
        return payload;
    }

    private Map<String, Object> forgotPayload(String email) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        return payload;
    }

    private Map<String, Object> resetPayload(String email, String code, String newPassword) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("code", code);
        payload.put("newPassword", newPassword);
        return payload;
    }

    private void verifyUserEmail(String email) throws Exception {
        EmailVerificationCode code = emailVerificationCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(
                        appUserRepository.findByEmailIgnoreCase(email).orElseThrow().getId())
                .orElseThrow();
        code.setCodeHash(passwordEncoder.encode("123456"));
        code.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(5));
        emailVerificationCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload(email, "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email został zweryfikowany. Możesz się zalogować."));
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        if (setCookieHeader == null || setCookieHeader.isBlank()) {
            return "";
        }
        String[] cookieParts = setCookieHeader.split(";");
        for (String part : cookieParts) {
            String trimmed = part.trim();
            if (trimmed.startsWith(cookieName + "=")) {
                return trimmed.substring((cookieName + "=").length());
            }
        }
        return "";
    }
}
