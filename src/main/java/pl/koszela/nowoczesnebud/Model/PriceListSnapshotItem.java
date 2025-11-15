package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model PriceListSnapshotItem - pojedynczy produkt w snapshotcie cennika
 * Przechowuje pełną migawkę danych produktu z momentu utworzenia snapshotu
 */
@Data
@Entity
@Table(name = "price_list_snapshot_items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PriceListSnapshotItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACJA: Wiele pozycji → Jeden snapshot
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_snapshot_id", nullable = false)
    @JsonIgnoreProperties({"items", "hibernateLazyInitializer", "handler"})
    private PriceListSnapshot priceListSnapshot;

    /**
     * Referencja do produktu w cenniku (może być null jeśli produkt został usunięty)
     * NIE foreign key, tylko Long - produkt może być usunięty z cennika
     */
    @Column(name = "product_id")
    private Long productId;

    /**
     * Pełny snapshot danych produktu z momentu utworzenia snapshotu
     */
    private String name;
    private String manufacturer;
    private String groupName;
    
    @Enumerated(EnumType.STRING)
    private ProductCategory category;
    
    private String mapperName;
    
    /**
     * Ceny z momentu snapshotu
     */
    @Column(name = "retail_price")
    private Double retailPrice;
    
    @Column(name = "purchase_price")
    private Double purchasePrice;
    
    @Column(name = "selling_price")
    private Double sellingPrice;
    
    /**
     * Rabaty z momentu snapshotu
     */
    @Column(name = "basic_discount")
    private Integer basicDiscount = 0;
    
    @Column(name = "promotion_discount")
    private Integer promotionDiscount = 0;
    
    @Column(name = "additional_discount")
    private Integer additionalDiscount = 0;
    
    @Column(name = "skonto_discount")
    private Integer skontoDiscount = 0;
    
    /**
     * Marża z momentu snapshotu
     */
    @Column(name = "margin_percent")
    private Double marginPercent = 0.0;
    
    /**
     * Jednostka i konwerter z momentu snapshotu
     */
    private String unit;
    
    @Column(name = "quantity_converter")
    private Double quantityConverter = 1.0;
    
    /**
     * Opcja produktu
     */
    @Column(name = "is_main_option")
    private Boolean isMainOption;

    /**
     * Typ akcesorium (STANDARD, PREMIUM, LUX) - tylko dla category=ACCESSORY
     */
    @Column(name = "accessory_type")
    private String accessoryType;

    /**
     * Data utworzenia pozycji w bazie
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

