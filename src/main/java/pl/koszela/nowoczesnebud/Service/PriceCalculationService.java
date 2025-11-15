package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * LOGIKA BIZNESOWA - BEZ ZMIAN!
 * Wszystkie kalkulacje przeniesione 1:1 z ProductTypeService i QuantityService
 */
@Service
public class PriceCalculationService {

    /**
     * Kalkulacja ceny zakupu po rabatach
     * Źródło: ProductTypeService.calculatePurchasePrice()
     */
    public double calculatePurchasePrice(Product product) {
        double price = product.getRetailPrice();
        if (price == 0.00) {
            return 0.00;
        }

        double purchasePrice = price 
            * calculatePercentage(product.getBasicDiscount())
            * calculatePercentage(product.getAdditionalDiscount())
            * calculatePercentage(product.getPromotionDiscount())
            * calculatePercentage(product.getSkontoDiscount());

        return setScale(purchasePrice);
    }

    /**
     * Kalkulacja ceny detalicznej z marży
     * Źródło: ProductTypeService.calculateDetalPrice()
     */
    public double calculateRetailPrice(Product product) {
        double purchasePrice = product.getPurchasePrice();
        if (purchasePrice == 0.00) {
            return 0.00;
        }

        double retailPrice = purchasePrice * (100 + product.getMarginPercent()) / 100;
        return setScale(retailPrice);
    }

    /**
     * Kalkulacja ceny sprzedaży z marżą
     */
    public double calculateSellingPriceWithMargin(Product product, int marginPercent) {
        double purchasePrice = product.getPurchasePrice();
        double sellingPrice = purchasePrice * (100 + marginPercent) / 100;
        return setScale(sellingPrice);
    }

    /**
     * Kalkulacja ceny sprzedaży z rabatem
     */
    public double calculateSellingPriceWithDiscount(Product product, int discountPercent) {
        double retailPrice = product.getRetailPrice();
        double sellingPrice = retailPrice * (100 - discountPercent) / 100;
        return setScale(sellingPrice);
    }

    /**
     * Kalkulacja ilości z konwerterem
     * Źródło: QuantityService.calculateProductType()
     */
    public double calculateProductQuantity(double inputQuantity, double quantityConverter) {
        double quantity = inputQuantity * quantityConverter;
        return setScale(quantity);
    }

    /**
     * Przeliczenie procentu na współczynnik
     */
    private double calculatePercentage(int discountPercent) {
        return (100 - discountPercent) / 100.0;
    }

    /**
     * Zaokrąglenie - DOKŁADNIE TA SAMA LOGIKA
     * Źródło: QuantityService.setScale()
     */
    public static double setScale(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}






















