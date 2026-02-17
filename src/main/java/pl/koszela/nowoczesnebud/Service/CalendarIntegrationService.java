package pl.koszela.nowoczesnebud.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.DTO.*;
import pl.koszela.nowoczesnebud.Model.AppUser;
import pl.koszela.nowoczesnebud.Model.ClientCalendarEventLink;
import pl.koszela.nowoczesnebud.Model.GoogleCalendarConnection;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;
import pl.koszela.nowoczesnebud.Repository.ClientCalendarEventLinkRepository;
import pl.koszela.nowoczesnebud.Repository.GoogleCalendarConnectionRepository;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CalendarIntegrationService {

    private final AppUserRepository appUserRepository;
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final ClientCalendarEventLinkRepository eventLinkRepository;
    private final GoogleCalendarGatewayService googleCalendarGatewayService;
    private final CalendarOAuthStateService calendarOAuthStateService;
    private final CalendarTokenCipherService tokenCipherService;
    private final ClientWorkflowService clientWorkflowService;

    @Value("${app.calendar.oauth.client-id:}")
    private String oauthClientId;

    @Value("${app.calendar.oauth.redirect-uri:}")
    private String oauthRedirectUri;

    @Value("${app.calendar.oauth.scope:https://www.googleapis.com/auth/calendar.events}")
    private String oauthScope;

    @Value("${app.calendar.default-calendar-id:primary}")
    private String defaultCalendarId;

    public CalendarIntegrationService(AppUserRepository appUserRepository,
                                      GoogleCalendarConnectionRepository connectionRepository,
                                      ClientCalendarEventLinkRepository eventLinkRepository,
                                      GoogleCalendarGatewayService googleCalendarGatewayService,
                                      CalendarOAuthStateService calendarOAuthStateService,
                                      CalendarTokenCipherService tokenCipherService,
                                      ClientWorkflowService clientWorkflowService) {
        this.appUserRepository = appUserRepository;
        this.connectionRepository = connectionRepository;
        this.eventLinkRepository = eventLinkRepository;
        this.googleCalendarGatewayService = googleCalendarGatewayService;
        this.calendarOAuthStateService = calendarOAuthStateService;
        this.tokenCipherService = tokenCipherService;
        this.clientWorkflowService = clientWorkflowService;
    }

    public String buildConnectUrl(Long userId) {
        if (oauthClientId == null || oauthClientId.isBlank() || oauthRedirectUri == null || oauthRedirectUri.isBlank()) {
            throw new IllegalStateException("Google Calendar OAuth nie jest skonfigurowany");
        }
        String state = calendarOAuthStateService.createState(userId);
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(oauthClientId)
                + "&redirect_uri=" + encode(oauthRedirectUri)
                + "&response_type=code"
                + "&scope=" + encode(oauthScope)
                + "&access_type=offline"
                + "&include_granted_scopes=true"
                + "&prompt=consent"
                + "&state=" + encode(state);
    }

    @Transactional
    public void completeOAuthCallback(String code, String state) {
        Long userId = calendarOAuthStateService.validateAndExtractUserId(state);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));

        Map<String, Object> tokenResponse = googleCalendarGatewayService.exchangeCodeForTokens(code);
        String accessToken = String.valueOf(tokenResponse.getOrDefault("access_token", ""));
        String refreshToken = String.valueOf(tokenResponse.getOrDefault("refresh_token", ""));
        int expiresIn = Integer.parseInt(String.valueOf(tokenResponse.getOrDefault("expires_in", 3600)));
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Google nie zwrócił access_token lub refresh_token");
        }

        String sub = null;
        String email = null;
        try {
            Map<String, Object> profile = googleCalendarGatewayService.fetchGoogleProfile(accessToken);
            sub = String.valueOf(profile.getOrDefault("sub", ""));
            email = String.valueOf(profile.getOrDefault("email", ""));
        } catch (Exception ignored) {
            // access token z samym zakresem calendar.events może nie mieć dostępu do userinfo
            // to nie powinno blokować połączenia kalendarza.
        }

        GoogleCalendarConnection connection = connectionRepository.findByUserId(user.getId())
                .orElseGet(GoogleCalendarConnection::new);
        connection.setUser(user);
        connection.setGoogleSub(sub);
        if (email != null && !email.isBlank()) {
            connection.setGoogleEmail(email);
        }
        connection.setCalendarId(defaultCalendarId);
        connection.setAccessTokenEncrypted(tokenCipherService.encrypt(accessToken));
        connection.setRefreshTokenEncrypted(tokenCipherService.encrypt(refreshToken));
        connection.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        connectionRepository.save(connection);
    }

    public CalendarConnectionStatusResponse getStatus(Long userId) {
        Optional<GoogleCalendarConnection> connectionOpt = connectionRepository.findByUserId(userId);
        if (connectionOpt.isEmpty()) {
            return new CalendarConnectionStatusResponse(false, null, null, null);
        }
        GoogleCalendarConnection connection = connectionOpt.get();
        return new CalendarConnectionStatusResponse(
                true,
                connection.getGoogleEmail(),
                connection.getCalendarId(),
                connection.getExpiresAt().toString()
        );
    }

    @Transactional
    public void disconnect(Long userId) {
        connectionRepository.deleteConnectionByUserId(userId);
    }

    public CalendarSchedulePreviewResponse previewSchedule(CalendarEventRequest request) {
        ZoneId zoneId = parseZoneId(request.getTimeZone());
        ZonedDateTime start = parseDateTime(request.getStartDateTime(), zoneId);
        ZonedDateTime end = parseDateTime(request.getEndDateTime(), zoneId);
        validateDateRange(start, end);
        String recurrenceRule = buildRRule(request.getRecurrence(), zoneId);

        List<String> reminderTimes = new ArrayList<>();
        for (CalendarReminderRequest reminder : safeReminders(request.getReminders())) {
            int minutes = reminder.getMinutes() == null ? 0 : reminder.getMinutes();
            reminderTimes.add(start.minusMinutes(minutes).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return new CalendarSchedulePreviewResponse(
                start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                zoneId.getId(),
                recurrenceRule,
                reminderTimes
        );
    }

    public CalendarConflictResponse checkConflicts(Long userId, CalendarConflictRequest request) {
        GoogleCalendarConnection connection = getConnection(userId);
        ZoneId zoneId = parseZoneId(request.getTimeZone());
        ZonedDateTime start = parseDateTime(request.getStartDateTime(), zoneId);
        ZonedDateTime end = parseDateTime(request.getEndDateTime(), zoneId);
        validateDateRange(start, end);

        String accessToken = googleCalendarGatewayService.resolveValidAccessToken(connection);
        String calendarId = resolveCalendarId(request.getCalendarId(), connection);

        List<CalendarConflictItemResponse> conflicts = new ArrayList<>();
        for (Map<String, Object> event : googleCalendarGatewayService.listEvents(
                accessToken,
                calendarId,
                start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )) {
            String eventId = String.valueOf(event.getOrDefault("id", ""));
            String summary = String.valueOf(event.getOrDefault("summary", "Zajęty termin"));
            String eventStart = extractEventDateTime(event.get("start"));
            String eventEnd = extractEventDateTime(event.get("end"));
            if (eventStart == null || eventEnd == null) {
                continue;
            }
            ZonedDateTime eventStartDt = parseDateTime(eventStart, zoneId);
            ZonedDateTime eventEndDt = parseDateTime(eventEnd, zoneId);
            if (isOverlapping(start, end, eventStartDt, eventEndDt)) {
                conflicts.add(new CalendarConflictItemResponse(eventId, summary, eventStart, eventEnd));
            }
        }
        return new CalendarConflictResponse(!conflicts.isEmpty(), conflicts);
    }

    @Transactional
    public CalendarEventResponse createEvent(Long userId, CalendarEventRequest request) {
        GoogleCalendarConnection connection = getConnection(userId);
        ZoneId zoneId = parseZoneId(request.getTimeZone());
        ZonedDateTime start = parseDateTime(request.getStartDateTime(), zoneId);
        ZonedDateTime end = parseDateTime(request.getEndDateTime(), zoneId);
        validateDateRange(start, end);
        String calendarId = resolveCalendarId(request.getCalendarId(), connection);

        if (Boolean.TRUE.equals(request.getBlockOnConflict())) {
            CalendarConflictRequest conflictRequest = new CalendarConflictRequest();
            conflictRequest.setStartDateTime(request.getStartDateTime());
            conflictRequest.setEndDateTime(request.getEndDateTime());
            conflictRequest.setTimeZone(request.getTimeZone());
            conflictRequest.setCalendarId(calendarId);
            CalendarConflictResponse conflicts = checkConflicts(userId, conflictRequest);
            if (conflicts.isHasConflict()) {
                throw new IllegalArgumentException("Wybrany termin jest już zajęty");
            }
        }

        String accessToken = googleCalendarGatewayService.resolveValidAccessToken(connection);
        Map<String, Object> payload = buildGoogleEventPayload(request, start, end, calendarId);
        Map<String, Object> created = googleCalendarGatewayService.createEvent(accessToken, calendarId, payload);
        CalendarEventResponse response = mapEventResponse(created, calendarId);
        saveLink(userId, request, response, start, end);
        clientWorkflowService.appendCalendarHistory(
                request.getProjectId(),
                request.getClientId(),
                response.getId(),
                response.getSummary(),
                start.toLocalDateTime(),
                response.getStatus(),
                request.getDescription(),
                userId
        );
        return response;
    }

    @Transactional
    public CalendarEventResponse updateEvent(Long userId, String eventId, CalendarEventRequest request) {
        GoogleCalendarConnection connection = getConnection(userId);
        ZoneId zoneId = parseZoneId(request.getTimeZone());
        ZonedDateTime start = parseDateTime(request.getStartDateTime(), zoneId);
        ZonedDateTime end = parseDateTime(request.getEndDateTime(), zoneId);
        validateDateRange(start, end);
        String calendarId = resolveCalendarId(request.getCalendarId(), connection);

        String accessToken = googleCalendarGatewayService.resolveValidAccessToken(connection);
        Map<String, Object> payload = buildGoogleEventPayload(request, start, end, calendarId);
        Map<String, Object> updated = googleCalendarGatewayService.updateEvent(accessToken, calendarId, eventId, payload);
        CalendarEventResponse response = mapEventResponse(updated, calendarId);
        saveLink(userId, request, response, start, end);
        clientWorkflowService.appendCalendarHistory(
                request.getProjectId(),
                request.getClientId(),
                response.getId(),
                response.getSummary(),
                start.toLocalDateTime(),
                "updated",
                request.getDescription(),
                userId
        );
        return response;
    }

    @Transactional
    public void deleteEvent(Long userId, String eventId, String calendarId) {
        GoogleCalendarConnection connection = getConnection(userId);
        String resolvedCalendarId = resolveCalendarId(calendarId, connection);
        String accessToken = googleCalendarGatewayService.resolveValidAccessToken(connection);

        Long projectId = null;
        Long clientId = null;
        String summary = null;
        LocalDateTime eventAt = null;
        Optional<ClientCalendarEventLink> existingLink = eventLinkRepository.findByUserIdAndGoogleEventId(userId, eventId);
        if (existingLink.isPresent()) {
            projectId = existingLink.get().getProjectId();
            clientId = existingLink.get().getClientId();
            summary = existingLink.get().getSummary();
            eventAt = existingLink.get().getStartAt();
        }

        googleCalendarGatewayService.deleteEvent(accessToken, resolvedCalendarId, eventId);

        eventLinkRepository.findByUserIdAndGoogleEventId(userId, eventId)
                .ifPresent(eventLinkRepository::delete);

        clientWorkflowService.appendCalendarHistory(
                projectId,
                clientId,
                eventId,
                summary,
                eventAt,
                "deleted",
                null,
                userId
        );
    }

    public List<CalendarEventResponse> listEvents(Long userId, String fromIso, String toIso, String calendarId) {
        GoogleCalendarConnection connection = getConnection(userId);
        String accessToken = googleCalendarGatewayService.resolveValidAccessToken(connection);
        String resolvedCalendarId = resolveCalendarId(calendarId, connection);

        List<Map<String, Object>> items = googleCalendarGatewayService.listEvents(accessToken, resolvedCalendarId, fromIso, toIso);
        List<CalendarEventResponse> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            results.add(mapEventResponse(item, resolvedCalendarId));
        }
        return results;
    }

    private void saveLink(Long userId,
                          CalendarEventRequest request,
                          CalendarEventResponse response,
                          ZonedDateTime start,
                          ZonedDateTime end) {
        if (response.getId() == null || response.getId().isBlank()) {
            return;
        }
        ClientCalendarEventLink link = eventLinkRepository.findByUserIdAndGoogleEventId(userId, response.getId())
                .orElseGet(ClientCalendarEventLink::new);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));
        link.setUser(user);
        link.setClientId(request.getClientId());
        link.setClientName(request.getClientName());
        link.setProjectId(request.getProjectId());
        link.setActionId(request.getActionId());
        link.setCalendarId(response.getCalendarId());
        link.setGoogleEventId(response.getId());
        link.setSummary(response.getSummary());
        link.setStartAt(start.toLocalDateTime());
        link.setEndAt(end.toLocalDateTime());
        link.setStatus(response.getStatus());
        eventLinkRepository.save(link);
    }

    private GoogleCalendarConnection getConnection(Long userId) {
        return connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kalendarz Google nie jest połączony"));
    }

    private String resolveCalendarId(String requestedCalendarId, GoogleCalendarConnection connection) {
        if (requestedCalendarId != null && !requestedCalendarId.isBlank()) {
            return requestedCalendarId;
        }
        if (connection.getCalendarId() != null && !connection.getCalendarId().isBlank()) {
            return connection.getCalendarId();
        }
        return defaultCalendarId;
    }

    private Map<String, Object> buildGoogleEventPayload(CalendarEventRequest request,
                                                        ZonedDateTime start,
                                                        ZonedDateTime end,
                                                        String calendarId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", request.getSummary());
        payload.put("description", request.getDescription());

        payload.put("start", buildDateTimeNode(start, request.getTimeZone()));
        payload.put("end", buildDateTimeNode(end, request.getTimeZone()));

        Map<String, Object> reminders = new HashMap<>();
        reminders.put("useDefault", false);
        List<Map<String, Object>> reminderOverrides = new ArrayList<>();
        for (CalendarReminderRequest reminder : safeReminders(request.getReminders())) {
            Map<String, Object> reminderNode = new HashMap<>();
            reminderNode.put("method", reminder.getMethod());
            reminderNode.put("minutes", reminder.getMinutes());
            reminderOverrides.add(reminderNode);
        }
        reminders.put("overrides", reminderOverrides);
        payload.put("reminders", reminders);

        String recurrenceRule = buildRRule(request.getRecurrence(), parseZoneId(request.getTimeZone()));
        if (recurrenceRule != null) {
            payload.put("recurrence", Collections.singletonList(recurrenceRule));
        }

        Map<String, String> privateProperties = new HashMap<>();
        if (request.getClientId() != null) {
            privateProperties.put("clientId", String.valueOf(request.getClientId()));
        }
        if (request.getClientName() != null && !request.getClientName().isBlank()) {
            privateProperties.put("clientName", request.getClientName());
        }
        if (request.getProjectId() != null) {
            privateProperties.put("projectId", String.valueOf(request.getProjectId()));
        }
        if (request.getActionId() != null && !request.getActionId().isBlank()) {
            privateProperties.put("actionId", request.getActionId());
        }
        privateProperties.put("sourceApp", "nowoczesne-bud");
        payload.put("extendedProperties", Collections.singletonMap("private", privateProperties));
        payload.put("calendarId", calendarId);
        return payload;
    }

    private Map<String, String> buildDateTimeNode(ZonedDateTime dateTime, String timeZone) {
        Map<String, String> node = new HashMap<>();
        node.put("dateTime", dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("timeZone", timeZone);
        return node;
    }

    private String buildRRule(CalendarRecurrenceRequest recurrence, ZoneId zoneId) {
        if (recurrence == null || recurrence.getFrequency() == null || recurrence.getFrequency().isBlank()) {
            return null;
        }
        String freq = recurrence.getFrequency().trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("DAILY", "WEEKLY", "MONTHLY", "YEARLY").contains(freq)) {
            throw new IllegalArgumentException("Nieobsługiwana częstotliwość cykliczności");
        }

        List<String> parts = new ArrayList<>();
        parts.add("FREQ=" + freq);
        if (recurrence.getInterval() != null && recurrence.getInterval() > 0) {
            parts.add("INTERVAL=" + recurrence.getInterval());
        }
        if (recurrence.getCount() != null && recurrence.getCount() > 0) {
            parts.add("COUNT=" + recurrence.getCount());
        }
        if (recurrence.getByDay() != null && !recurrence.getByDay().isBlank()) {
            parts.add("BYDAY=" + recurrence.getByDay().toUpperCase(Locale.ROOT));
        }
        if (recurrence.getUntil() != null && !recurrence.getUntil().isBlank()) {
            ZonedDateTime until = parseDateTime(recurrence.getUntil(), zoneId);
            parts.add("UNTIL=" + until.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")));
        }
        return "RRULE:" + String.join(";", parts);
    }

    private CalendarEventResponse mapEventResponse(Map<String, Object> source, String calendarId) {
        String id = String.valueOf(source.getOrDefault("id", ""));
        String summary = String.valueOf(source.getOrDefault("summary", ""));
        String description = String.valueOf(source.getOrDefault("description", ""));
        String htmlLink = String.valueOf(source.getOrDefault("htmlLink", ""));
        String status = String.valueOf(source.getOrDefault("status", ""));

        String startDateTime = null;
        String endDateTime = null;
        if (source.get("start") instanceof Map<?, ?>) {
            Object startValue = ((Map<?, ?>) source.get("start")).get("dateTime");
            startDateTime = startValue == null ? "" : String.valueOf(startValue);
        }
        if (source.get("end") instanceof Map<?, ?>) {
            Object endValue = ((Map<?, ?>) source.get("end")).get("dateTime");
            endDateTime = endValue == null ? "" : String.valueOf(endValue);
        }
        return new CalendarEventResponse(id, calendarId, summary, description, startDateTime, endDateTime, htmlLink, status);
    }

    private ZoneId parseZoneId(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Nieprawidłowa strefa czasowa");
        }
    }

    private ZonedDateTime parseDateTime(String dateTime, ZoneId zoneId) {
        try {
            if (dateTime.endsWith("Z") || dateTime.contains("+")) {
                return OffsetDateTime.parse(dateTime).atZoneSameInstant(zoneId);
            }
            return LocalDateTime.parse(dateTime).atZone(zoneId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Nieprawidłowy format daty/czasu: " + dateTime);
        }
    }

    private void validateDateRange(ZonedDateTime start, ZonedDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Data zakończenia musi być późniejsza niż data rozpoczęcia");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new IllegalArgumentException("Wydarzenie nie może trwać dłużej niż 365 dni");
        }
    }

    private List<CalendarReminderRequest> safeReminders(List<CalendarReminderRequest> reminders) {
        if (reminders == null) {
            return Collections.emptyList();
        }
        return reminders;
    }

    private String encode(String value) {
        return value.replace(" ", "%20")
                .replace(":", "%3A")
                .replace("/", "%2F")
                .replace("?", "%3F")
                .replace("&", "%26")
                .replace("=", "%3D");
    }

    private String extractEventDateTime(Object node) {
        if (!(node instanceof Map<?, ?>)) {
            return null;
        }
        Object dateTime = ((Map<?, ?>) node).get("dateTime");
        if (dateTime != null) {
            return String.valueOf(dateTime);
        }
        // All-day event fallback
        Object date = ((Map<?, ?>) node).get("date");
        if (date != null) {
            return String.valueOf(date) + "T00:00:00";
        }
        return null;
    }

    private boolean isOverlapping(ZonedDateTime requestedStart,
                                  ZonedDateTime requestedEnd,
                                  ZonedDateTime eventStart,
                                  ZonedDateTime eventEnd) {
        return requestedStart.isBefore(eventEnd) && requestedEnd.isAfter(eventStart);
    }
}

