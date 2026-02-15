package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CalendarConflictItemResponse {
    private String eventId;
    private String summary;
    private String startDateTime;
    private String endDateTime;
}

