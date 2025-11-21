package pl.koszela.nowoczesnebud.Model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model ProjectDraftInput - tymczasowe, niezapisane Input z formularza
 * Przechowuje zmiany w Input do czasu kliknięcia "Zapisz projekt"
 */
@Entity
@Table(name = "project_draft_inputs")
public class ProjectDraftInput {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "mapper_name", nullable = false)
    private String mapperName; // Nazwa pola z formularza (np. "Powierzchnia połaci")
    
    @Column(name = "name")
    private String name; // Nazwa wyświetlana (np. "Powierzchnia połaci")
    
    @Column(name = "quantity")
    private Double quantity; // Wartość Input (np. 200 dla "Powierzchnia połaci")
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ProjectDraftInput() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getMapperName() {
        return mapperName;
    }
    
    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

