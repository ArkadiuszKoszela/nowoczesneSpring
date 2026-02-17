package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ClientStatusHistoryResponse {
    private Long id;
    private Long projectId;
    private Long clientId;
    private Long fromStatusId;
    private String fromStatusName;
    private Long toStatusId;
    private String toStatusName;
    private String note;
    private String source;
    private String calendarEventId;
    private String calendarSummary;
    private LocalDateTime eventAt;
    private LocalDateTime createdAt;
}

