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
 * Model Input - dane wejściowe z formularza dla projektu
 * RELACJA: Wiele Inputs → Jeden Project
 * 
 * Input przechowuje TYLKO dane z formularza (np. "Powierzchnia połaci" = 20).
 * Dane produktów są w snapshotach (PriceListSnapshotItem), nie w Input.
 */
@Data
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
     * Wartość z formularza (np. 20 dla "Powierzchnia połaci")
     * Używana do obliczenia quantity produktów: quantity = inputQuantity * quantityConverter
     * Frontend zawsze wysyła liczby (nie stringi/null), więc standardowa deserializacja Double wystarczy
     */
    @Column(nullable = true)
    @JsonProperty
    private Double quantity;

    // ========== OVERRIDE DLA PRODUKTÓW ==========
    /**
     * ID produktu ze snapshotu (jeśli != null, to override ceny/ilości dla produktu)
     * Jeśli productId != null, to ten Input nie jest danymi z formularza, tylko override'em dla produktu
     */
    @Column(name = "product_id", nullable = true)
    @JsonProperty
    private Long productId;
    
    /**
     * Ręczna cena sprzedaży produktu (override dla sellingPrice ze snapshotu)
     * Używane tylko gdy productId != null
     */
    @Column(name = "manual_selling_price", nullable = true)
    @JsonProperty
    private Double manualSellingPrice;
    
    /**
     * Ręczna cena zakupu produktu (override dla purchasePrice ze snapshotu)
     * Używane tylko gdy productId != null
     */
    @Column(name = "manual_purchase_price", nullable = true)
    @JsonProperty
    private Double manualPurchasePrice;
    
    /**
     * Ręczna ilość produktu (override dla automatycznie obliczonej quantity)
     * Używane tylko gdy productId != null
     */
    @Column(name = "manual_quantity", nullable = true)
    @JsonProperty
    private Double manualQuantity;
    
    // ========== OPCJE DLA GRUP PRODUKTÓW ==========
    /**
     * Producent grupy produktów (jeśli != null, to opcja dla grupy)
     * Używane razem z groupName do identyfikacji grupy
     */
    @Column(name = "group_manufacturer", nullable = true)
    @JsonProperty
    private String groupManufacturer;
    
    /**
     * Nazwa grupy produktów (jeśli != null, to opcja dla grupy)
     * Używane razem z groupManufacturer do identyfikacji grupy
     */
    @Column(name = "group_name", nullable = true)
    @JsonProperty
    private String groupName;
    
    /**
     * Opcja dla grupy produktów (Główna/Opcjonalna)
     * true = Główna, false = Opcjonalna, null = Nie wybrano
     * Używane tylko gdy groupManufacturer != null i groupName != null
     */
    @Column(name = "is_main_option", nullable = true)
    @JsonProperty
    private Boolean isMainOption;
    
    /**
     * Kategoria produktu dla Input z productId (TILE, GUTTER, ACCESSORY)
     * Używane do filtrowania Input po kategorii (np. dla akcesoriów)
     */
    @Column(name = "category", nullable = true)
    @JsonProperty
    private String category;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonIgnore
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonIgnore
    private LocalDateTime updatedAt;
}
