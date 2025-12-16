package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.InputRepository;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ TESTY WYDAJNOŚCIOWE DLA OPERACJI BATCHOWYCH NA PRODUKTACH
 * 
 * Testuje operacje batchowe na produktach:
 * - updateProductsBatch() - aktualizacja dużej liczby produktów
 * - updateGroupDiscounts() - aktualizacja rabatów dla całej grupy
 * - fillProductQuantities() - przeliczanie ilości dla dużej liczby produktów
 * 
 * Testuje:
 * - Wydajność dla dużej liczby produktów (1000+)
 * - Poprawność aktualizacji
 * - Edge cases (puste listy, null wartości)
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class ProductServiceBatchOperationsTest extends BaseProjectServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceBatchOperationsTest.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private InputRepository inputRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE - updateProductsBatch()
    // ==========================================

    @Test
    void testUpdateProductsBatch_Performance_1000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: updateProductsBatch - 1000 produktów");
        
        // 1. Utwórz 1000 produktów testowych
        long createStartTime = System.currentTimeMillis();
        List<Product> products = createProductsBatch(1000);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 1000 produktów: {}ms", createEndTime - createStartTime);
        
        // 2. Zmodyfikuj produkty (zmiana cen, rabatów)
        long modifyStartTime = System.currentTimeMillis();
        for (Product product : products) {
            product.setRetailPrice(product.getRetailPrice() + 10.0);
            product.setBasicDiscount(15);
            product.setPromotionDiscount(5);
        }
        long modifyEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Modyfikacja 1000 produktów: {}ms", modifyEndTime - modifyStartTime);
        
        // 3. Aktualizuj produkty w bazie (batch update)
        long updateStartTime = System.currentTimeMillis();
        List<Product> updatedProducts = productService.updateProductsBatch(products);
        long updateEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] updateProductsBatch - 1000 produktów: {}ms", 
                   updateEndTime - updateStartTime);
        
        // 4. Weryfikacja
        assertEquals(1000, updatedProducts.size(), "Powinno zaktualizować 1000 produktów");
        
        // Sprawdź kilka produktów
        for (int i = 0; i < Math.min(10, updatedProducts.size()); i++) {
            Product updated = updatedProducts.get(i);
            assertNotNull(updated.getId(), "Produkt powinien mieć ID");
            assertEquals(15, updated.getBasicDiscount(), "basicDiscount powinien być 15");
            assertEquals(5, updated.getPromotionDiscount(), "promotionDiscount powinien być 5");
        }
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Zaktualizowano {} produktów w {}ms", 
                   updatedProducts.size(), updateEndTime - updateStartTime);
    }

    @Test
    void testUpdateProductsBatch_Performance_5000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: updateProductsBatch - 5000 produktów");
        
        long createStartTime = System.currentTimeMillis();
        List<Product> products = createProductsBatch(5000);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 5000 produktów: {}ms", createEndTime - createStartTime);
        
        for (Product product : products) {
            product.setRetailPrice(product.getRetailPrice() + 20.0);
            product.setAdditionalDiscount(10);
        }
        
        long updateStartTime = System.currentTimeMillis();
        List<Product> updatedProducts = productService.updateProductsBatch(products);
        long updateEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] updateProductsBatch - 5000 produktów: {}ms ({}s)", 
                   updateEndTime - updateStartTime, (updateEndTime - updateStartTime) / 1000.0);
        
        assertEquals(5000, updatedProducts.size(), "Powinno zaktualizować 5000 produktów");
        assertTrue(updateEndTime - updateStartTime < 30000, 
                  "5000 produktów powinno być zaktualizowanych w mniej niż 30s");
    }

    @Test
    void testUpdateProductsBatch_EdgeCase_EmptyList() {
        logger.info("🧪 TEST: updateProductsBatch - pusta lista");
        
        List<Product> emptyList = new ArrayList<>();
        
        assertDoesNotThrow(() -> {
            List<Product> result = productService.updateProductsBatch(emptyList);
            assertTrue(result.isEmpty(), "Pusta lista powinna zwrócić pustą listę");
        }, "Nie powinno rzucać wyjątku dla pustej listy");
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE - updateGroupDiscounts()
    // ==========================================

    @Test
    void testUpdateGroupDiscounts_Performance_LargeGroup() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: updateGroupDiscounts - duża grupa produktów");
        
        // 1. Utwórz produkty dla jednej grupy
        long createStartTime = System.currentTimeMillis();
        String manufacturer = "TEST_MANUFACTURER";
        String groupName = "TEST_GROUP";
        
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setManufacturer(manufacturer);
            product.setGroupName(groupName);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setBasicDiscount(10);
            product.setPromotionDiscount(5);
            products.add(product);
        }
        productRepository.saveAll(products);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 1000 produktów w grupie: {}ms", 
                   createEndTime - createStartTime);
        
        // 2. Aktualizuj rabaty dla całej grupy
        long updateStartTime = System.currentTimeMillis();
        List<Product> updatedProducts = productService.updateGroupDiscounts(
            ProductCategory.TILE,
            manufacturer,
            groupName,
            25,  // basicDiscount
            10,  // additionalDiscount
            15,  // promotionDiscount
            3,   // skontoDiscount
            null, // productType (wszystkie)
            DiscountCalculationMethod.KASKADOWO_B
        );
        long updateEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] updateGroupDiscounts - 1000 produktów: {}ms ({}s)", 
                   updateEndTime - updateStartTime, (updateEndTime - updateStartTime) / 1000.0);
        
        // 3. Weryfikacja
        assertEquals(1000, updatedProducts.size(), "Powinno zaktualizować 1000 produktów");
        
        // Sprawdź kilka produktów
        for (int i = 0; i < Math.min(10, updatedProducts.size()); i++) {
            Product updated = updatedProducts.get(i);
            assertEquals(25, updated.getBasicDiscount(), "basicDiscount powinien być 25");
            assertEquals(10, updated.getAdditionalDiscount(), "additionalDiscount powinien być 10");
            assertEquals(15, updated.getPromotionDiscount(), "promotionDiscount powinien być 15");
            assertEquals(3, updated.getSkontoDiscount(), "skontoDiscount powinien być 3");
            assertEquals(DiscountCalculationMethod.KASKADOWO_B, updated.getDiscountCalculationMethod(),
                         "discountCalculationMethod powinien być KASKADOWO_B");
        }
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Zaktualizowano {} produktów w grupie", 
                   updatedProducts.size());
    }

    @Test
    void testUpdateGroupDiscounts_EdgeCase_NonExistentGroup() {
        logger.info("🧪 TEST: updateGroupDiscounts - nieistniejąca grupa");
        
        List<Product> result = productService.updateGroupDiscounts(
            ProductCategory.TILE,
            "NON_EXISTENT_MANUFACTURER",
            "NON_EXISTENT_GROUP",
            25, 10, 15, 3,
            null,
            DiscountCalculationMethod.SUMARYCZNY
        );
        
        assertTrue(result.isEmpty(), "Nieistniejąca grupa powinna zwrócić pustą listę");
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE - fillProductQuantities()
    // ==========================================

    @Test
    void testFillProductQuantities_Performance_1000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: fillProductQuantities - 1000 produktów");
        
        // 1. Utwórz 1000 produktów z różnymi mapperName
        long createStartTime = System.currentTimeMillis();
        List<Product> products = createProductsBatch(1000);
        
        // Ustaw mapperName dla produktów
        String[] mapperNames = {"Powierzchnia polaci", "dlugosc krawedzi lewych", 
                               "dlugosc krawedzi prawych", "gasiar podstawowy", 
                               "gasior koncowy", "dlugosc okapu", "dlugosc kalenic"};
        for (int i = 0; i < products.size(); i++) {
            products.get(i).setMapperName(mapperNames[i % mapperNames.length]);
        }
        productRepository.saveAll(products);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 1000 produktów z mapperName: {}ms", 
                   createEndTime - createStartTime);
        
        // 2. Utwórz inputy odpowiadające mapperName
        List<Input> inputs = new ArrayList<>();
        for (String mapperName : mapperNames) {
            Input input = new Input();
            input.setMapperName(mapperName);
            input.setQuantity(100.0);
            input.setProject(testProject);
            inputs.add(input);
        }
        inputRepository.saveAll(inputs);
        
        // 3. Przelicz ilości
        long fillStartTime = System.currentTimeMillis();
        List<Product> filledProducts = productService.fillProductQuantities(
            inputs, ProductCategory.TILE);
        long fillEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] fillProductQuantities - 1000 produktów: {}ms ({}s)", 
                   fillEndTime - fillStartTime, (fillEndTime - fillStartTime) / 1000.0);
        
        // 4. Weryfikacja
        assertTrue(filledProducts.size() > 0, "Powinno zwrócić produkty");
        
        // Sprawdź czy ilości są przeliczone
        int productsWithQuantity = 0;
        for (Product product : filledProducts) {
            if (product.getQuantity() != null && product.getQuantity() > 0) {
                productsWithQuantity++;
            }
        }
        
        logger.info("📊 Produkty z przeliczoną ilością: {}/{}", productsWithQuantity, filledProducts.size());
        assertTrue(productsWithQuantity > 0, "Przynajmniej część produktów powinna mieć przeliczoną ilość");
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Przeliczono ilości dla {} produktów", 
                   filledProducts.size());
    }

    @Test
    void testFillProductQuantities_Performance_5000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: fillProductQuantities - 5000 produktów");
        
        long createStartTime = System.currentTimeMillis();
        List<Product> products = createProductsBatch(5000);
        
        String[] mapperNames = {"Powierzchnia polaci", "dlugosc krawedzi lewych", 
                               "dlugosc krawedzi prawych", "gasiar podstawowy"};
        for (int i = 0; i < products.size(); i++) {
            products.get(i).setMapperName(mapperNames[i % mapperNames.length]);
        }
        productRepository.saveAll(products);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 5000 produktów: {}ms", createEndTime - createStartTime);
        
        List<Input> inputs = new ArrayList<>();
        for (String mapperName : mapperNames) {
            Input input = new Input();
            input.setMapperName(mapperName);
            input.setQuantity(200.0);
            input.setProject(testProject);
            inputs.add(input);
        }
        inputRepository.saveAll(inputs);
        
        long fillStartTime = System.currentTimeMillis();
        List<Product> filledProducts = productService.fillProductQuantities(
            inputs, ProductCategory.TILE);
        long fillEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] fillProductQuantities - 5000 produktów: {}ms ({}s)", 
                   fillEndTime - fillStartTime, (fillEndTime - fillStartTime) / 1000.0);
        
        assertTrue(filledProducts.size() > 0, "Powinno zwrócić produkty");
        assertTrue(fillEndTime - fillStartTime < 10000, 
                  "5000 produktów powinno być przeliczonych w mniej niż 10s");
    }

    @Test
    void testFillProductQuantities_EdgeCase_EmptyInputs() {
        logger.info("🧪 TEST: fillProductQuantities - pusta lista inputów");
        
        List<Product> products = createProductsBatch(100);
        productRepository.saveAll(products);
        
        List<Input> emptyInputs = new ArrayList<>();
        
        List<Product> result = productService.fillProductQuantities(emptyInputs, ProductCategory.TILE);
        
        assertNotNull(result, "Wynik nie powinien być null");
        assertTrue(result.size() > 0, "Powinno zwrócić produkty (bez przeliczonych ilości)");
    }

    @Test
    void testFillProductQuantities_EdgeCase_NoMatchingMapperName() {
        logger.info("🧪 TEST: fillProductQuantities - brak dopasowania mapperName");
        
        List<Product> products = createProductsBatch(100);
        productRepository.saveAll(products);
        
        List<Input> inputs = new ArrayList<>();
        Input input = new Input();
        input.setMapperName("NON_EXISTENT_MAPPER");
        input.setQuantity(100.0);
        input.setProject(testProject);
        inputs.add(input);
        inputRepository.saveAll(inputs);
        
        List<Product> result = productService.fillProductQuantities(inputs, ProductCategory.TILE);
        
        assertNotNull(result, "Wynik nie powinien być null");
        // Produkty powinny być zwrócone, ale bez przeliczonych ilości
        boolean allQuantitiesZero = result.stream()
            .allMatch(p -> p.getQuantity() == null || p.getQuantity() == 0.0);
        assertTrue(allQuantitiesZero, "Produkty bez dopasowania powinny mieć quantity = 0 lub null");
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE - deleteProductsByIds()
    // ==========================================

    @Test
    void testDeleteProductsByIds_Performance_1000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: deleteProductsByIds - 1000 produktów");
        
        // 1. Utwórz 1000 produktów testowych
        long createStartTime = System.currentTimeMillis();
        List<Product> products = createProductsBatch(1000);
        List<Long> productIds = products.stream().map(Product::getId).collect(java.util.stream.Collectors.toList());
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 1000 produktów: {}ms", createEndTime - createStartTime);
        
        // 2. Usuń produkty używając batch delete
        long deleteStartTime = System.currentTimeMillis();
        java.util.Map<String, Object> result = productService.deleteProductsByIds(productIds);
        long deleteEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] deleteProductsByIds - 1000 produktów: {}ms", 
                   deleteEndTime - deleteStartTime);
        
        // 3. Weryfikacja
        assertEquals(1000, result.get("deletedCount"), "Powinno usunąć 1000 produktów");
        assertEquals(0, result.get("notFoundCount"), "Nie powinno być nieistniejących produktów");
        
        // Sprawdź czy produkty zostały rzeczywiście usunięte
        for (Long id : productIds) {
            assertFalse(productRepository.existsById(id), "Produkt ID " + id + " powinien być usunięty");
        }
        
        assertTrue((Long) deleteEndTime - deleteStartTime < 10000, 
                  "Usuwanie 1000 produktów powinno zakończyć się w ciągu 10 sekund");
    }

    @Test
    void testDeleteProductsByIds_Performance_5000Products() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: deleteProductsByIds - 5000 produktów (JDBC batch DELETE)");
        
        // 1. Utwórz 5000 produktów testowych
        long createStartTime = System.currentTimeMillis();
        List<Product> products = createProductsBatch(5000);
        List<Long> productIds = products.stream().map(Product::getId).collect(java.util.stream.Collectors.toList());
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 5000 produktów: {}ms ({}s)", 
                   createEndTime - createStartTime, (createEndTime - createStartTime) / 1000.0);
        
        // 2. Usuń produkty używając batch delete (JDBC batch DELETE dla > 50 produktów)
        long deleteStartTime = System.currentTimeMillis();
        java.util.Map<String, Object> result = productService.deleteProductsByIds(productIds);
        long deleteEndTime = System.currentTimeMillis();
        long deleteDuration = deleteEndTime - deleteStartTime;
        logger.info("⏱️ [PERFORMANCE] deleteProductsByIds - 5000 produktów: {}ms ({}s)", 
                   deleteDuration, deleteDuration / 1000.0);
        
        // 3. Weryfikacja
        assertEquals(5000, result.get("deletedCount"), "Powinno usunąć 5000 produktów");
        assertEquals(0, result.get("notFoundCount"), "Nie powinno być nieistniejących produktów");
        
        // Sprawdź czy produkty zostały rzeczywiście usunięte (sprawdź próbkę)
        int sampleSize = Math.min(100, productIds.size());
        for (int i = 0; i < sampleSize; i++) {
            Long id = productIds.get(i);
            assertFalse(productRepository.existsById(id), "Produkt ID " + id + " powinien być usunięty");
        }
        
        assertTrue(deleteDuration < 30000, 
                  "Usuwanie 5000 produktów powinno zakończyć się w ciągu 30 sekund");
    }

    @Test
    void testDeleteProductsByIds_Correctness_MixedExistingAndNonExisting() {
        logger.info("🧪 TEST POPRAWNOŚCIOWY: deleteProductsByIds - mieszanka istniejących i nieistniejących produktów");
        
        // 1. Utwórz 10 produktów
        List<Product> products = createProductsBatch(10);
        List<Long> existingIds = products.stream().map(Product::getId).collect(java.util.stream.Collectors.toList());
        
        // 2. Dodaj nieistniejące ID
        List<Long> allIds = new ArrayList<>(existingIds);
        allIds.add(999999L); // Nieistniejące ID
        allIds.add(999998L); // Nieistniejące ID
        
        // 3. Usuń produkty
        long startTime = System.currentTimeMillis();
        java.util.Map<String, Object> result = productService.deleteProductsByIds(allIds);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteProductsByIds (mixed): {}ms", duration);
        
        // 4. Weryfikacja
        assertEquals(10, result.get("deletedCount"), "Powinno usunąć 10 istniejących produktów");
        assertEquals(2, result.get("notFoundCount"), "Powinno znaleźć 2 nieistniejące produkty");
        
        @SuppressWarnings("unchecked")
        List<Long> deletedIds = (List<Long>) result.get("deletedIds");
        @SuppressWarnings("unchecked")
        List<Long> notFoundIds = (List<Long>) result.get("notFoundIds");
        
        assertEquals(10, deletedIds.size(), "Powinno być 10 usuniętych ID");
        assertEquals(2, notFoundIds.size(), "Powinno być 2 nieistniejących ID");
        
        // Sprawdź czy istniejące produkty zostały usunięte
        for (Long id : existingIds) {
            assertFalse(productRepository.existsById(id), "Produkt ID " + id + " powinien być usunięty");
        }
        
        // Sprawdź czy nieistniejące ID są w notFoundIds
        assertTrue(notFoundIds.contains(999999L), "999999 powinien być w notFoundIds");
        assertTrue(notFoundIds.contains(999998L), "999998 powinien być w notFoundIds");
    }

    @Test
    void testDeleteProductsByIds_EdgeCase_EmptyList() {
        logger.info("🧪 TEST BRZEGOWY: deleteProductsByIds - pusta lista");
        
        List<Long> emptyList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        java.util.Map<String, Object> result = productService.deleteProductsByIds(emptyList);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteProductsByIds (empty): {}ms", duration);
        
        assertEquals(0, result.get("deletedCount"), "Powinno zwrócić 0 usuniętych produktów");
        assertEquals(0, result.get("notFoundCount"), "Powinno zwrócić 0 nieistniejących produktów");
    }

    @Test
    void testDeleteAllByCategory_Performance_LargeCategory() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: deleteAllByCategory - duża kategoria (2000 produktów)");
        
        // 1. Utwórz 2000 produktów kategorii TILE
        long createStartTime = System.currentTimeMillis();
        createProductsBatch(2000, ProductCategory.TILE);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 2000 produktów TILE: {}ms ({}s)", 
                   createEndTime - createStartTime, (createEndTime - createStartTime) / 1000.0);
        
        // 2. Sprawdź ile produktów jest w kategorii przed usunięciem
        long countBefore = productRepository.findByCategory(ProductCategory.TILE).size();
        logger.info("📊 Produktów w kategorii TILE przed usunięciem: {}", countBefore);
        
        // 3. Usuń wszystkie produkty kategorii TILE
        long deleteStartTime = System.currentTimeMillis();
        productService.deleteAllByCategory(ProductCategory.TILE);
        long deleteEndTime = System.currentTimeMillis();
        long deleteDuration = deleteEndTime - deleteStartTime;
        logger.info("⏱️ [PERFORMANCE] deleteAllByCategory - TILE: {}ms ({}s)", 
                   deleteDuration, deleteDuration / 1000.0);
        
        // 4. Sprawdź czy produkty zostały usunięte
        long countAfter = productRepository.findByCategory(ProductCategory.TILE).size();
        logger.info("📊 Produktów w kategorii TILE po usunięciu: {}", countAfter);
        
        assertEquals(0, countAfter, "Wszystkie produkty kategorii TILE powinny być usunięte");
        assertTrue(deleteDuration < 30000, 
                  "Usuwanie wszystkich produktów kategorii powinno zakończyć się w ciągu 30 sekund");
    }

    @Test
    void testDeleteAllByManufacturer_Performance_LargeManufacturer() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: deleteAllByManufacturer - duży producent (1500 produktów)");
        
        // 1. Utwórz 1500 produktów tego samego producenta
        String manufacturer = "Test Manufacturer Large";
        long createStartTime = System.currentTimeMillis();
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.GUTTER);
            product.setManufacturer(manufacturer);
            product.setGroupName("Group " + (i % 10));
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            products.add(product);
        }
        productRepository.saveAll(products);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 1500 produktów producenta: {}ms ({}s)", 
                   createEndTime - createStartTime, (createEndTime - createStartTime) / 1000.0);
        
        // 2. Sprawdź ile produktów jest przed usunięciem
        long countBefore = productRepository.findByCategory(ProductCategory.GUTTER).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .count();
        logger.info("📊 Produktów producenta przed usunięciem: {}", countBefore);
        
        // 3. Usuń wszystkie produkty producenta
        long deleteStartTime = System.currentTimeMillis();
        productService.deleteAllByManufacturer(ProductCategory.GUTTER, manufacturer);
        long deleteEndTime = System.currentTimeMillis();
        long deleteDuration = deleteEndTime - deleteStartTime;
        logger.info("⏱️ [PERFORMANCE] deleteAllByManufacturer: {}ms ({}s)", 
                   deleteDuration, deleteDuration / 1000.0);
        
        // 4. Sprawdź czy produkty zostały usunięte
        long countAfter = productRepository.findByCategory(ProductCategory.GUTTER).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .count();
        logger.info("📊 Produktów producenta po usunięciu: {}", countAfter);
        
        assertEquals(0, countAfter, "Wszystkie produkty producenta powinny być usunięte");
        assertTrue(deleteDuration < 20000, 
                  "Usuwanie wszystkich produktów producenta powinno zakończyć się w ciągu 20 sekund");
    }

    @Test
    void testDeleteAllByGroup_Performance_LargeGroup() {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: deleteAllByGroup - duża grupa (800 produktów)");
        
        // 1. Utwórz 800 produktów tej samej grupy
        String manufacturer = "Test Manufacturer Group";
        String groupName = "Large Test Group";
        long createStartTime = System.currentTimeMillis();
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 800; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.ACCESSORY);
            product.setManufacturer(manufacturer);
            product.setGroupName(groupName);
            product.setRetailPrice(50.0 + i);
            product.setPurchasePrice(40.0 + i);
            products.add(product);
        }
        productRepository.saveAll(products);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 800 produktów grupy: {}ms ({}s)", 
                   createEndTime - createStartTime, (createEndTime - createStartTime) / 1000.0);
        
        // 2. Sprawdź ile produktów jest przed usunięciem
        long countBefore = productRepository.findByCategory(ProductCategory.ACCESSORY).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()) && groupName.equals(p.getGroupName()))
                .count();
        logger.info("📊 Produktów grupy przed usunięciem: {}", countBefore);
        
        // 3. Usuń wszystkie produkty grupy
        long deleteStartTime = System.currentTimeMillis();
        productService.deleteAllByGroup(ProductCategory.ACCESSORY, manufacturer, groupName);
        long deleteEndTime = System.currentTimeMillis();
        long deleteDuration = deleteEndTime - deleteStartTime;
        logger.info("⏱️ [PERFORMANCE] deleteAllByGroup: {}ms ({}s)", 
                   deleteDuration, deleteDuration / 1000.0);
        
        // 4. Sprawdź czy produkty zostały usunięte
        long countAfter = productRepository.findByCategory(ProductCategory.ACCESSORY).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()) && groupName.equals(p.getGroupName()))
                .count();
        logger.info("📊 Produktów grupy po usunięciu: {}", countAfter);
        
        assertEquals(0, countAfter, "Wszystkie produkty grupy powinny być usunięte");
        assertTrue(deleteDuration < 15000, 
                  "Usuwanie wszystkich produktów grupy powinno zakończyć się w ciągu 15 sekund");
    }
}

