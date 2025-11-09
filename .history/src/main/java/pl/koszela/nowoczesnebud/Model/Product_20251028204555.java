package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nazwa produktu z Excela
    @ExcelCellName("name")
    private String name;

    // Producent (z nazwy pliku lub kolumny)
    private String manufacturer;

    // Kategoria produktu (TILE, GUTTER, ACCESSORY)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    // Nazwa grupy produktowej (np. "NUANE", "Ceramic", itp.)
    private String groupName;

    // === CENNIK ===
    // Cena detaliczna (katalogowa)
    @ExcelCell(1)
    @ExcelCellName("detalPrice")
    private Double retailPrice = 0.00;

    // Cena zakupu (po rabacie)
    @ExcelCellName("cena zakupu")
    private Double purchasePrice = 0.00;

    // Cena sprzedaży (z marżą)
    private Double sellingPrice = 0.00;

    // === JEDNOSTKA I PRZELICZNIKI ===
    @ExcelCellName("unit")
    private String unit;

    @ExcelCell(2)
    @ExcelCellName("quantityConverter")
    private Double quantityConverter = 1.0;

    // Ilość w ofercie
    private Double quantity = 0.00;

    // === MAPOWANIE DO INPUTÓW ===
    @ExcelCellName("mapperName")
    private String mapperName;

    // === RABATY ===
    @ExcelCellName("basicDiscount")
    private Integer basicDiscount = 0;

    @ExcelCellName("promotionDiscount")
    private Integer promotionDiscount = 0;

    @ExcelCellName("additionalDiscount")
    private Integer additionalDiscount = 0;

    @ExcelCellName("skonto")
    private Integer skontoDiscount = 0;

    // === MARŻA ===
    @ExcelCellName("procent katalog")
    private Double marginPercent = 0.00;

    // === METADANE ===
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Czy produkt jest opcją główną
    private Boolean isMainOption;

    // Typ akcesoriów (dla kategorii ACCESSORY)
    @ExcelCellName("type")
    private String accessoryType;
}

