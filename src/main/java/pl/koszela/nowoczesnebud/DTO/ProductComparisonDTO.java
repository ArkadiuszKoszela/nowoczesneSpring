package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod;
import pl.koszela.nowoczesnebud.Model.PriceChangeSource;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

/**
 * DTO dla porównania cen produktu (Stara vs Nowa cena)
 * Używane w zakładkach Dachówki/Rynny/Akcesoria do pokazania zmian cen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductComparisonDTO {
    
    // ========== IDENTYFIKACJA PRODUKTU ==========
    private Long productId;
    private String name;
    private String manufacturer;
    private String groupName;
    private ProductCategory category;
    private String unit;
    private Double quantityConverter;
    private String mapperName;
    
    // ========== AKTUALNE CENY (z cennika) ==========
    private Double currentRetailPrice;     // Cena katalogowa z cennika
    private Double currentPurchasePrice;   // Cena zakupu z cennika
    private Double currentSellingPrice;    // Cena sprzedaży z cennika
    
    // ========== ZAPISANE CENY (z ProjectProduct) ==========
    private Double savedRetailPrice;       // Ostatnio zapisana cena katalogowa
    private Double savedPurchasePrice;     // Ostatnio zapisana cena zakupu
    private Double savedSellingPrice;      // Ostatnio zapisana cena sprzedaży
    private Double savedQuantity;          // Ostatnio zapisana ilość
    
    // ========== ŹRÓDŁO ZMIAN ==========
    private PriceChangeSource priceChangeSource;  // AUTO/MARGIN/DISCOUNT/MANUAL
    private Double savedMarginPercent;     // Zapisana marża %
    private Double savedDiscountPercent;   // Zapisany rabat %
    
    // ========== FLAGI ZMIAN ==========
    private Boolean priceChanged;          // true jeśli currentPrice != savedPrice
    private Boolean quantityChanged;       // true jeśli currentQuantity != savedQuantity
    
    // ========== RABAT Z CENNIKA ==========
    private Double discount;  // Jeden rabat (procent) - obliczany z 4 rabatów w zarządzaniu rabatami
    private DiscountCalculationMethod discountCalculationMethod;  // Metoda obliczania końcowego rabatu
    private Double marginPercent;
    
    // ========== DRAFT CHANGES (tymczasowe, niezapisane) ==========
    private Double draftRetailPrice;       // Tymczasowa cena katalogowa
    private Double draftPurchasePrice;     // Tymczasowa cena zakupu
    private Double draftSellingPrice;      // Tymczasowa cena sprzedaży
    private Double draftQuantity;          // Tymczasowa ilość
    private Double draftMarginPercent;     // Tymczasowa marża % (dla produktu)
    private Double draftDiscountPercent;   // Tymczasowy rabat % (dla produktu)
    
    // ========== DRAFT CHANGES - MARŻA/RABAT KATEGORII ==========
    // ⚠️ WAŻNE: Marża/rabat kategorii z draft changes (wszystkie produkty mają tę samą wartość)
    // Używane do przywrócenia marży/rabatu w UI po odświeżeniu strony
    private Double categoryDraftMarginPercent;     // Tymczasowa marża % dla całej kategorii
    private Double categoryDraftDiscountPercent;   // Tymczasowy rabat % dla całej kategorii
}



