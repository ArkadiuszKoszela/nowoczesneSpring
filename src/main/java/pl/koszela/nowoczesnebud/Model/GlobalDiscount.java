package pl.koszela.nowoczesnebud.Model;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * Rabat globalny dla całej kategorii produktów (TILE, GUTTER, ACCESSORY)
 * Może być typu:
 * - MAIN (Główny/Ogólny) - standardowy rabat dla wszystkich produktów
 * - OPTIONAL (Opcjonalny) - dodatkowy rabat opcjonalny
 */
@Entity
@Table(name = "global_discounts")
public class GlobalDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category; // TILE, GUTTER, ACCESSORY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type; // MAIN, OPTIONAL

    @Column(nullable = false)
    private Double discountPercent; // Procent rabatu (np. 15.0 = 15%)

    @Column(length = 500)
    private String description; // Opis rabatu (opcjonalnie)

    @Column(nullable = false)
    private LocalDate validFrom; // Data od kiedy obowiązuje

    @Column(nullable = false)
    private LocalDate validTo; // Data do kiedy obowiązuje

    @Column(nullable = false)
    private Boolean active = true; // Czy rabat jest aktywny

    public enum DiscountType {
        MAIN,      // Rabat główny/ogólny
        OPTIONAL   // Rabat opcjonalny
    }

    // Constructors
    public GlobalDiscount() {}

    public GlobalDiscount(ProductCategory category, DiscountType type, Double discountPercent, 
                         LocalDate validFrom, LocalDate validTo) {
        this.category = category;
        this.type = type;
        this.discountPercent = discountPercent;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public DiscountType getType() { return type; }
    public void setType(DiscountType type) { this.type = type; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    /**
     * Sprawdza czy rabat jest aktualnie ważny
     */
    public boolean isCurrentlyValid() {
        if (!active) return false;
        LocalDate now = LocalDate.now();
        return !now.isBefore(validFrom) && !now.isAfter(validTo);
    }

    @Override
    public String toString() {
        return "GlobalDiscount{" +
                "id=" + id +
                ", category=" + category +
                ", type=" + type +
                ", discountPercent=" + discountPercent +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", active=" + active +
                '}';
    }
}














