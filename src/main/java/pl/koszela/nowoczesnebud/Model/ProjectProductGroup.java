package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model ProjectProductGroup - opcje dla grup produktowych w projekcie
 * RELACJA: Wiele ProjectProductGroup → Jeden Project
 * 
 * Przechowuje informację czy grupa produktowa jest "Główną" czy "Opcjonalną"
 * (np. CANTUS-czarna vs CANTUS-grafitowa - tylko jedna może być główna)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_product_groups")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class ProjectProductGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    
    /**
     * RELACJA: Wiele ProjectProductGroup → Jeden Project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;
    
    /**
     * Kategoria produktu (TILE, GUTTER, ACCESSORY)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty
    private ProductCategory category;
    
    /**
     * Producent (np. "CANTUS")
     */
    @Column(name = "manufacturer", nullable = false)
    @JsonProperty
    private String manufacturer;
    
    /**
     * Nazwa grupy produktowej (np. "czarna ang NUANE")
     */
    @Column(name = "group_name", nullable = false)
    @JsonProperty
    private String groupName;
    
    /**
     * Czy grupa jest "Główna" (true) czy "Opcjonalna" (false)
     * null = nie wybrano opcji
     * 
     * Tylko jedna grupa na producenta może być "Główna"
     */
    @Column(name = "is_main_option")
    @JsonProperty
    private Boolean isMainOption;
    
    @CreationTimestamp
    @Column(name = "created_at")
    @JsonIgnore
    private LocalDateTime createdAt;
}

