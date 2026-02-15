package pl.koszela.nowoczesnebud.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class CalendarOAuthStateService {

    @Value("${app.calendar.oauth.state-secret:}")
    private String stateSecret;

    @Value("${app.calendar.oauth.state-ttl-seconds:600}")
    private long stateTtlSeconds;

    public String createState(Long userId) {
        long expiresAt = Instant.now().getEpochSecond() + stateTtlSeconds;
        String payload = userId + ":" + expiresAt;
        String signature = hmac(payload);
        return base64Url(payload) + "." + base64Url(signature);
    }

    public Long validateAndExtractUserId(String state) {
        if (state == null || !state.contains(".")) {
            throw new IllegalArgumentException("Nieprawidłowy stan OAuth");
        }
        String[] parts = state.split("\\.", 2);
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String providedSignature = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String expectedSignature = hmac(payload);

        if (!constantTimeEquals(expectedSignature, providedSignature)) {
            throw new IllegalArgumentException("Nieprawidłowy podpis stanu OAuth");
        }

        String[] payloadParts = payload.split(":");
        if (payloadParts.length != 2) {
            throw new IllegalArgumentException("Nieprawidłowy format stanu OAuth");
        }
        long userId = Long.parseLong(payloadParts[0]);
        long exp = Long.parseLong(payloadParts[1]);
        if (Instant.now().getEpochSecond() > exp) {
            throw new IllegalArgumentException("Stan OAuth wygasł");
        }
        return userId;
    }

    private String hmac(String value) {
        if (stateSecret == null || stateSecret.isBlank()) {
            throw new IllegalStateException("Brak app.calendar.oauth.state-secret");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stateSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Nie udało się podpisać stanu OAuth", ex);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

