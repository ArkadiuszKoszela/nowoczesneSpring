package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import pl.koszela.nowoczesnebud.Model.PriceChangeSource;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

/**
 * DTO dla zapisu danych produktu w projekcie
 * Wysyłane z frontendu podczas kliknięcia "Zapisz projekt"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveProjectProductDTO {
    private Long productId;
    private ProductCategory category;
    
    // Zapisane ceny (z momentu ostatniego zapisu)
    private Double savedRetailPrice;
    private Double savedPurchasePrice;
    private Double savedSellingPrice;
    
    // Zapisana ilość
    private Double savedQuantity;
    
    // Źródło zmiany ceny
    private PriceChangeSource priceChangeSource; // AUTO, MARGIN, DISCOUNT, MANUAL
    
    // Zapisane parametry dla przeliczenia
    private Double savedMarginPercent;
    private Double savedDiscountPercent;
}

