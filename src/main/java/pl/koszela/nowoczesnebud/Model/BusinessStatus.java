package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "business_status_catalog")
public class BusinessStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String code;

    @Column(length = 20)
    private String color = "#64748B";

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "requires_next_task", nullable = false)
    private Boolean requiresNextTask = false;

    @Column(name = "requires_loss_reason", nullable = false)
    private Boolean requiresLossReason = false;

    @Column(name = "is_terminal", nullable = false)
    private Boolean terminal = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

