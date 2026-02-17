package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "project_tasks")
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProjectTaskType type = ProjectTaskType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectTaskStatus status = ProjectTaskStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectTaskPriority priority = ProjectTaskPriority.MEDIUM;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(length = 2000)
    private String note;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "auto_created", nullable = false)
    private Boolean autoCreated = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

