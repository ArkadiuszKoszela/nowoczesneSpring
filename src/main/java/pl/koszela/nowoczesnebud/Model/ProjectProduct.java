package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model ProjectProduct - zapisane ceny i ilości produktów w projekcie
 * RELACJA: Wiele ProjectProduct → Jeden Project
 * 
 * Przechowuje:
 * - Zapisane ceny z momentu ostatniego zapisu projektu (savedRetailPrice, savedPurchasePrice, savedSellingPrice)
 * - Zapisaną ilość produktu (savedQuantity)
 * - Źródło zmiany ceny (priceChangeSource)
 * - Parametry dla przeliczenia (savedMarginPercent, savedDiscountPercent)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_products", indexes = {
    @Index(name = "idx_project_product_project_category", columnList = "project_id,category"),
    @Index(name = "idx_project_product_product_id", columnList = "product_id")
})
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class ProjectProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty
    private Long id;
    
    /**
     * RELACJA: Wiele ProjectProduct → Jeden Project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;
    
    /**
     * ID produktu z tabeli Product (cennik)
     * Używane do identyfikacji produktu i porównania z aktualnym cennikiem
     */
    @Column(name = "product_id", nullable = false)
    @JsonProperty
    private Long productId;
    
    /**
     * Kategoria produktu (TILE, GUTTER, ACCESSORY)
     * Używane do filtrowania produktów w zakładkach
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty
    private ProductCategory category;
    
    // ========== ZAPISANE CENY (ostatni zapis projektu) ==========
    
    /**
     * Zapisana cena katalogowa (z momentu ostatniego zapisu projektu)
     * Porównywana z aktualną ceną katalogową z cennika
     */
    @Column(name = "saved_retail_price")
    @JsonProperty
    private Double savedRetailPrice;
    
    /**
     * Zapisana cena zakupu (z momentu ostatniego zapisu projektu)
     * Porównywana z aktualną ceną zakupu z cennika
     */
    @Column(name = "saved_purchase_price")
    @JsonProperty
    private Double savedPurchasePrice;
    
    /**
     * Zapisana cena sprzedaży (z momentu ostatniego zapisu projektu)
     * Może być zmieniona przez użytkownika (Marża/Rabat/ręcznie)
     */
    @Column(name = "saved_selling_price")
    @JsonProperty
    private Double savedSellingPrice;
    
    // ========== ZAPISANA ILOŚĆ ==========
    
    /**
     * Zapisana ilość produktu (z momentu ostatniego zapisu projektu)
     * Może być zmieniona ręcznie przez użytkownika
     */
    @Column(name = "saved_quantity")
    @JsonProperty
    private Double savedQuantity;
    
    // ========== ŹRÓDŁO ZMIANY CENY ==========
    
    /**
     * Źródło zmiany ceny sprzedaży
     * AUTO - z cennika (domyślnie)
     * MARGIN - obliczona przez przycisk "Marża"
     * DISCOUNT - obliczona przez przycisk "Rabat"
     * MANUAL - zmieniona ręcznie
     * 
     * Używane do określenia koloru wiersza w UI:
     * - AUTO: biały
     * - MARGIN: zielony
     * - DISCOUNT: żółty
     * - MANUAL: ostrzegawczy (czerwony)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_change_source")
    @JsonProperty
    private PriceChangeSource priceChangeSource;
    
    // ========== PARAMETRY DLA PRZELICZENIA ==========
    
    /**
     * Zapisana marża (%) użyta do obliczenia ceny sprzedaży
     * Używane gdy priceChangeSource = MARGIN
     * Cena sprzedaży = savedPurchasePrice * (1 + savedMarginPercent/100)
     */
    @Column(name = "saved_margin_percent")
    @JsonProperty
    private Double savedMarginPercent;
    
    /**
     * Zapisany rabat (%) użyty do obliczenia ceny sprzedaży
     * Używane gdy priceChangeSource = DISCOUNT
     * Cena sprzedaży = savedRetailPrice * (1 - savedDiscountPercent/100)
     */
    @Column(name = "saved_discount_percent")
    @JsonProperty
    private Double savedDiscountPercent;
    
    @CreationTimestamp
    @Column(name = "created_at")
    @JsonIgnore
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonIgnore
    private LocalDateTime updatedAt;
}

