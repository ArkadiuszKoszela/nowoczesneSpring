package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model Input - dane wejściowe z formularza dla projektu
 * RELACJA: Wiele Inputs → Jeden Project
 * 
 * Input przechowuje TYLKO dane z formularza (np. "Powierzchnia połaci" = 200).
 * Dane produktów (ceny, ilości) są w ProjectProduct, nie w Input.
 */
@Data
@ToString(exclude = {"project"}) // ⚠️ Wyklucz cykliczną referencję do Project z toString()
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inputs")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Input {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    
    /**
     * RELACJA: Wiele Inputs → Jeden Project
     * Ignorowane podczas deserializacji (JSON nie zawiera project, tylko project_id jest ustawiane przez serwis)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;
    
    // ========== DANE Z FORMULARZA ==========
    /**
     * Nazwa pola z formularza (np. "Powierzchnia połaci")
     */
    @JsonProperty
    private String name;
    
    /**
     * Nazwa do mapowania z produktami ze snapshotu (np. "powierzchnia polaci")
     * Używane do dopasowania Input z formularza do produktów w snapshotach
     */
    @JsonProperty
    private String mapperName;
    
    /**
     * Wartość z formularza (np. 200 dla "Powierzchnia połaci")
     * Używana do obliczenia quantity produktów: quantity = inputQuantity * quantityConverter
     */
    @Column(nullable = true)
    @JsonProperty
    private Double quantity;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonIgnore
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonIgnore
    private LocalDateTime updatedAt;
}
