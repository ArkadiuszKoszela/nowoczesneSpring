package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ClientPipelineItemResponse {
    private Long projectId;
    private Long clientId;
    private String clientName;
    private String email;
    private String telephone;
    private Long currentStatusId;
    private String currentStatusName;
    private String currentStatusColor;
    private LocalDateTime statusUpdatedAt;
    private Long nextTaskId;
    private String nextTaskTitle;
    private LocalDateTime nextTaskDueAt;
    private String nextTaskStatus;
    private Boolean atRisk;
}

