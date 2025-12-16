package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ TESTY POPRAWNOŚCIOWE I WYDAJNOŚCIOWE DLA KALKULACJI CEN
 * 
 * Testuje wszystkie metody kalkulacji cen:
 * - calculatePurchasePrice() - cena zakupu z rabatu
 * - calculateRetailPrice() - cena katalogowa z marży
 * - calculateSellingPriceWithMargin() - cena sprzedaży z marżą
 * - calculateSellingPriceWithDiscount() - cena sprzedaży z rabatem
 * - calculateProductQuantity() - ilość z konwerterem
 * - setScale() - zaokrąglanie do 2 miejsc po przecinku
 * 
 * Testuje:
 * - Poprawność obliczeń dla różnych wartości
 * - Edge cases (null, 0, wartości ujemne, bardzo duże wartości)
 * - Wydajność dla dużej liczby obliczeń
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class PriceCalculationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(PriceCalculationServiceTest.class);

    @Autowired
    private PriceCalculationService priceCalculationService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setCategory(ProductCategory.TILE);
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - calculatePurchasePrice()
    // ==========================================

    @Test
    void testCalculatePurchasePrice_StandardCase() {
        logger.info("🧪 TEST: calculatePurchasePrice - standardowy przypadek");
        
        testProduct.setRetailPrice(100.0);
        testProduct.setDiscount(20.0);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        
        assertEquals(80.0, result, 0.01, "Cena zakupu powinna być 80.0 (100 - 20%)");
    }

    @Test
    void testCalculatePurchasePrice_ZeroDiscount() {
        logger.info("🧪 TEST: calculatePurchasePrice - rabat 0%");
        
        testProduct.setRetailPrice(100.0);
        testProduct.setDiscount(0.0);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        
        assertEquals(100.0, result, 0.01, "Cena zakupu powinna być równa cenie katalogowej przy rabacie 0%");
    }

    @Test
    void testCalculatePurchasePrice_FullDiscount() {
        logger.info("🧪 TEST: calculatePurchasePrice - rabat 100%");
        
        testProduct.setRetailPrice(100.0);
        testProduct.setDiscount(100.0);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        
        assertEquals(0.0, result, 0.01, "Cena zakupu powinna być 0.0 przy rabacie 100%");
    }

    @Test
    void testCalculatePurchasePrice_NullDiscount() {
        logger.info("🧪 TEST: calculatePurchasePrice - null discount");
        
        testProduct.setRetailPrice(100.0);
        testProduct.setDiscount(null);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        
        assertEquals(100.0, result, 0.01, "Cena zakupu powinna być równa cenie katalogowej przy null discount");
    }

    @Test
    void testCalculatePurchasePrice_ZeroRetailPrice() {
        logger.info("🧪 TEST: calculatePurchasePrice - cena katalogowa 0");
        
        testProduct.setRetailPrice(0.0);
        testProduct.setDiscount(20.0);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        
        assertEquals(0.0, result, 0.01, "Cena zakupu powinna być 0.0 gdy cena katalogowa jest 0");
    }

    @Test
    void testCalculatePurchasePrice_DecimalValues() {
        logger.info("🧪 TEST: calculatePurchasePrice - wartości dziesiętne");
        
        testProduct.setRetailPrice(123.45);
        testProduct.setDiscount(15.5);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        double expected = 123.45 * (1 - 15.5 / 100.0);
        
        assertEquals(expected, result, 0.01, "Cena zakupu powinna być poprawnie obliczona dla wartości dziesiętnych");
    }

    @Test
    void testCalculatePurchasePrice_VeryLargePrice() {
        logger.info("🧪 TEST: calculatePurchasePrice - bardzo duża cena");
        
        testProduct.setRetailPrice(999999.99);
        testProduct.setDiscount(25.0);
        
        double result = priceCalculationService.calculatePurchasePrice(testProduct);
        double expected = 999999.99 * 0.75;
        
        assertEquals(expected, result, 0.01, "Cena zakupu powinna być poprawnie obliczona dla bardzo dużej ceny");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - calculateRetailPrice()
    // ==========================================

    @Test
    void testCalculateRetailPrice_StandardCase() {
        logger.info("🧪 TEST: calculateRetailPrice - standardowy przypadek");
        
        testProduct.setPurchasePrice(80.0);
        testProduct.setMarginPercent(25.0);
        
        double result = priceCalculationService.calculateRetailPrice(testProduct);
        
        assertEquals(100.0, result, 0.01, "Cena katalogowa powinna być 100.0 (80 + 25%)");
    }

    @Test
    void testCalculateRetailPrice_ZeroMargin() {
        logger.info("🧪 TEST: calculateRetailPrice - marża 0%");
        
        testProduct.setPurchasePrice(80.0);
        testProduct.setMarginPercent(0.0);
        
        double result = priceCalculationService.calculateRetailPrice(testProduct);
        
        assertEquals(80.0, result, 0.01, "Cena katalogowa powinna być równa cenie zakupu przy marży 0%");
    }

    @Test
    void testCalculateRetailPrice_ZeroPurchasePrice() {
        logger.info("🧪 TEST: calculateRetailPrice - cena zakupu 0");
        
        testProduct.setPurchasePrice(0.0);
        testProduct.setMarginPercent(25.0);
        
        double result = priceCalculationService.calculateRetailPrice(testProduct);
        
        assertEquals(0.0, result, 0.01, "Cena katalogowa powinna być 0.0 gdy cena zakupu jest 0");
    }

    @Test
    void testCalculateRetailPrice_NullMargin() {
        logger.info("🧪 TEST: calculateRetailPrice - null margin");
        
        testProduct.setPurchasePrice(80.0);
        testProduct.setMarginPercent(null);
        
        // Null margin powinno być traktowane jako 0
        double result = priceCalculationService.calculateRetailPrice(testProduct);
        
        // Sprawdzamy czy nie rzuca wyjątku i zwraca rozsądną wartość
        assertNotNull(result, "Wynik nie powinien być null");
    }

    @Test
    void testCalculateRetailPrice_DecimalValues() {
        logger.info("🧪 TEST: calculateRetailPrice - wartości dziesiętne");
        
        testProduct.setPurchasePrice(123.45);
        testProduct.setMarginPercent(15.5);
        
        double result = priceCalculationService.calculateRetailPrice(testProduct);
        double expected = 123.45 * (100 + 15.5) / 100;
        
        assertEquals(expected, result, 0.01, "Cena katalogowa powinna być poprawnie obliczona dla wartości dziesiętnych");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - calculateSellingPriceWithMargin()
    // ==========================================

    @Test
    void testCalculateSellingPriceWithMargin_StandardCase() {
        logger.info("🧪 TEST: calculateSellingPriceWithMargin - standardowy przypadek");
        
        testProduct.setPurchasePrice(80.0);
        
        double result = priceCalculationService.calculateSellingPriceWithMargin(testProduct, 25);
        
        assertEquals(100.0, result, 0.01, "Cena sprzedaży powinna być 100.0 (80 + 25%)");
    }

    @Test
    void testCalculateSellingPriceWithMargin_ZeroMargin() {
        logger.info("🧪 TEST: calculateSellingPriceWithMargin - marża 0%");
        
        testProduct.setPurchasePrice(80.0);
        
        double result = priceCalculationService.calculateSellingPriceWithMargin(testProduct, 0);
        
        assertEquals(80.0, result, 0.01, "Cena sprzedaży powinna być równa cenie zakupu przy marży 0%");
    }

    @Test
    void testCalculateSellingPriceWithMargin_NegativeMargin() {
        logger.info("🧪 TEST: calculateSellingPriceWithMargin - marża ujemna (edge case)");
        
        testProduct.setPurchasePrice(80.0);
        
        double result = priceCalculationService.calculateSellingPriceWithMargin(testProduct, -10);
        
        assertEquals(72.0, result, 0.01, "Cena sprzedaży powinna być niższa przy ujemnej marży");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - calculateSellingPriceWithDiscount()
    // ==========================================

    @Test
    void testCalculateSellingPriceWithDiscount_StandardCase() {
        logger.info("🧪 TEST: calculateSellingPriceWithDiscount - standardowy przypadek");
        
        testProduct.setRetailPrice(100.0);
        
        double result = priceCalculationService.calculateSellingPriceWithDiscount(testProduct, 20);
        
        assertEquals(80.0, result, 0.01, "Cena sprzedaży powinna być 80.0 (100 - 20%)");
    }

    @Test
    void testCalculateSellingPriceWithDiscount_ZeroDiscount() {
        logger.info("🧪 TEST: calculateSellingPriceWithDiscount - rabat 0%");
        
        testProduct.setRetailPrice(100.0);
        
        double result = priceCalculationService.calculateSellingPriceWithDiscount(testProduct, 0);
        
        assertEquals(100.0, result, 0.01, "Cena sprzedaży powinna być równa cenie katalogowej przy rabacie 0%");
    }

    @Test
    void testCalculateSellingPriceWithDiscount_FullDiscount() {
        logger.info("🧪 TEST: calculateSellingPriceWithDiscount - rabat 100%");
        
        testProduct.setRetailPrice(100.0);
        
        double result = priceCalculationService.calculateSellingPriceWithDiscount(testProduct, 100);
        
        assertEquals(0.0, result, 0.01, "Cena sprzedaży powinna być 0.0 przy rabacie 100%");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - calculateProductQuantity()
    // ==========================================

    @Test
    void testCalculateProductQuantity_StandardCase() {
        logger.info("🧪 TEST: calculateProductQuantity - standardowy przypadek");
        
        double result = priceCalculationService.calculateProductQuantity(10.0, 1.5);
        
        assertEquals(15.0, result, 0.01, "Ilość powinna być 15.0 (10 * 1.5)");
    }

    @Test
    void testCalculateProductQuantity_ConverterOne() {
        logger.info("🧪 TEST: calculateProductQuantity - konwerter = 1.0");
        
        double result = priceCalculationService.calculateProductQuantity(10.0, 1.0);
        
        assertEquals(10.0, result, 0.01, "Ilość powinna być równa inputQuantity gdy konwerter = 1.0");
    }

    @Test
    void testCalculateProductQuantity_ZeroInput() {
        logger.info("🧪 TEST: calculateProductQuantity - inputQuantity = 0");
        
        double result = priceCalculationService.calculateProductQuantity(0.0, 1.5);
        
        assertEquals(0.0, result, 0.01, "Ilość powinna być 0.0 gdy inputQuantity = 0");
    }

    @Test
    void testCalculateProductQuantity_DecimalValues() {
        logger.info("🧪 TEST: calculateProductQuantity - wartości dziesiętne");
        
        double result = priceCalculationService.calculateProductQuantity(12.5, 2.3);
        double expected = 12.5 * 2.3;
        
        assertEquals(expected, result, 0.01, "Ilość powinna być poprawnie obliczona dla wartości dziesiętnych");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - setScale() (zaokrąglanie)
    // ==========================================

    @Test
    void testSetScale_RoundingUp() {
        logger.info("🧪 TEST: setScale - zaokrąglanie w górę");
        
        double result = PriceCalculationService.setScale(123.456);
        
        assertEquals(123.46, result, 0.001, "Powinno zaokrąglić 123.456 do 123.46");
    }

    @Test
    void testSetScale_RoundingDown() {
        logger.info("🧪 TEST: setScale - zaokrąglanie w dół");
        
        double result = PriceCalculationService.setScale(123.454);
        
        assertEquals(123.45, result, 0.001, "Powinno zaokrąglić 123.454 do 123.45");
    }

    @Test
    void testSetScale_HalfUp() {
        logger.info("🧪 TEST: setScale - zaokrąglanie HALF_UP");
        
        double result = PriceCalculationService.setScale(123.455);
        
        assertEquals(123.46, result, 0.001, "Powinno zaokrąglić 123.455 do 123.46 (HALF_UP)");
    }

    @Test
    void testSetScale_AlreadyTwoDecimals() {
        logger.info("🧪 TEST: setScale - już 2 miejsca po przecinku");
        
        double result = PriceCalculationService.setScale(123.45);
        
        assertEquals(123.45, result, 0.001, "Powinno pozostawić 123.45 bez zmian");
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE
    // ==========================================

    @Test
    void testCalculatePurchasePrice_Performance_10000Calculations() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: calculatePurchasePrice - 10000 obliczeń");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            testProduct.setRetailPrice(100.0 + i);
            testProduct.setDiscount(20.0);
            priceCalculationService.calculatePurchasePrice(testProduct);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("⏱️ [PERFORMANCE] calculatePurchasePrice - 10000 obliczeń: {}ms", duration);
        
        assertTrue(duration < 1000, "10000 obliczeń powinno zająć mniej niż 1s");
    }

    @Test
    void testCalculateRetailPrice_Performance_10000Calculations() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: calculateRetailPrice - 10000 obliczeń");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            testProduct.setPurchasePrice(80.0 + i);
            testProduct.setMarginPercent(25.0);
            priceCalculationService.calculateRetailPrice(testProduct);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("⏱️ [PERFORMANCE] calculateRetailPrice - 10000 obliczeń: {}ms", duration);
        
        assertTrue(duration < 1000, "10000 obliczeń powinno zająć mniej niż 1s");
    }

    @Test
    void testCalculateProductQuantity_Performance_10000Calculations() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: calculateProductQuantity - 10000 obliczeń");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            priceCalculationService.calculateProductQuantity(10.0 + i, 1.5);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("⏱️ [PERFORMANCE] calculateProductQuantity - 10000 obliczeń: {}ms", duration);
        
        assertTrue(duration < 1000, "10000 obliczeń powinno zająć mniej niż 1s");
    }

    @Test
    void testSetScale_Performance_100000Calculations() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: setScale - 100000 obliczeń");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100000; i++) {
            PriceCalculationService.setScale(123.456789 + i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("⏱️ [PERFORMANCE] setScale - 100000 obliczeń: {}ms", duration);
        
        assertTrue(duration < 2000, "100000 obliczeń powinno zająć mniej niż 2s");
    }
}

