package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

@Data
public class CalendarRecurrenceRequest {
    private String frequency;
    private Integer interval = 1;
    private String until;
    private Integer count;
    private String byDay;
}

