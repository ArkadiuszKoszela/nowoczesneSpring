package pl.koszela.nowoczesnebud.Controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.koszela.nowoczesnebud.DTO.*;
import pl.koszela.nowoczesnebud.Model.AppUser;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;
import pl.koszela.nowoczesnebud.Service.CalendarIntegrationService;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@Validated
public class CalendarController {

    private static final Logger logger = LoggerFactory.getLogger(CalendarController.class);

    private final CalendarIntegrationService calendarIntegrationService;
    private final AppUserRepository appUserRepository;

    public CalendarController(CalendarIntegrationService calendarIntegrationService,
                              AppUserRepository appUserRepository) {
        this.calendarIntegrationService = calendarIntegrationService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/connect-url")
    public ResponseEntity<CalendarConnectUrlResponse> getConnectUrl() {
        Long userId = getCurrentUser().getId();
        String url = calendarIntegrationService.buildConnectUrl(userId);
        return ResponseEntity.ok(new CalendarConnectUrlResponse(url));
    }

    @GetMapping(value = "/oauth/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> oauthCallback(@RequestParam(required = false) String code,
                                                @RequestParam(required = false) String state,
                                                @RequestParam(required = false) String error) {
        logger.info("OAuth callback received. error={}, hasCode={}, hasState={}",
                error, code != null && !code.isBlank(), state != null && !state.isBlank());
        if (error != null) {
            logger.warn("OAuth callback returned error: {}", error);
            return ResponseEntity.ok(callbackHtml("error", "Google OAuth zwrócił błąd: " + error));
        }
        if (code == null || state == null) {
            logger.warn("OAuth callback missing required parameters");
            return ResponseEntity.badRequest().body(callbackHtml("error", "Brak code lub state"));
        }
        try {
            calendarIntegrationService.completeOAuthCallback(code, state);
            logger.info("OAuth callback completed successfully");
            return ResponseEntity.ok(callbackHtml("success", "Połączono kalendarz Google"));
        } catch (Exception ex) {
            logger.error("OAuth callback failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(callbackHtml("error", ex.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<CalendarConnectionStatusResponse> status() {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(calendarIntegrationService.getStatus(userId));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<MessageResponse> disconnect() {
        Long userId = getCurrentUser().getId();
        calendarIntegrationService.disconnect(userId);
        return ResponseEntity.ok(new MessageResponse("Rozłączono kalendarz Google"));
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventResponse> createEvent(@Valid @RequestBody CalendarEventRequest request) {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(calendarIntegrationService.createEvent(userId, request));
    }

    @PostMapping("/events/from-action/{actionId}")
    public ResponseEntity<CalendarEventResponse> createEventFromAction(@PathVariable String actionId,
                                                                       @Valid @RequestBody CalendarEventRequest request) {
        Long userId = getCurrentUser().getId();
        request.setActionId(actionId);
        return ResponseEntity.ok(calendarIntegrationService.createEvent(userId, request));
    }

    @PatchMapping("/events/{eventId}")
    public ResponseEntity<CalendarEventResponse> updateEvent(@PathVariable String eventId,
                                                             @Valid @RequestBody CalendarEventRequest request) {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(calendarIntegrationService.updateEvent(userId, eventId, request));
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<MessageResponse> deleteEvent(@PathVariable String eventId,
                                                       @RequestParam(required = false) String calendarId) {
        Long userId = getCurrentUser().getId();
        calendarIntegrationService.deleteEvent(userId, eventId, calendarId);
        return ResponseEntity.ok(new MessageResponse("Usunięto wydarzenie"));
    }

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> listEvents(@RequestParam String from,
                                                                  @RequestParam String to,
                                                                  @RequestParam(required = false) String calendarId) {
        Long userId = getCurrentUser().getId();
        String fromIso = OffsetDateTime.parse(from).toString();
        String toIso = OffsetDateTime.parse(to).toString();
        return ResponseEntity.ok(calendarIntegrationService.listEvents(userId, fromIso, toIso, calendarId));
    }

    @PostMapping("/conflicts/check")
    public ResponseEntity<CalendarConflictResponse> conflicts(@Valid @RequestBody CalendarConflictRequest request) {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(calendarIntegrationService.checkConflicts(userId, request));
    }

    @PostMapping("/schedules/preview")
    public ResponseEntity<CalendarSchedulePreviewResponse> preview(@Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(calendarIntegrationService.previewSchedule(request));
    }

    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Brak użytkownika w kontekście bezpieczeństwa");
        }
        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));
    }

    private String callbackHtml(String status, String message) {
        String safeStatus = "success".equals(status) ? "success" : "error";
        String safeMessage = message == null ? "" : message.replace("'", "");
        String closeScript = "success".equals(safeStatus)
                ? "setTimeout(function(){ window.close(); }, 300);"
                : "";
        return "<!doctype html><html><head><meta charset='utf-8'><title>Google Calendar</title></head>"
                + "<body><script>"
                + "if (window.opener) { window.opener.postMessage({ source: 'google-calendar-oauth', status: '" + safeStatus + "', message: '" + safeMessage + "' }, '*'); }"
                + closeScript
                + "</script>"
                + "<h3>Google Calendar OAuth</h3>"
                + "<p>Status: <strong>" + safeStatus + "</strong></p>"
                + "<p>" + safeMessage + "</p>"
                + "</body></html>";
    }
}

