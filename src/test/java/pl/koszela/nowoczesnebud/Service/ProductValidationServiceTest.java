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
import pl.koszela.nowoczesnebud.Service.ProductValidationService.BatchValidationResult;
import pl.koszela.nowoczesnebud.Service.ProductValidationService.ValidationResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ TESTY POPRAWNOŚCIOWE I WYDAJNOŚCIOWE DLA WALIDACJI PRODUKTÓW
 * 
 * Testuje walidację produktów:
 * - validate() - walidacja pojedynczego produktu
 * - validateBatch() - walidacja wielu produktów
 * 
 * Testuje:
 * - Poprawność wykrywania błędów (cena sprzedaży < cena zakupu, suma rabatów > 100%, ceny ujemne)
 * - Poprawność wykrywania ostrzeżeń (bardzo wysokie rabaty, brak ceny katalogowej)
 * - Edge cases (null wartości, wartości graniczne)
 * - Wydajność dla dużej liczby produktów (1000+)
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class ProductValidationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductValidationServiceTest.class);

    @Autowired
    private ProductValidationService productValidationService;

    private Product validProduct;

    @BeforeEach
    void setUp() {
        validProduct = new Product();
        validProduct.setName("Test Product");
        validProduct.setCategory(ProductCategory.TILE);
        validProduct.setRetailPrice(100.0);
        validProduct.setPurchasePrice(80.0);
        validProduct.setSellingPrice(100.0);
        validProduct.setBasicDiscount(10);
        validProduct.setAdditionalDiscount(5);
        validProduct.setPromotionDiscount(3);
        validProduct.setSkontoDiscount(2);
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - validate() - Błędy
    // ==========================================

    @Test
    void testValidate_ValidProduct() {
        logger.info("🧪 TEST: validate - poprawny produkt");
        
        ValidationResult result = productValidationService.validate(validProduct);
        
        assertTrue(result.isValid(), "Poprawny produkt powinien przejść walidację");
        assertTrue(result.getErrors().isEmpty(), "Nie powinno być błędów");
    }

    @Test
    void testValidate_Error_SellingPriceLowerThanPurchasePrice() {
        logger.info("🧪 TEST: validate - błąd: cena sprzedaży < cena zakupu");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(100.0);
        product.setPurchasePrice(80.0);
        product.setSellingPrice(70.0); // STRATA!
        
        ValidationResult result = productValidationService.validate(product);
        
        assertFalse(result.isValid(), "Produkt ze stratą nie powinien przejść walidacji");
        assertTrue(result.getErrors().size() > 0, "Powinien być błąd");
        assertTrue(result.getErrors().get(0).contains("STRATA"), 
                   "Błąd powinien zawierać słowo 'STRATA'");
    }

    @Test
    void testValidate_Error_TotalDiscountOver100() {
        logger.info("🧪 TEST: validate - błąd: suma rabatów > 100%");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(100.0);
        product.setBasicDiscount(50);
        product.setAdditionalDiscount(30);
        product.setPromotionDiscount(25);
        product.setSkontoDiscount(10); // Suma: 115%
        
        ValidationResult result = productValidationService.validate(product);
        
        assertFalse(result.isValid(), "Produkt z sumą rabatów > 100% nie powinien przejść walidacji");
        assertTrue(result.getErrors().size() > 0, "Powinien być błąd");
        assertTrue(result.getErrors().get(0).contains("przekracza 100%"), 
                   "Błąd powinien informować o przekroczeniu 100%");
    }

    @Test
    void testValidate_Error_NegativeRetailPrice() {
        logger.info("🧪 TEST: validate - błąd: ujemna cena katalogowa");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(-10.0);
        
        ValidationResult result = productValidationService.validate(product);
        
        assertFalse(result.isValid(), "Produkt z ujemną ceną nie powinien przejść walidacji");
        assertTrue(result.getErrors().size() > 0, "Powinien być błąd");
    }

    @Test
    void testValidate_Error_NegativePurchasePrice() {
        logger.info("🧪 TEST: validate - błąd: ujemna cena zakupu");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setPurchasePrice(-10.0);
        
        ValidationResult result = productValidationService.validate(product);
        
        assertFalse(result.isValid(), "Produkt z ujemną ceną zakupu nie powinien przejść walidacji");
        assertTrue(result.getErrors().size() > 0, "Powinien być błąd");
    }

    @Test
    void testValidate_Error_DiscountOutOfRange() {
        logger.info("🧪 TEST: validate - błąd: rabat poza zakresem 0-100%");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setBasicDiscount(150); // Poza zakresem
        
        ValidationResult result = productValidationService.validate(product);
        
        assertFalse(result.isValid(), "Produkt z rabatem poza zakresem nie powinien przejść walidacji");
        assertTrue(result.getErrors().size() > 0, "Powinien być błąd");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - validate() - Ostrzeżenia
    // ==========================================

    @Test
    void testValidate_Warning_HighDiscounts() {
        logger.info("🧪 TEST: validate - ostrzeżenie: bardzo wysokie rabaty");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(100.0);
        product.setBasicDiscount(30);
        product.setAdditionalDiscount(15);
        product.setPromotionDiscount(10); // Suma: 55% (> 50%)
        
        ValidationResult result = productValidationService.validate(product);
        
        assertTrue(result.isValid(), "Produkt powinien przejść walidację (tylko ostrzeżenie)");
        assertTrue(result.getWarnings().size() > 0, "Powinno być ostrzeżenie");
        assertTrue(result.getWarnings().get(0).contains("bardzo wysokie rabaty"), 
                   "Ostrzeżenie powinno informować o bardzo wysokich rabatach");
    }

    @Test
    void testValidate_Warning_NoRetailPrice() {
        logger.info("🧪 TEST: validate - ostrzeżenie: brak ceny katalogowej");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(null);
        
        ValidationResult result = productValidationService.validate(product);
        
        assertTrue(result.isValid(), "Produkt powinien przejść walidację (tylko ostrzeżenie)");
        assertTrue(result.getWarnings().size() > 0, "Powinno być ostrzeżenie");
        assertTrue(result.getWarnings().get(0).contains("Brak ceny katalogowej"), 
                   "Ostrzeżenie powinno informować o braku ceny katalogowej");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - validate() - Edge Cases
    // ==========================================

    @Test
    void testValidate_EdgeCase_Accessory_NoSellingPriceCheck() {
        logger.info("🧪 TEST: validate - edge case: akcesoria (nie sprawdza ceny sprzedaży vs zakupu)");
        
        Product product = new Product();
        product.setName("Test Accessory");
        product.setCategory(ProductCategory.ACCESSORY);
        product.setPurchasePrice(80.0);
        product.setSellingPrice(70.0); // Dla akcesoriów to jest OK
        
        ValidationResult result = productValidationService.validate(product);
        
        // Dla akcesoriów nie sprawdzamy czy sellingPrice < purchasePrice
        assertTrue(result.getErrors().stream()
            .noneMatch(e -> e.contains("STRATA")), 
            "Akcesoria nie powinny mieć błędu STRATA");
    }

    @Test
    void testValidate_EdgeCase_NullDiscounts() {
        logger.info("🧪 TEST: validate - edge case: null rabaty");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(100.0);
        product.setBasicDiscount(null);
        product.setAdditionalDiscount(null);
        product.setPromotionDiscount(null);
        product.setSkontoDiscount(null);
        
        ValidationResult result = productValidationService.validate(product);
        
        assertTrue(result.isValid(), "Null rabaty powinny być traktowane jako 0");
    }

    @Test
    void testValidate_EdgeCase_ZeroPrices() {
        logger.info("🧪 TEST: validate - edge case: ceny = 0");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setCategory(ProductCategory.TILE);
        product.setRetailPrice(0.0);
        product.setPurchasePrice(0.0);
        product.setSellingPrice(0.0);
        
        ValidationResult result = productValidationService.validate(product);
        
        // Ceny = 0 są dozwolone (może być produkt bez ceny)
        assertTrue(result.isValid() || result.getErrors().isEmpty(), 
                  "Ceny = 0 nie powinny powodować błędów walidacji");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - validateBatch()
    // ==========================================

    @Test
    void testValidateBatch_MixedValidAndInvalid() {
        logger.info("🧪 TEST: validateBatch - mieszanka poprawnych i niepoprawnych produktów");
        
        List<Product> products = new ArrayList<>();
        
        // Poprawny produkt
        products.add(validProduct);
        
        // Niepoprawny produkt (strata)
        Product invalidProduct = new Product();
        invalidProduct.setName("Invalid Product");
        invalidProduct.setCategory(ProductCategory.TILE);
        invalidProduct.setRetailPrice(100.0);
        invalidProduct.setPurchasePrice(80.0);
        invalidProduct.setSellingPrice(70.0); // STRATA!
        products.add(invalidProduct);
        
        // Produkt z ostrzeżeniem
        Product warningProduct = new Product();
        warningProduct.setName("Warning Product");
        warningProduct.setCategory(ProductCategory.TILE);
        warningProduct.setRetailPrice(100.0);
        warningProduct.setBasicDiscount(60); // Bardzo wysoki rabat
        products.add(warningProduct);
        
        BatchValidationResult result = productValidationService.validateBatch(products);
        
        assertFalse(result.isAllValid(), "Nie wszystkie produkty są poprawne");
        // Poprawne produkty: validProduct (bez błędów i ostrzeżeń) + warningProduct (tylko ostrzeżenie, brak błędów) = 2
        assertEquals(2, result.getValidCount(), "Powinny być 2 poprawne produkty (validProduct + warningProduct)");
        assertEquals(1, result.getErrorCount(), "Powinien być 1 produkt z błędem");
        assertEquals(1, result.getWarningCount(), "Powinien być 1 produkt z ostrzeżeniem");
    }

    @Test
    void testValidateBatch_AllValid() {
        logger.info("🧪 TEST: validateBatch - wszystkie produkty poprawne");
        
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setSellingPrice(100.0 + i);
            product.setBasicDiscount(10);
            products.add(product);
        }
        
        BatchValidationResult result = productValidationService.validateBatch(products);
        
        assertTrue(result.isAllValid(), "Wszystkie produkty powinny być poprawne");
        assertEquals(10, result.getValidCount());
        assertEquals(0, result.getErrorCount());
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE - validateBatch()
    // ==========================================

    @Test
    void testValidateBatch_Performance_1000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: validateBatch - 1000 produktów");
        
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setSellingPrice(100.0 + i);
            product.setBasicDiscount(10);
            products.add(product);
        }
        
        long startTime = System.currentTimeMillis();
        BatchValidationResult result = productValidationService.validateBatch(products);
        long endTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] validateBatch - 1000 produktów: {}ms", endTime - startTime);
        
        assertTrue(result.isAllValid(), "Wszystkie produkty powinny być poprawne");
        assertEquals(1000, result.getValidCount());
        assertTrue(endTime - startTime < 2000, "1000 produktów powinno być zwalidowanych w mniej niż 2s");
    }

    @Test
    void testValidateBatch_Performance_5000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: validateBatch - 5000 produktów");
        
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setSellingPrice(100.0 + i);
            product.setBasicDiscount(10);
            products.add(product);
        }
        
        long startTime = System.currentTimeMillis();
        BatchValidationResult result = productValidationService.validateBatch(products);
        long endTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] validateBatch - 5000 produktów: {}ms ({}s)", 
                   endTime - startTime, (endTime - startTime) / 1000.0);
        
        assertTrue(result.isAllValid(), "Wszystkie produkty powinny być poprawne");
        assertEquals(5000, result.getValidCount());
        assertTrue(endTime - startTime < 10000, "5000 produktów powinno być zwalidowanych w mniej niż 10s");
    }

    @Test
    void testValidateBatch_EdgeCase_EmptyList() {
        logger.info("🧪 TEST: validateBatch - pusta lista");
        
        List<Product> emptyList = new ArrayList<>();
        
        BatchValidationResult result = productValidationService.validateBatch(emptyList);
        
        assertTrue(result.isAllValid(), "Pusta lista powinna być traktowana jako poprawna");
        assertEquals(0, result.getValidCount());
        assertEquals(0, result.getErrorCount());
    }
}

