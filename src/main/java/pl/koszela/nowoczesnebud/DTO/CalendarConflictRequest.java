package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CalendarConflictRequest {

    @NotBlank(message = "Data rozpoczęcia jest wymagana")
    private String startDateTime;

    @NotBlank(message = "Data zakończenia jest wymagana")
    private String endDateTime;

    @NotBlank(message = "Strefa czasowa jest wymagana")
    private String timeZone;

    private String calendarId;
}

