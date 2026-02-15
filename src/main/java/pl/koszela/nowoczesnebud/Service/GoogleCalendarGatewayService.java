package pl.koszela.nowoczesnebud.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pl.koszela.nowoczesnebud.Model.GoogleCalendarConnection;
import pl.koszela.nowoczesnebud.Repository.GoogleCalendarConnectionRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GoogleCalendarGatewayService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarGatewayService.class);

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_CALENDAR_EVENTS_ENDPOINT = "https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events";
    private static final String GOOGLE_CALENDAR_FREE_BUSY_ENDPOINT = "https://www.googleapis.com/calendar/v3/freeBusy";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final CalendarTokenCipherService tokenCipherService;

    @Value("${app.calendar.oauth.client-id:}")
    private String oauthClientId;

    @Value("${app.calendar.oauth.client-secret:}")
    private String oauthClientSecret;

    @Value("${app.calendar.oauth.redirect-uri:}")
    private String oauthRedirectUri;

    public GoogleCalendarGatewayService(GoogleCalendarConnectionRepository connectionRepository,
                                        CalendarTokenCipherService tokenCipherService) {
        this.connectionRepository = connectionRepository;
        this.tokenCipherService = tokenCipherService;
    }

    public Map<String, Object> exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", oauthClientId);
        formData.add("client_secret", oauthClientSecret);
        formData.add("redirect_uri", oauthRedirectUri);
        formData.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> response = restTemplate.postForEntity(
                TOKEN_ENDPOINT,
                new HttpEntity<>(formData, headers),
                String.class
        );
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("Nie udało się wymienić authorization code na tokeny");
        }

        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Nie udało się odczytać odpowiedzi token endpoint", ex);
        }
    }

    public Map<String, Object> fetchGoogleProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> response = restTemplate.exchange(
                USER_INFO_ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("Nie udało się pobrać profilu Google");
        }

        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Nie udało się odczytać profilu Google", ex);
        }
    }

    public String resolveValidAccessToken(GoogleCalendarConnection connection) {
        if (connection.getExpiresAt().isAfter(LocalDateTime.now().plusSeconds(60))) {
            return tokenCipherService.decrypt(connection.getAccessTokenEncrypted());
        }
        return refreshAccessToken(connection);
    }

    public String refreshAccessToken(GoogleCalendarConnection connection) {
        String refreshToken = tokenCipherService.decrypt(connection.getRefreshTokenEncrypted());
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", oauthClientId);
        formData.add("client_secret", oauthClientSecret);
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response = restTemplate.postForEntity(
                TOKEN_ENDPOINT,
                new HttpEntity<>(formData, headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Nie udało się odświeżyć tokenu Google");
        }

        try {
            Map<String, Object> map = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            String newAccessToken = String.valueOf(map.getOrDefault("access_token", ""));
            int expiresIn = Integer.parseInt(String.valueOf(map.getOrDefault("expires_in", 3600)));
            if (newAccessToken.isBlank()) {
                throw new IllegalStateException("Google nie zwrócił access_token");
            }

            connection.setAccessTokenEncrypted(tokenCipherService.encrypt(newAccessToken));
            connection.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            connectionRepository.save(connection);
            return newAccessToken;
        } catch (Exception ex) {
            throw new IllegalStateException("Nie udało się przetworzyć odświeżenia tokenu", ex);
        }
    }

    public Map<String, Object> createEvent(String accessToken, String calendarId, Map<String, Object> payload) {
        HttpHeaders headers = buildJsonHeaders(accessToken);
        ResponseEntity<String> response = restTemplate.postForEntity(
                GOOGLE_CALENDAR_EVENTS_ENDPOINT,
                new HttpEntity<>(payload, headers),
                String.class,
                calendarId
        );
        return readBodyAsMap(response, "Nie udało się utworzyć wydarzenia Google Calendar");
    }

    public Map<String, Object> updateEvent(String accessToken, String calendarId, String eventId, Map<String, Object> payload) {
        HttpHeaders headers = buildJsonHeaders(accessToken);
        String url = GOOGLE_CALENDAR_EVENTS_ENDPOINT + "/{eventId}";
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                new HttpEntity<>(payload, headers),
                String.class,
                calendarId,
                eventId
        );
        return readBodyAsMap(response, "Nie udało się zaktualizować wydarzenia Google Calendar");
    }

    public void deleteEvent(String accessToken, String calendarId, String eventId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        String url = GOOGLE_CALENDAR_EVENTS_ENDPOINT + "/{eventId}";
        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class,
                calendarId,
                eventId
        );
    }

    public List<Map<String, Object>> listEvents(String accessToken, String calendarId, String fromIso, String toIso) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        String resolvedCalendarId = (calendarId == null || calendarId.isBlank()) ? "primary" : calendarId;
        String normalizedFromIso = normalizeToUtcIsoInstant(fromIso);
        String normalizedToIso = normalizeToUtcIsoInstant(toIso);
        String url = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events")
                .queryParam("timeMin", normalizedFromIso)
                .queryParam("timeMax", normalizedToIso)
                .queryParam("singleEvents", "true")
                .queryParam("orderBy", "startTime")
                .buildAndExpand(resolvedCalendarId)
                .encode()
                .toUriString();
        logger.info("Google Calendar listEvents url={}, calendarId={}, timeMin={}, timeMax={}",
                url, resolvedCalendarId, normalizedFromIso, normalizedToIso);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        Map<String, Object> result = readBodyAsMap(response, "Nie udało się pobrać wydarzeń Google Calendar");
        Object items = result.get("items");
        if (!(items instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : (List<?>) items) {
            if (item instanceof Map<?, ?>) {
                mapped.add(objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}));
            }
        }
        return mapped;
    }

    private String normalizeToUtcIsoInstant(String isoDateTime) {
        try {
            return OffsetDateTime.parse(isoDateTime)
                    .toInstant()
                    .toString();
        } catch (Exception ex) {
            try {
                return DateTimeFormatter.ISO_INSTANT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(isoDateTime));
            } catch (Exception ignored) {
                return isoDateTime;
            }
        }
    }

    public List<Map<String, Object>> freeBusy(String accessToken, String calendarId, String fromIso, String toIso) {
        HttpHeaders headers = buildJsonHeaders(accessToken);
        Map<String, Object> payload = new HashMap<>();
        payload.put("timeMin", fromIso);
        payload.put("timeMax", toIso);
        payload.put("items", Collections.singletonList(Collections.singletonMap("id", calendarId)));

        ResponseEntity<String> response = restTemplate.postForEntity(
                GOOGLE_CALENDAR_FREE_BUSY_ENDPOINT,
                new HttpEntity<>(payload, headers),
                String.class
        );
        Map<String, Object> map = readBodyAsMap(response, "Nie udało się pobrać freeBusy");
        Object calendarsNode = map.get("calendars");
        if (!(calendarsNode instanceof Map<?, ?>)) {
            return Collections.emptyList();
        }
        Map<String, Object> calendars = objectMapper.convertValue(calendarsNode, new TypeReference<Map<String, Object>>() {});
        Object calendar = calendars.get(calendarId);
        if (!(calendar instanceof Map<?, ?>)) {
            return Collections.emptyList();
        }
        Object busy = ((Map<?, ?>) calendar).get("busy");
        if (!(busy instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object slot : (List<?>) busy) {
            if (slot instanceof Map<?, ?>) {
                result.add(objectMapper.convertValue(slot, new TypeReference<Map<String, Object>>() {}));
            }
        }
        return result;
    }

    private HttpHeaders buildJsonHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> readBodyAsMap(ResponseEntity<String> response, String errorMessage) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }
}

