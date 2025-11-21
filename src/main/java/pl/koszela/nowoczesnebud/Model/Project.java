package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model Project - zastępuje Offer
 * Relacja: Jeden Client (User) → Wiele Projektów
 */
@Data
@ToString(exclude = {"inputs", "projectProducts", "projectProductGroups"}) // ⚠️ Wyklucz cykliczne referencje z toString()
@Entity
@Table(name = "projects") // Nazwa tabeli w bazie
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * RELACJA: Wiele projektów → Jeden klient
     * Zmiana z @OneToOne na @ManyToOne
     * EAGER żeby uniknąć problemów z serializacją lazy proxy
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;
    
    /**
     * Nazwa projektu (np. "Projekt dachu - 15.11.2024")
     */
    private String projectName;
    
    /**
     * Status projektu
     */
    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.DRAFT;
    
    // ========== RABATY GLOBALNE DLA ZAKŁADEK ==========
    
    /**
     * DACHÓWKI - Marża (%)
     */
    @Column(name = "tiles_margin")
    private Double tilesMargin = 0.0;
    
    /**
     * DACHÓWKI - Rabat indywidualny (%)
     */
    @Column(name = "tiles_discount")
    private Double tilesDiscount = 0.0;
    
    /**
     * RYNNY - Marża (%)
     */
    @Column(name = "gutters_margin")
    private Double guttersMargin = 0.0;
    
    /**
     * RYNNY - Rabat indywidualny (%)
     */
    @Column(name = "gutters_discount")
    private Double guttersDiscount = 0.0;
    
    /**
     * AKCESORIA - Marża (%)
     */
    @Column(name = "accessories_margin")
    private Double accessoriesMargin = 0.0;
    
    /**
     * AKCESORIA - Rabat indywidualny (%)
     */
    @Column(name = "accessories_discount")
    private Double accessoriesDiscount = 0.0;
    
    
    /**
     * RELACJA: Jeden projekt → Wiele inputów
     * Zmiana z @JoinColumn na mappedBy (Input ma @ManyToOne)
     * LAZY - inputs nie są automatycznie ładowane (unika LazyInitializationException)
     * Inputs są ładowane tylko w getProjectById() gdzie są potrzebne (używa JOIN FETCH)
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"project", "hibernateLazyInitializer", "handler"})
    private List<Input> inputs = new ArrayList<>();
    
    /**
     * RELACJA: Jeden projekt → Wiele produktów (zapisane ceny i ilości)
     * ProjectProduct przechowuje zapisane ceny produktów z momentu ostatniego zapisu projektu
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"project", "hibernateLazyInitializer", "handler"})
    private List<ProjectProduct> projectProducts = new ArrayList<>();
    
    /**
     * RELACJA: Jeden projekt → Wiele grup produktowych (opcje Główna/Opcjonalna)
     * ProjectProductGroup przechowuje opcje dla grup produktowych (np. CANTUS-czarna vs CANTUS-grafitowa)
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"project", "hibernateLazyInitializer", "handler"})
    private List<ProjectProductGroup> projectProductGroups = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Status projektu
     */
    public enum ProjectStatus {
        DRAFT("Szkic"),
        IN_PROGRESS("W trakcie"),
        COMPLETED("Zakończony");
        
        private final String displayName;
        
        ProjectStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}

