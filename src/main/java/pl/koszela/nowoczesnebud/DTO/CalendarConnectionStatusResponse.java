package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CalendarConnectionStatusResponse {
    private boolean connected;
    private String googleEmail;
    private String calendarId;
    private String expiresAt;
}

