package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProjectTaskResponse {
    private Long id;
    private Long projectId;
    private Long clientId;
    private String title;
    private String note;
    private String type;
    private String status;
    private String priority;
    private LocalDateTime dueAt;
    private Boolean autoCreated;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

