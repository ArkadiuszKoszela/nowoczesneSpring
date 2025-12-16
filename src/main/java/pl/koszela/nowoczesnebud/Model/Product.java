package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * NOWY UPROSZCZONY MODEL PRODUKTU
 * Zastępuje: Tile + Gutter + Accessory + ProductType
 */
@Data
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_mapper_name", columnList = "mapperName")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nazwa produktu z Excela
    @ExcelCellName("name")
    private String name;

    // Producent (z nazwy pliku)
    private String manufacturer;

    // Kategoria (TILE, GUTTER, ACCESSORY)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    // Nazwa grupy (z nazwy pliku: "CANTUS-NUANE" -> groupName = "NUANE")
    private String groupName;

    // === CENNIK ===
    @ExcelCell(1)
    @ExcelCellName("unitDetalP")  // Stara nazwa dla kompatybilności z importem
    private Double retailPrice = 0.00;

    @ExcelCellName("cena zakupu")
    private Double purchasePrice = 0.00;

    private Double sellingPrice = 0.00;

    // === JEDNOSTKA ===
    @ExcelCellName("unit")
    private String unit;

    @ExcelCell(2)
    @ExcelCellName("quantityCo")  // Stara nazwa dla kompatybilności z importem
    private Double quantityConverter = 1.0;

    private Double quantity = 0.00;

    // === MAPOWANIE ===
    @ExcelCellName("mapperName")
    private String mapperName;

    // === RABAT ===
    @ExcelCellName("rabat")  // Nazwa dla kompatybilności z importem
    private Double discount = 0.0;  // Jeden rabat (procent) - obliczany z 4 rabatów w zarządzaniu rabatami
    
    // === METODA OBLICZANIA RABATU ===
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_calculation_method")
    private DiscountCalculationMethod discountCalculationMethod;  // Metoda obliczania końcowego rabatu
    
    // === SKŁADOWE RABATY (dla zarządzania i importu/exportu) ===
    @ExcelCellName("basicDisc")  // Stara nazwa dla kompatybilności z importem
    private Integer basicDiscount = 0;
    
    @ExcelCellName("promotion")  // Stara nazwa dla kompatybilności z importem
    private Integer promotionDiscount = 0;
    
    @ExcelCellName("additional")  // Stara nazwa dla kompatybilności z importem
    private Integer additionalDiscount = 0;
    
    @ExcelCellName("skonto")
    private Integer skontoDiscount = 0;

    // === MARŻA ===
    @ExcelCellName("procent katalog")
    private Double marginPercent = 0.00;

    // === OPCJE ===
    // ⚠️ UWAGA: To pole jest tylko przejściowe (transient) - nie jest zapisywane w bazie
    // Używane do wyświetlania w UI i filtrowania produktów
    @Transient
    private GroupOption isMainOption;

    // === TIMESTAMPS ===
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Typ akcesoriów
    @Column(name = "accessory_type")
    @ExcelCellName("type")
    private String accessoryType;

    // Typ produktu (dla grupowania - głównie dla Dachówek)
    @Column(name = "product_type")
    @ExcelCellName("productType")
    private String productType;

    // === RABATY GLOBALNE (nie zapisywane w bazie - wypełniane dynamicznie) ===
    @Transient
    private Double globalMainDiscount; // Rabat główny/ogólny (procent)

    @Transient
    private Double globalOptionalDiscount; // Rabat opcjonalny (procent)

    @Transient
    private boolean hasGlobalDiscount; // Czy ma jakikolwiek rabat globalny
    
    // === OVERRIDE DLA PROJEKTU (nie zapisywane w bazie - wypełniane dynamicznie) ===
    @Transient
    private Boolean isManualPrice; // Czy cena sprzedaży była zmieniona ręcznie w projekcie
    
    @Transient
    private Double originalSellingPrice; // Oryginalna cena ze snapshotu (dla porównania)
    
    @Transient
    private Boolean isManualPurchasePrice; // Czy cena zakupu była zmieniona ręcznie w projekcie
    
    @Transient
    private Double originalPurchasePrice; // Oryginalna cena zakupu ze snapshotu (dla porównania)
    
    @Transient
    private Boolean isManualQuantity; // Czy ilość była zmieniona ręcznie w projekcie
    
    @Transient
    private Double originalQuantity; // Oryginalna ilość obliczona automatycznie (dla porównania)
}


