package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "client_status_history")
public class ClientStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id")
    private BusinessStatus fromStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id")
    private BusinessStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusHistorySource source = StatusHistorySource.MANUAL;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "calendar_event_id", length = 300)
    private String calendarEventId;

    @Column(name = "calendar_summary", length = 400)
    private String calendarSummary;

    @Column(length = 2000)
    private String note;

    @Column(name = "event_at")
    private LocalDateTime eventAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

