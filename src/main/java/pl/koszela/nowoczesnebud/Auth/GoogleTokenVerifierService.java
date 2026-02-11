package pl.koszela.nowoczesnebud.Auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.koszela.nowoczesnebud.DTO.UserProfileResponse;

import java.util.Map;

@Service
public class GoogleTokenVerifierService {

    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Value("${app.auth.google-client-id:}")
    private String googleClientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserProfileResponse verifyAndExtractProfile(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalArgumentException("Google login nie jest skonfigurowany (brak app.auth.google-client-id)");
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(GOOGLE_TOKEN_INFO_URL + idToken, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalArgumentException("Nie udało się zweryfikować tokenu Google");
            }

            Map<String, Object> payload = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            String aud = String.valueOf(payload.getOrDefault("aud", ""));
            String email = String.valueOf(payload.getOrDefault("email", ""));
            String emailVerified = String.valueOf(payload.getOrDefault("email_verified", "false"));

            if (!googleClientId.equals(aud)) {
                throw new IllegalArgumentException("Token Google ma nieprawidłowy audience");
            }
            if (!"true".equalsIgnoreCase(emailVerified)) {
                throw new IllegalArgumentException("Email Google nie został zweryfikowany");
            }
            if (email.isBlank()) {
                throw new IllegalArgumentException("Brak adresu email w tokenie Google");
            }

            String preferredUsername = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
            return new UserProfileResponse(0L, preferredUsername, email.toLowerCase(), "ROLE_USER");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Nieprawidłowy token Google");
        }
    }
}
