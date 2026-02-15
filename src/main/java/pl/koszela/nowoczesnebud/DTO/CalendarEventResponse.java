package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventResponse {
    private String id;
    private String calendarId;
    private String summary;
    private String description;
    private String startDateTime;
    private String endDateTime;
    private String htmlLink;
    private String status;
}

