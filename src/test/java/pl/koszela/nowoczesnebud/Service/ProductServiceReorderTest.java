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
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla funkcji zmiany kolejności produktów w grupie
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
class ProductServiceReorderTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceReorderTest.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private String testManufacturer;
    private String testGroupName;
    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
        testManufacturer = "TEST_MANUFACTURER";
        testGroupName = "TEST_GROUP";
        testCategory = ProductCategory.TILE;
        
        // Wyczyść produkty testowe przed każdym testem
        productRepository.deleteAll();
    }

    /**
     * Utwórz grupę produktów testowych z określoną liczbą produktów
     */
    private List<Product> createTestProducts(int count) {
        List<Product> products = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product p = new Product();
            p.setName("Produkt " + (i + 1));
            p.setManufacturer(testManufacturer);
            p.setGroupName(testGroupName);
            p.setCategory(testCategory);
            p.setRetailPrice(100.0 + i);
            p.setQuantityConverter(1.0);
            p.setDisplayOrder(i);
            products.add(p);
        }
        return productRepository.saveAll(products);
    }

    /**
     * ✅ TEST 1: Przesuń produkt w górę - podstawowy przypadek
     */
    @Test
    void testMoveProductUp_Basic() {
        logger.info("🧪 TEST 1: Przesuń produkt w górę - podstawowy przypadek");
        
        List<Product> products = createTestProducts(5);
        Product productToMove = products.get(2); // Produkt na pozycji 2
        
        // Przesuń w górę
        boolean moved = productService.moveProductUp(productToMove.getId());
        
        assertTrue(moved, "Produkt powinien zostać przesunięty w górę");
        
        // Sprawdź kolejność
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        assertEquals(5, updatedProducts.size());
        
        // Produkt powinien być teraz na pozycji 1
        Product movedProduct = updatedProducts.stream()
            .filter(p -> p.getId().equals(productToMove.getId()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(1, movedProduct.getDisplayOrder(), "Produkt powinien być na pozycji 1");
        logger.info("✅ Produkt przesunięty z pozycji 2 na pozycję 1");
    }

    /**
     * ✅ TEST 2: Przesuń produkt w dół - podstawowy przypadek
     */
    @Test
    void testMoveProductDown_Basic() {
        logger.info("🧪 TEST 2: Przesuń produkt w dół - podstawowy przypadek");
        
        List<Product> products = createTestProducts(5);
        Product productToMove = products.get(2); // Produkt na pozycji 2
        
        // Przesuń w dół
        boolean moved = productService.moveProductDown(productToMove.getId());
        
        assertTrue(moved, "Produkt powinien zostać przesunięty w dół");
        
        // Sprawdź kolejność
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product movedProduct = updatedProducts.stream()
            .filter(p -> p.getId().equals(productToMove.getId()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(3, movedProduct.getDisplayOrder(), "Produkt powinien być na pozycji 3");
        logger.info("✅ Produkt przesunięty z pozycji 2 na pozycję 3");
    }

    /**
     * ✅ TEST 3: Przesuń produkt na pierwszą pozycję - brzegowy przypadek
     */
    @Test
    void testMoveProductUp_FirstPosition() {
        logger.info("🧪 TEST 3: Przesuń produkt na pierwszą pozycję - brzegowy przypadek");
        
        List<Product> products = createTestProducts(5);
        Product firstProduct = products.get(0); // Produkt na pozycji 0 (pierwszy)
        
        // Próba przesunięcia pierwszego produktu w górę powinna zwrócić false
        boolean moved = productService.moveProductUp(firstProduct.getId());
        
        assertFalse(moved, "Produkt na pierwszej pozycji nie powinien być przesunięty w górę");
        
        // Sprawdź że kolejność się nie zmieniła
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product stillFirst = updatedProducts.get(0);
        assertEquals(firstProduct.getId(), stillFirst.getId(), "Pierwszy produkt powinien pozostać na pierwszej pozycji");
        assertEquals(0, stillFirst.getDisplayOrder(), "DisplayOrder powinien pozostać 0");
        logger.info("✅ Produkt na pierwszej pozycji nie został przesunięty");
    }

    /**
     * ✅ TEST 4: Przesuń produkt na ostatnią pozycję - brzegowy przypadek
     */
    @Test
    void testMoveProductDown_LastPosition() {
        logger.info("🧪 TEST 4: Przesuń produkt na ostatnią pozycję - brzegowy przypadek");
        
        List<Product> products = createTestProducts(5);
        Product lastProduct = products.get(4); // Produkt na pozycji 4 (ostatni)
        
        // Próba przesunięcia ostatniego produktu w dół powinna zwrócić false
        boolean moved = productService.moveProductDown(lastProduct.getId());
        
        assertFalse(moved, "Produkt na ostatniej pozycji nie powinien być przesunięty w dół");
        
        // Sprawdź że kolejność się nie zmieniła
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product stillLast = updatedProducts.get(updatedProducts.size() - 1);
        assertEquals(lastProduct.getId(), stillLast.getId(), "Ostatni produkt powinien pozostać na ostatniej pozycji");
        assertEquals(4, stillLast.getDisplayOrder(), "DisplayOrder powinien pozostać 4");
        logger.info("✅ Produkt na ostatniej pozycji nie został przesunięty");
    }

    /**
     * ✅ TEST 5: Przesuń produkt na konkretną pozycję
     */
    @Test
    void testMoveProductToPosition() {
        logger.info("🧪 TEST 5: Przesuń produkt na konkretną pozycję");
        
        List<Product> products = createTestProducts(5);
        Product productToMove = products.get(4); // Produkt na pozycji 4
        
        // Przesuń na pozycję 1
        boolean moved = productService.moveProductToPosition(productToMove.getId(), 1);
        
        assertTrue(moved, "Produkt powinien zostać przesunięty na pozycję 1");
        
        // Sprawdź kolejność
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product movedProduct = updatedProducts.stream()
            .filter(p -> p.getId().equals(productToMove.getId()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(1, movedProduct.getDisplayOrder(), "Produkt powinien być na pozycji 1");
        
        // Sprawdź że wszystkie produkty mają poprawne displayOrder (0, 1, 2, 3, 4)
        for (int i = 0; i < updatedProducts.size(); i++) {
            assertEquals(i, updatedProducts.get(i).getDisplayOrder(), 
                        "Produkt na pozycji " + i + " powinien mieć displayOrder = " + i);
        }
        logger.info("✅ Produkt przesunięty na pozycję 1, wszystkie produkty mają poprawne displayOrder");
    }

    /**
     * ✅ TEST 6: Przesuń produkt na pozycję 0 (pierwsza pozycja)
     */
    @Test
    void testMoveProductToPosition_First() {
        logger.info("🧪 TEST 6: Przesuń produkt na pozycję 0 (pierwsza pozycja)");
        
        List<Product> products = createTestProducts(5);
        Product productToMove = products.get(3); // Produkt na pozycji 3
        
        // Przesuń na pozycję 0
        boolean moved = productService.moveProductToPosition(productToMove.getId(), 0);
        
        assertTrue(moved, "Produkt powinien zostać przesunięty na pozycję 0");
        
        // Sprawdź kolejność
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product firstProduct = updatedProducts.get(0);
        assertEquals(productToMove.getId(), firstProduct.getId(), "Produkt powinien być na pierwszej pozycji");
        assertEquals(0, firstProduct.getDisplayOrder(), "DisplayOrder powinien być 0");
        logger.info("✅ Produkt przesunięty na pierwszą pozycję");
    }

    /**
     * ✅ TEST 7: Przesuń produkt na ostatnią pozycję
     */
    @Test
    void testMoveProductToPosition_Last() {
        logger.info("🧪 TEST 7: Przesuń produkt na ostatnią pozycję");
        
        List<Product> products = createTestProducts(5);
        Product productToMove = products.get(0); // Produkt na pozycji 0
        
        // Przesuń na ostatnią pozycję (4)
        boolean moved = productService.moveProductToPosition(productToMove.getId(), 4);
        
        assertTrue(moved, "Produkt powinien zostać przesunięty na ostatnią pozycję");
        
        // Sprawdź kolejność
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product lastProduct = updatedProducts.get(updatedProducts.size() - 1);
        assertEquals(productToMove.getId(), lastProduct.getId(), "Produkt powinien być na ostatniej pozycji");
        assertEquals(4, lastProduct.getDisplayOrder(), "DisplayOrder powinien być 4");
        logger.info("✅ Produkt przesunięty na ostatnią pozycję");
    }

    /**
     * ✅ TEST 8: Zamień miejscami dwa produkty
     */
    @Test
    void testSwapProducts() {
        logger.info("🧪 TEST 8: Zamień miejscami dwa produkty");
        
        List<Product> products = createTestProducts(5);
        Product product1 = products.get(1); // Pozycja 1
        Product product2 = products.get(3); // Pozycja 3
        
        Integer order1Before = product1.getDisplayOrder();
        Integer order2Before = product2.getDisplayOrder();
        
        // Zamień miejscami
        boolean swapped = productService.swapProducts(product1.getId(), product2.getId());
        
        assertTrue(swapped, "Produkty powinny zostać zamienione miejscami");
        
        // Sprawdź kolejność
        product1 = productRepository.findById(product1.getId()).orElseThrow();
        product2 = productRepository.findById(product2.getId()).orElseThrow();
        
        assertEquals(order2Before, product1.getDisplayOrder(), "Produkt 1 powinien mieć displayOrder produktu 2");
        assertEquals(order1Before, product2.getDisplayOrder(), "Produkt 2 powinien mieć displayOrder produktu 1");
        logger.info("✅ Produkty zamienione miejscami: {} ↔ {}", order1Before, order2Before);
    }

    /**
     * ✅ TEST 9: Grupa z jednym produktem - brzegowy przypadek
     */
    @Test
    void testMoveProductUp_SingleProduct() {
        logger.info("🧪 TEST 9: Grupa z jednym produktem - brzegowy przypadek");
        
        List<Product> products = createTestProducts(1);
        Product product = products.get(0);
        
        // Próba przesunięcia jedynego produktu w górę powinna zwrócić false
        boolean movedUp = productService.moveProductUp(product.getId());
        assertFalse(movedUp, "Nie można przesunąć jedynego produktu w górę");
        
        // Próba przesunięcia jedynego produktu w dół powinna zwrócić false
        boolean movedDown = productService.moveProductDown(product.getId());
        assertFalse(movedDown, "Nie można przesunąć jedynego produktu w dół");
        
        logger.info("✅ Grupa z jednym produktem - operacje zwróciły false");
    }

    /**
     * ✅ TEST 10: Nieistniejący produkt - brzegowy przypadek
     */
    @Test
    void testMoveProductUp_NonExistentProduct() {
        logger.info("🧪 TEST 10: Nieistniejący produkt - brzegowy przypadek");
        
        // Próba przesunięcia nieistniejącego produktu powinna rzucić wyjątek
        assertThrows(IllegalArgumentException.class, () -> {
            productService.moveProductUp(99999L);
        }, "Powinien zostać rzucony wyjątek dla nieistniejącego produktu");
        
        logger.info("✅ Nieistniejący produkt - wyjątek został rzucony");
    }

    /**
     * ✅ TEST 11: Przesuń produkt na pozycję poza zakresem - brzegowy przypadek
     */
    @Test
    void testMoveProductToPosition_OutOfRange() {
        logger.info("🧪 TEST 11: Przesuń produkt na pozycję poza zakresem - brzegowy przypadek");
        
        List<Product> products = createTestProducts(5);
        Product product = products.get(0);
        
        // Próba przesunięcia na pozycję poza zakresem powinna rzucić wyjątek
        assertThrows(IllegalArgumentException.class, () -> {
            productService.moveProductToPosition(product.getId(), 10);
        }, "Powinien zostać rzucony wyjątek dla pozycji poza zakresem");
        
        logger.info("✅ Pozycja poza zakresem - wyjątek został rzucony");
    }

    /**
     * ✅ TEST 12: Przesuń produkt na ujemną pozycję - brzegowy przypadek
     */
    @Test
    void testMoveProductToPosition_NegativePosition() {
        logger.info("🧪 TEST 12: Przesuń produkt na ujemną pozycję - brzegowy przypadek");
        
        List<Product> products = createTestProducts(5);
        Product product = products.get(0);
        
        // Próba przesunięcia na ujemną pozycję powinna rzucić wyjątek
        assertThrows(IllegalArgumentException.class, () -> {
            productService.moveProductToPosition(product.getId(), -1);
        }, "Powinien zostać rzucony wyjątek dla ujemnej pozycji");
        
        logger.info("✅ Ujemna pozycja - wyjątek został rzucony");
    }

    /**
     * ✅ TEST 13: Zamień miejscami produkty z różnych grup - brzegowy przypadek
     */
    @Test
    void testSwapProducts_DifferentGroups() {
        logger.info("🧪 TEST 13: Zamień miejscami produkty z różnych grup - brzegowy przypadek");
        
        // Utwórz dwie grupy produktów
        List<Product> group1 = createTestProducts(2);
        group1.forEach(p -> {
            p.setGroupName("GROUP_1");
            productRepository.save(p);
        });
        
        List<Product> group2 = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Product p = new Product();
            p.setName("Produkt Group2 " + (i + 1));
            p.setManufacturer(testManufacturer);
            p.setGroupName("GROUP_2");
            p.setCategory(testCategory);
            p.setRetailPrice(200.0 + i);
            p.setQuantityConverter(1.0);
            p.setDisplayOrder(i);
            group2.add(productRepository.save(p));
        }
        
        Product product1 = group1.get(0);
        Product product2 = group2.get(0);
        
        // Próba zamiany produktów z różnych grup powinna rzucić wyjątek
        assertThrows(IllegalArgumentException.class, () -> {
            productService.swapProducts(product1.getId(), product2.getId());
        }, "Powinien zostać rzucony wyjątek dla produktów z różnych grup");
        
        logger.info("✅ Produkty z różnych grup - wyjątek został rzucony");
    }

    /**
     * ✅ TEST 14: Wielokrotne przesunięcia w górę
     */
    @Test
    void testMoveProductUp_MultipleTimes() {
        logger.info("🧪 TEST 14: Wielokrotne przesunięcia w górę");
        
        List<Product> products = createTestProducts(5);
        Product product = products.get(4); // Produkt na pozycji 4 (ostatni)
        
        // Przesuń 3 razy w górę
        assertTrue(productService.moveProductUp(product.getId()), "Pierwsze przesunięcie");
        assertTrue(productService.moveProductUp(product.getId()), "Drugie przesunięcie");
        assertTrue(productService.moveProductUp(product.getId()), "Trzecie przesunięcie");
        
        // Sprawdź pozycję
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        Product movedProduct = updatedProducts.stream()
            .filter(p -> p.getId().equals(product.getId()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(1, movedProduct.getDisplayOrder(), "Produkt powinien być na pozycji 1");
        
        // Próba przesunięcia jeszcze raz powinna zwrócić false (już na pozycji 1, nie można wyżej)
        assertFalse(productService.moveProductUp(product.getId()), "Nie można przesunąć dalej w górę");
        
        logger.info("✅ Wielokrotne przesunięcia w górę - produkt na pozycji 1");
    }

    /**
     * ✅ TEST 15: Sprawdź czy wszystkie produkty mają poprawne displayOrder po operacjach
     */
    @Test
    void testDisplayOrder_Consistency() {
        logger.info("🧪 TEST 15: Sprawdź czy wszystkie produkty mają poprawne displayOrder po operacjach");
        
        List<Product> products = createTestProducts(5);
        
        // Wykonaj różne operacje
        productService.moveProductUp(products.get(2).getId());
        productService.moveProductDown(products.get(1).getId());
        productService.swapProducts(products.get(0).getId(), products.get(4).getId());
        
        // Sprawdź czy wszystkie produkty mają poprawne displayOrder (0, 1, 2, 3, 4)
        List<Product> updatedProducts = productService.getProductsByGroup(testCategory, testManufacturer, testGroupName);
        
        assertEquals(5, updatedProducts.size(), "Powinno być 5 produktów");
        
        // Sprawdź czy displayOrder są unikalne i ciągłe (0, 1, 2, 3, 4)
        List<Integer> orders = updatedProducts.stream()
            .map(p -> p.getDisplayOrder() != null ? p.getDisplayOrder() : 0)
            .sorted()
            .collect(Collectors.toList());
        
        for (int i = 0; i < orders.size(); i++) {
            assertEquals(i, orders.get(i), "DisplayOrder powinien być ciągły: " + i);
        }
        
        logger.info("✅ Wszystkie produkty mają poprawne i ciągłe displayOrder");
    }

    /**
     * ✅ TEST 16: Produkt bez category/manufacturer/groupName - brzegowy przypadek
     */
    @Test
    void testMoveProductUp_MissingFields() {
        logger.info("🧪 TEST 16: Produkt bez category/manufacturer/groupName - brzegowy przypadek");
        
        Product product = new Product();
        product.setName("Produkt bez grupy");
        product.setRetailPrice(100.0);
        Product savedProduct = productRepository.save(product);
        
        // Próba przesunięcia produktu bez grupy powinna rzucić wyjątek
        assertThrows(IllegalArgumentException.class, () -> {
            productService.moveProductUp(savedProduct);
        }, "Powinien zostać rzucony wyjątek dla produktu bez grupy");
        
        logger.info("✅ Produkt bez grupy - wyjątek został rzucony");
    }
}

