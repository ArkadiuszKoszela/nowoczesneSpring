package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import pl.koszela.nowoczesnebud.Model.ProjectTaskPriority;
import pl.koszela.nowoczesnebud.Model.ProjectTaskType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class CreateProjectTaskRequest {

    @NotBlank(message = "Tytuł zadania jest wymagany")
    @Size(max = 240, message = "Tytuł zadania może mieć maksymalnie 240 znaków")
    private String title;

    @Size(max = 2000, message = "Notatka może mieć maksymalnie 2000 znaków")
    private String note;

    @NotNull(message = "Termin zadania jest wymagany")
    private LocalDateTime dueAt;

    private ProjectTaskType type = ProjectTaskType.OTHER;
    private ProjectTaskPriority priority = ProjectTaskPriority.MEDIUM;
    private Boolean autoCreated = false;
}

