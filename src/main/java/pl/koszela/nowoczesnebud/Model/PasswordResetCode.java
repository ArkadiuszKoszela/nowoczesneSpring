package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "password_reset_codes",
        indexes = {
                @Index(name = "idx_password_reset_user_used", columnList = "user_id,used"),
                @Index(name = "idx_password_reset_expiry", columnList = "expiresAt")
        }
)
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 255)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = false;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private LocalDateTime resendAvailableAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
