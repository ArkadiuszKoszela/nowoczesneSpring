package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CalendarSchedulePreviewResponse {
    private String startDateTime;
    private String endDateTime;
    private String timeZone;
    private String recurrenceRule;
    private List<String> reminderPreviewTimes;
}

