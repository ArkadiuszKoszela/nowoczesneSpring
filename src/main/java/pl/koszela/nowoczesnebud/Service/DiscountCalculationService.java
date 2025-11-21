package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Serwis do obliczania końcowego rabatu z 4 składowych rabatów
 * Obsługuje 5 metod obliczania: sumaryczny i 4 warianty kaskadowe
 */
@Service
public class DiscountCalculationService {

    /**
     * Oblicza końcowy rabat na podstawie wybranej metody i 4 składowych rabatów
     * 
     * @param method Metoda obliczania
     * @param basicDiscount Rabat podstawowy (0-100)
     * @param additionalDiscount Rabat dodatkowy (0-100)
     * @param promotionDiscount Rabat promocyjny (0-100)
     * @param skontoDiscount Skonto (0-100)
     * @return Końcowy rabat w procentach (0-100)
     */
    public double calculateDiscount(DiscountCalculationMethod method,
                                   Integer basicDiscount,
                                   Integer additionalDiscount,
                                   Integer promotionDiscount,
                                   Integer skontoDiscount) {
        
        if (method == null) {
            throw new IllegalArgumentException("Metoda obliczania rabatu nie może być null");
        }
        
        // Normalizuj wartości (null -> 0)
        int basic = basicDiscount != null ? basicDiscount : 0;
        int additional = additionalDiscount != null ? additionalDiscount : 0;
        int promotion = promotionDiscount != null ? promotionDiscount : 0;
        int skonto = skontoDiscount != null ? skontoDiscount : 0;
        
        double result = 0.0;
        
        switch (method) {
            case SUMARYCZNY:
                // Sumaryczny: basic + additional + promotion + skonto
                result = basic + additional + promotion + skonto;
                break;
                
            case KASKADOWO_A:
                // Kaskadowo A: podstawowy → dodatkowy → skonto (bez promocyjnego)
                // 100 - 25% = 75 - 10% = 67.5 - 3% = 65.48 → wynik: 34.52%
                double valueA = 100.0;
                valueA = valueA * (1 - basic / 100.0);
                valueA = valueA * (1 - additional / 100.0);
                valueA = valueA * (1 - skonto / 100.0);
                result = 100.0 - valueA;
                break;
                
            case KASKADOWO_B:
                // Kaskadowo B: podstawowy → dodatkowy → promocyjny → skonto
                // 100 - 25% = 75 - 10% = 67.5 - 10% = 60.75 - 3% = 58.93 → wynik: 41.07%
                double valueB = 100.0;
                valueB = valueB * (1 - basic / 100.0);
                valueB = valueB * (1 - additional / 100.0);
                valueB = valueB * (1 - promotion / 100.0);
                valueB = valueB * (1 - skonto / 100.0);
                result = 100.0 - valueB;
                break;
                
            case KASKADOWO_C:
                // Kaskadowo C: podstawowy → (dodatkowy + promocyjny) → skonto
                // 100 - 25% = 75 - (10% + 10%) = 60 - 3% = 58.2 → wynik: 41.8%
                double valueC = 100.0;
                valueC = valueC * (1 - basic / 100.0);
                int additionalPlusPromotion = additional + promotion;
                valueC = valueC * (1 - additionalPlusPromotion / 100.0);
                valueC = valueC * (1 - skonto / 100.0);
                result = 100.0 - valueC;
                break;
                
            case KASKADOWO_D:
                // Kaskadowo D: (podstawowy + dodatkowy + promocyjny) → skonto
                // 25% + 10% + 10% = 45%, 100 - 45% = 55 - 3% = 53.35 → wynik: 46.65%
                int basicPlusAdditionalPlusPromotion = basic + additional + promotion;
                double valueD = 100.0;
                valueD = valueD * (1 - basicPlusAdditionalPlusPromotion / 100.0);
                valueD = valueD * (1 - skonto / 100.0);
                result = 100.0 - valueD;
                break;
        }
        
        // Zaokrąglij do 2 miejsc po przecinku
        return BigDecimal.valueOf(result)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

