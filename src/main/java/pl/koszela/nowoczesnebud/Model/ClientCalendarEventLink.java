package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "client_calendar_event_links")
public class ClientCalendarEventLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "action_id", length = 100)
    private String actionId;

    @Column(name = "calendar_id", nullable = false, length = 150)
    private String calendarId;

    @Column(name = "google_event_id", nullable = false, length = 300)
    private String googleEventId;

    @Column(name = "summary", length = 400)
    private String summary;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "status", length = 60)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

