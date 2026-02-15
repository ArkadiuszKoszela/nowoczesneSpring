package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "google_calendar_connections",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_id")
        }
)
public class GoogleCalendarConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "google_sub", length = 100)
    private String googleSub;

    @Column(name = "google_email", length = 150)
    private String googleEmail;

    @Column(name = "calendar_id", nullable = false, length = 150)
    private String calendarId = "primary";

    @Column(name = "access_token_encrypted", nullable = false, length = 4096)
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", nullable = false, length = 4096)
    private String refreshTokenEncrypted;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime connectedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

