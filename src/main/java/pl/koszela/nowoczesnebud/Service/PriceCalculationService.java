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
     * Kalkulacja ceny zakupu po rabacie
     * Cena zakupu = cena katalogowa * (1 - rabat/100)
     */
    public double calculatePurchasePrice(Product product) {
        double price = product.getRetailPrice();
        if (price == 0.00) {
            return 0.00;
        }

        double discountPercent = product.getDiscount() != null ? product.getDiscount() : 0.0;
        double purchasePrice = price * (1 - discountPercent / 100.0);

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

        // Jeśli marginPercent jest null, użyj 0 jako domyślnej wartości
        double marginPercent = product.getMarginPercent() != null ? product.getMarginPercent() : 0.0;
        double retailPrice = purchasePrice * (100 + marginPercent) / 100;
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
     * Zaokrąglenie - DOKŁADNIE TA SAMA LOGIKA
     * Źródło: QuantityService.setScale()
     */
    public static double setScale(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}

























