package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Serwis odpowiedzialny za kalkulacje cen, rabatów i marż
 * LOGIKA BIZNESOWA - BEZ ZMIAN z oryginalnej implementacji
 */
@Service
public class PriceCalculationService {

    /**
     * Kalkulacja ceny zakupu po uwzględnieniu wszystkich rabatów
     * Odpowiednik: ProductTypeService.calculatePurchasePrice()
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
     * Kalkulacja ceny detalicznej na podstawie ceny zakupu i marży
     * Odpowiednik: ProductTypeService.calculateDetalPrice()
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
     * Kalkulacja ceny sprzedaży z rabatem od ceny detalicznej
     */
    public double calculateSellingPriceWithDiscount(Product product, int discountPercent) {
        double retailPrice = product.getRetailPrice();
        double sellingPrice = retailPrice * (100 - discountPercent) / 100;
        return setScale(sellingPrice);
    }

    /**
     * Kalkulacja ilości produktu na podstawie inputu
     * Odpowiednik: QuantityService.calculateProductType()
     */
    public double calculateProductQuantity(double inputQuantity, double quantityConverter) {
        double quantity = inputQuantity * quantityConverter;
        return setScale(quantity);
    }

    /**
     * Kalkulacja całkowitej ceny detalicznej dla produktu
     */
    public double calculateTotalRetailPrice(Product product) {
        return setScale(product.getRetailPrice() * product.getQuantity());
    }

    /**
     * Kalkulacja całkowitej ceny zakupu dla produktu
     */
    public double calculateTotalPurchasePrice(Product product) {
        return setScale(product.getPurchasePrice() * product.getQuantity());
    }

    /**
     * Kalkulacja całkowitej ceny sprzedaży dla produktu
     */
    public double calculateTotalSellingPrice(Product product) {
        return setScale(product.getSellingPrice() * product.getQuantity());
    }

    /**
     * Kalkulacja zysku (marża)
     */
    public double calculateProfit(Product product) {
        double totalRetail = calculateTotalRetailPrice(product);
        double totalPurchase = calculateTotalPurchasePrice(product);
        return setScale(totalRetail - totalPurchase);
    }

    /**
     * Przeliczenie procentu na współczynnik
     * Np. 10% -> 0.90 (czyli 100% - 10% = 90%)
     */
    private double calculatePercentage(int discountPercent) {
        return (100 - discountPercent) / 100.0;
    }

    /**
     * Zaokrąglenie do 2 miejsc po przecinku
     * DOKŁADNIE TA SAMA LOGIKA co QuantityService.setScale()
     */
    public static double setScale(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}

