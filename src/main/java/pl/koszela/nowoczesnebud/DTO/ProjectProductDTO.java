package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import pl.koszela.nowoczesnebud.Model.PriceChangeSource;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

/**
 * DTO zwracane z backendu - dane produktu zapisane w projekcie
 * Używane przez frontend do wyświetlenia "Starej ceny" vs "Nowej ceny"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectProductDTO {
    private Long productId;
    private ProductCategory category;
    
    // Zapisane ceny (z momentu ostatniego zapisu) - "Stara cena"
    private Double savedRetailPrice;
    private Double savedPurchasePrice;
    private Double savedSellingPrice;
    
    // Zapisana ilość
    private Double savedQuantity;
    
    // Źródło zmiany ceny
    private PriceChangeSource priceChangeSource;
    
    // Zapisane parametry
    private Double savedMarginPercent;
    private Double savedDiscountPercent;
}

