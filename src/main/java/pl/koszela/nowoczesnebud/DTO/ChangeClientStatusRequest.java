package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import pl.koszela.nowoczesnebud.Model.ProjectTaskPriority;
import pl.koszela.nowoczesnebud.Model.ProjectTaskType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class ChangeClientStatusRequest {

    @NotNull(message = "statusId jest wymagane")
    private Long statusId;

    @Size(max = 2000, message = "Notatka może mieć maksymalnie 2000 znaków")
    private String note;

    // Jeśli status wymaga kolejnego kroku, można go dostarczyć od razu tutaj.
    @Size(max = 240, message = "Tytuł zadania może mieć maksymalnie 240 znaków")
    private String nextTaskTitle;
    private LocalDateTime nextTaskDueAt;
    private ProjectTaskType nextTaskType = ProjectTaskType.OTHER;
    private ProjectTaskPriority nextTaskPriority = ProjectTaskPriority.MEDIUM;
}

