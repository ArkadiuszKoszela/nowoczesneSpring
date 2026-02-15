package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class CalendarEventRequest {

    @NotBlank(message = "Tytuł jest wymagany")
    private String summary;

    private String description;

    @NotBlank(message = "Data rozpoczęcia jest wymagana")
    private String startDateTime;

    @NotBlank(message = "Data zakończenia jest wymagana")
    private String endDateTime;

    @NotBlank(message = "Strefa czasowa jest wymagana")
    private String timeZone;

    private String calendarId;

    @Valid
    private List<CalendarReminderRequest> reminders = new ArrayList<>();

    @Valid
    private CalendarRecurrenceRequest recurrence;

    private Long clientId;
    private String clientName;
    private Long projectId;
    private String actionId;

    @NotNull
    private Boolean blockOnConflict = true;
}

