package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model szablonu oferty
 * Przechowuje HTML, CSS i konfigurację JSON dla visual buildera
 */
@Data
@Entity
@Table(name = "offer_templates")
public class OfferTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    /**
     * Szablon HTML z placeholderami Thymeleaf
     * Przykład: <h1 th:text="${project.client.name}">Nazwa klienta</h1>
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    /**
     * Style CSS dla szablonu
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String cssContent;

    /**
     * Konfiguracja JSON dla visual buildera
     * Przechowuje strukturę sekcji, kolory, ustawienia
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String jsonConfig;

    /**
     * Czy to domyślny szablon używany przy generowaniu PDF
     */
    @Column(nullable = false)
    private Boolean isDefault = false;

    /**
     * Zdjęcia w formacie base64 (JSON array)
     * Format: ["data:image/png;base64,iVBORw0KG...", ...]
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String imageBase64;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}

