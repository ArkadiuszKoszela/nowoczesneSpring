package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ TESTY POPRAWNOŚCIOWE I WYDAJNOŚCIOWE DLA OBLICZANIA RABATÓW
 * 
 * Testuje wszystkie 5 metod obliczania rabatów:
 * - SUMARYCZNY: basic + additional + promotion + skonto
 * - KASKADOWO_A: podstawowy → dodatkowy → skonto (bez promocyjnego)
 * - KASKADOWO_B: podstawowy → dodatkowy → promocyjny → skonto
 * - KASKADOWO_C: podstawowy → (dodatkowy + promocyjny) → skonto
 * - KASKADOWO_D: (podstawowy + dodatkowy + promocyjny) → skonto
 * 
 * Testuje:
 * - Poprawność obliczeń dla różnych wartości
 * - Edge cases (null, wartości >100%, wartości ujemne)
 * - Porównanie wyników różnych metod
 * - Wydajność dla dużej liczby obliczeń
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class DiscountCalculationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DiscountCalculationServiceTest.class);

    @Autowired
    private DiscountCalculationService discountCalculationService;

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - SUMARYCZNY
    // ==========================================

    @Test
    void testCalculateDiscount_SUMARYCZNY_StandardCase() {
        logger.info("🧪 TEST: SUMARYCZNY - standardowy przypadek");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY,
            25, 10, 10, 3
        );
        
        assertEquals(48.0, result, 0.01, "Sumaryczny: 25% + 10% + 10% + 3% = 48%");
    }

    @Test
    void testCalculateDiscount_SUMARYCZNY_ZeroDiscounts() {
        logger.info("🧪 TEST: SUMARYCZNY - wszystkie rabaty = 0");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY,
            0, 0, 0, 0
        );
        
        assertEquals(0.0, result, 0.01, "Sumaryczny: 0% + 0% + 0% + 0% = 0%");
    }

    @Test
    void testCalculateDiscount_SUMARYCZNY_NullValues() {
        logger.info("🧪 TEST: SUMARYCZNY - null wartości");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY,
            null, null, null, null
        );
        
        assertEquals(0.0, result, 0.01, "Sumaryczny: null powinno być traktowane jako 0");
    }

    @Test
    void testCalculateDiscount_SUMARYCZNY_Over100Percent() {
        logger.info("🧪 TEST: SUMARYCZNY - suma > 100% (edge case)");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY,
            50, 30, 25, 10
        );
        
        assertEquals(115.0, result, 0.01, "Sumaryczny: 50% + 30% + 25% + 10% = 115% (może być > 100%)");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - KASKADOWO_A
    // ==========================================

    @Test
    void testCalculateDiscount_KASKADOWO_A_StandardCase() {
        logger.info("🧪 TEST: KASKADOWO_A - standardowy przypadek");
        
        // 100 - 25% = 75 - 10% = 67.5 - 3% = 65.48 → wynik: 34.52%
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_A,
            25, 10, 0, 3
        );
        
        assertEquals(34.52, result, 0.1, "KASKADOWO_A: podstawowy → dodatkowy → skonto");
    }

    @Test
    void testCalculateDiscount_KASKADOWO_A_IgnoresPromotion() {
        logger.info("🧪 TEST: KASKADOWO_A - ignoruje promotion");
        
        double resultWithPromotion = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_A,
            25, 10, 10, 3
        );
        
        double resultWithoutPromotion = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_A,
            25, 10, 0, 3
        );
        
        assertEquals(resultWithoutPromotion, resultWithPromotion, 0.01, 
                    "KASKADOWO_A powinno ignorować promotion");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - KASKADOWO_B
    // ==========================================

    @Test
    void testCalculateDiscount_KASKADOWO_B_StandardCase() {
        logger.info("🧪 TEST: KASKADOWO_B - standardowy przypadek");
        
        // 100 - 25% = 75 - 10% = 67.5 - 10% = 60.75 - 3% = 58.93 → wynik: 41.07%
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_B,
            25, 10, 10, 3
        );
        
        assertEquals(41.07, result, 0.1, "KASKADOWO_B: podstawowy → dodatkowy → promocyjny → skonto");
    }

    @Test
    void testCalculateDiscount_KASKADOWO_B_AllDiscounts() {
        logger.info("🧪 TEST: KASKADOWO_B - wszystkie rabaty");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_B,
            30, 15, 10, 5
        );
        
        assertTrue(result > 0 && result < 100, "KASKADOWO_B powinno zwrócić wartość między 0 a 100%");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - KASKADOWO_C
    // ==========================================

    @Test
    void testCalculateDiscount_KASKADOWO_C_StandardCase() {
        logger.info("🧪 TEST: KASKADOWO_C - standardowy przypadek");
        
        // 100 - 25% = 75 - (10% + 10%) = 60 - 3% = 58.2 → wynik: 41.8%
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_C,
            25, 10, 10, 3
        );
        
        assertEquals(41.8, result, 0.1, "KASKADOWO_C: podstawowy → (dodatkowy + promocyjny) → skonto");
    }

    @Test
    void testCalculateDiscount_KASKADOWO_C_SumAdditionalAndPromotion() {
        logger.info("🧪 TEST: KASKADOWO_C - sumuje additional i promotion");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_C,
            20, 10, 10, 0
        );
        
        // Powinno być różne od KASKADOWO_B gdzie są osobno
        double resultB = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_B,
            20, 10, 10, 0
        );
        
        assertNotEquals(result, resultB, 0.01, 
                       "KASKADOWO_C powinno dawać inny wynik niż KASKADOWO_B");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - KASKADOWO_D
    // ==========================================

    @Test
    void testCalculateDiscount_KASKADOWO_D_StandardCase() {
        logger.info("🧪 TEST: KASKADOWO_D - standardowy przypadek");
        
        // 25% + 10% + 10% = 45%, 100 - 45% = 55 - 3% = 53.35 → wynik: 46.65%
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_D,
            25, 10, 10, 3
        );
        
        assertEquals(46.65, result, 0.1, "KASKADOWO_D: (podstawowy + dodatkowy + promocyjny) → skonto");
    }

    @Test
    void testCalculateDiscount_KASKADOWO_D_SumFirstThree() {
        logger.info("🧪 TEST: KASKADOWO_D - sumuje pierwsze trzy rabaty");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_D,
            20, 15, 10, 5
        );
        
        // Powinno być różne od innych metod
        double resultSumaryczny = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY,
            20, 15, 10, 5
        );
        
        assertNotEquals(result, resultSumaryczny, 0.01, 
                       "KASKADOWO_D powinno dawać inny wynik niż SUMARYCZNY");
    }

    // ==========================================
    // TESTY PORÓWNANIA METOD
    // ==========================================

    @Test
    void testCompareAllMethods_SameInputs() {
        logger.info("🧪 TEST: Porównanie wszystkich metod dla tych samych danych");
        
        int basic = 25;
        int additional = 10;
        int promotion = 10;
        int skonto = 3;
        
        double sumaryczny = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY, basic, additional, promotion, skonto);
        double kaskadowoA = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_A, basic, additional, promotion, skonto);
        double kaskadowoB = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_B, basic, additional, promotion, skonto);
        double kaskadowoC = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_C, basic, additional, promotion, skonto);
        double kaskadowoD = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.KASKADOWO_D, basic, additional, promotion, skonto);
        
        logger.info("📊 Wyniki dla basic={}, additional={}, promotion={}, skonto={}:", 
                   basic, additional, promotion, skonto);
        logger.info("   SUMARYCZNY: {}%", sumaryczny);
        logger.info("   KASKADOWO_A: {}%", kaskadowoA);
        logger.info("   KASKADOWO_B: {}%", kaskadowoB);
        logger.info("   KASKADOWO_C: {}%", kaskadowoC);
        logger.info("   KASKADOWO_D: {}%", kaskadowoD);
        
        // SUMARYCZNY powinien dać najwyższy wynik
        assertTrue(sumaryczny >= kaskadowoA, "SUMARYCZNY powinien dać >= KASKADOWO_A");
        assertTrue(sumaryczny >= kaskadowoB, "SUMARYCZNY powinien dać >= KASKADOWO_B");
        assertTrue(sumaryczny >= kaskadowoC, "SUMARYCZNY powinien dać >= KASKADOWO_C");
        assertTrue(sumaryczny >= kaskadowoD, "SUMARYCZNY powinien dać >= KASKADOWO_D");
        
        // Wszystkie metody powinny dać różne wyniki (dla tych danych)
        assertNotEquals(kaskadowoA, kaskadowoB, 0.01, "KASKADOWO_A i KASKADOWO_B powinny dać różne wyniki");
        assertNotEquals(kaskadowoB, kaskadowoC, 0.01, "KASKADOWO_B i KASKADOWO_C powinny dać różne wyniki");
        assertNotEquals(kaskadowoC, kaskadowoD, 0.01, "KASKADOWO_C i KASKADOWO_D powinny dać różne wyniki");
    }

    // ==========================================
    // TESTY EDGE CASES
    // ==========================================

    @Test
    void testCalculateDiscount_NullMethod() {
        logger.info("🧪 TEST: Edge case - null method");
        
        assertThrows(IllegalArgumentException.class, () -> {
            discountCalculationService.calculateDiscount(null, 25, 10, 10, 3);
        }, "Powinno rzucić IllegalArgumentException dla null method");
    }

    @Test
    void testCalculateDiscount_NegativeDiscounts() {
        logger.info("🧪 TEST: Edge case - ujemne rabaty");
        
        // Ujemne rabaty mogą być traktowane jako 0 lub mogą być dozwolone
        // Sprawdzamy czy nie rzuca wyjątku
        assertDoesNotThrow(() -> {
            double result = discountCalculationService.calculateDiscount(
                DiscountCalculationMethod.SUMARYCZNY, -10, -5, 0, 0);
            logger.info("   Wynik dla ujemnych rabatów: {}%", result);
        }, "Nie powinno rzucać wyjątku dla ujemnych rabatów");
    }

    @Test
    void testCalculateDiscount_VeryLargeDiscounts() {
        logger.info("🧪 TEST: Edge case - bardzo duże rabaty");
        
        double result = discountCalculationService.calculateDiscount(
            DiscountCalculationMethod.SUMARYCZNY, 200, 100, 50, 25);
        
        assertEquals(375.0, result, 0.01, "Powinno obsłużyć bardzo duże rabaty");
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE
    // ==========================================

    @Test
    void testCalculateDiscount_Performance_AllMethods_10000Calculations() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: Wszystkie metody - 10000 obliczeń");
        
        DiscountCalculationMethod[] methods = {
            DiscountCalculationMethod.SUMARYCZNY,
            DiscountCalculationMethod.KASKADOWO_A,
            DiscountCalculationMethod.KASKADOWO_B,
            DiscountCalculationMethod.KASKADOWO_C,
            DiscountCalculationMethod.KASKADOWO_D
        };
        
        long totalStartTime = System.currentTimeMillis();
        
        for (DiscountCalculationMethod method : methods) {
            long methodStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < 2000; i++) {
                discountCalculationService.calculateDiscount(method, 25, 10, 10, 3);
            }
            
            long methodEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] {} - 2000 obliczeń: {}ms", method, methodEndTime - methodStartTime);
        }
        
        long totalEndTime = System.currentTimeMillis();
        long totalDuration = totalEndTime - totalStartTime;
        
        logger.info("⏱️ [PERFORMANCE] Wszystkie metody - 10000 obliczeń łącznie: {}ms", totalDuration);
        
        assertTrue(totalDuration < 2000, "10000 obliczeń powinno zająć mniej niż 2s");
    }

    @Test
    void testCalculateDiscount_Performance_ComplexCalculations_5000Calculations() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: Złożone obliczenia - 5000 obliczeń");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 5000; i++) {
            // Różne kombinacje rabatów
            int basic = i % 50;
            int additional = (i * 2) % 30;
            int promotion = (i * 3) % 20;
            int skonto = i % 10;
            
            discountCalculationService.calculateDiscount(
                DiscountCalculationMethod.KASKADOWO_B, basic, additional, promotion, skonto);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("⏱️ [PERFORMANCE] Złożone obliczenia - 5000 obliczeń: {}ms", duration);
        
        assertTrue(duration < 1000, "5000 obliczeń powinno zająć mniej niż 1s");
    }
}

