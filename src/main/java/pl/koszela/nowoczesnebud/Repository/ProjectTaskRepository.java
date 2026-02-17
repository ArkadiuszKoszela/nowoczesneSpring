package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProjectTask;
import pl.koszela.nowoczesnebud.Model.ProjectTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {
    List<ProjectTask> findByProjectIdOrderByDueAtAscCreatedAtDesc(Long projectId);
    List<ProjectTask> findByProjectIdAndStatusOrderByDueAtAscCreatedAtDesc(Long projectId, ProjectTaskStatus status);
    List<ProjectTask> findByStatusAndDueAtBefore(ProjectTaskStatus status, LocalDateTime dueAt);
    boolean existsByProjectIdAndStatusAndDueAtAfter(Long projectId, ProjectTaskStatus status, LocalDateTime dueAt);
}

