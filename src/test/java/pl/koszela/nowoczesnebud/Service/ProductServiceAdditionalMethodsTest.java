package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.DTO.CheckExistingGroupsRequest;
import pl.koszela.nowoczesnebud.DTO.GroupAttributesRequest;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.GroupOption;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;
import pl.koszela.nowoczesnebud.Repository.ProductGroupAttributesRepository;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 TESTY DLA DODATKOWYCH METOD ProductService
 * 
 * Testuje metody, które nie były jeszcze przetestowane:
 * - setGroupOption - ustawianie opcji (główna/opcjonalna) dla grup produktów
 * - renameManufacturer - zmiana nazwy producenta dla wszystkich produktów
 * - renameGroup - zmiana nazwy grupy dla wszystkich produktów w grupie
 * - deleteAllByManufacturer - usuwanie wszystkich produktów producenta
 * - deleteAllByGroup - usuwanie wszystkich produktów grupy
 * - getAttributeSuggestions - pobieranie słownika atrybutów dla autouzupełniania
 * - getGroupAttributes - pobieranie atrybutów dla konkretnej grupy
 * - saveGroupAttributes - zapisywanie atrybutów dla grupy
 * - checkExistingGroups - sprawdzanie istniejących kombinacji producent+grupa
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class ProductServiceAdditionalMethodsTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceAdditionalMethodsTest.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductGroupAttributesRepository productGroupAttributesRepository;

    @Autowired
    private javax.persistence.EntityManager entityManager;

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        // Utwórz testowe produkty
        testProducts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setManufacturer("Test Manufacturer");
            product.setGroupName("Test Group");
            product.setRetailPrice(100.0 + i * 10);
            product.setPurchasePrice(80.0 + i * 8);
            product.setMapperName("Mapper " + i);
            product.setIsMainOption(GroupOption.NONE);
            testProducts.add(product);
        }
        testProducts = productRepository.saveAll(testProducts);
    }

    // ========== TESTY POPRAWNOŚCIOWE ==========

    @Test
    void testSetGroupOption_MainOption_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: setGroupOption - ustawienie jako główna opcja");

        List<Product> updated = productService.setGroupOption(
            ProductCategory.TILE,
            "Test Manufacturer",
            "Test Group",
            GroupOption.MAIN
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] setGroupOption (main): {}ms | zaktualizowano: {} produktów", 
                   duration, updated.size());

        assertNotNull(updated);
        assertFalse(updated.isEmpty());
        updated.forEach(p -> {
            assertEquals(GroupOption.MAIN, p.getIsMainOption());
            assertEquals("Test Manufacturer", p.getManufacturer());
            assertEquals("Test Group", p.getGroupName());
        });
    }

    @Test
    void testSetGroupOption_OptionalOption_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: setGroupOption - ustawienie jako opcjonalna opcja");

        List<Product> updated = productService.setGroupOption(
            ProductCategory.TILE,
            "Test Manufacturer",
            "Test Group",
            GroupOption.OPTIONAL
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] setGroupOption (optional): {}ms | zaktualizowano: {} produktów", 
                   duration, updated.size());

        assertNotNull(updated);
        assertFalse(updated.isEmpty());
        updated.forEach(p -> {
            assertEquals(GroupOption.OPTIONAL, p.getIsMainOption());
        });
    }

    @Test
    void testRenameManufacturer_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: renameManufacturer - poprawność");

        String oldManufacturer = "Test Manufacturer";
        String newManufacturer = "Renamed Manufacturer";

        List<Product> updated = productService.renameManufacturer(
            ProductCategory.TILE,
            oldManufacturer,
            newManufacturer
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] renameManufacturer: {}ms | zaktualizowano: {} produktów", 
                   duration, updated.size());

        assertNotNull(updated);
        assertEquals(10, updated.size());
        updated.forEach(p -> {
            assertEquals(newManufacturer, p.getManufacturer());
            assertEquals("Test Group", p.getGroupName());
        });

        // Sprawdź czy stare produkty nie istnieją
        List<Product> oldProducts = productRepository.findByCategoryAndManufacturer(
            ProductCategory.TILE, oldManufacturer);
        assertTrue(oldProducts.isEmpty());
    }

    @Test
    void testRenameGroup_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: renameGroup - poprawność");

        String manufacturer = "Test Manufacturer";
        String oldGroupName = "Test Group";
        String newGroupName = "Renamed Group";

        List<Product> updated = productService.renameGroup(
            ProductCategory.TILE,
            manufacturer,
            oldGroupName,
            newGroupName
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] renameGroup: {}ms | zaktualizowano: {} produktów", 
                   duration, updated.size());

        assertNotNull(updated);
        assertEquals(10, updated.size());
        updated.forEach(p -> {
            assertEquals(manufacturer, p.getManufacturer());
            assertEquals(newGroupName, p.getGroupName());
        });

        // Sprawdź czy stare produkty nie istnieją
        List<Product> allProducts = productRepository.findByCategoryAndManufacturer(
            ProductCategory.TILE, manufacturer);
        List<Product> oldProducts = allProducts.stream()
            .filter(p -> oldGroupName.equals(p.getGroupName()))
            .toList();
        assertTrue(oldProducts.isEmpty());
    }

    @Test
    void testDeleteAllByManufacturer_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: deleteAllByManufacturer - poprawność");

        String manufacturer = "Test Manufacturer";

        productService.deleteAllByManufacturer(ProductCategory.TILE, manufacturer);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteAllByManufacturer: {}ms", duration);

        // Sprawdź czy produkty zostały usunięte
        List<Product> remaining = productRepository.findByCategoryAndManufacturer(
            ProductCategory.TILE, manufacturer);
        assertTrue(remaining.isEmpty());
    }

    @Test
    void testDeleteAllByGroup_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: deleteAllByGroup - poprawność");

        String manufacturer = "Test Manufacturer";
        String groupName = "Test Group";

        productService.deleteAllByGroup(ProductCategory.TILE, manufacturer, groupName);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteAllByGroup: {}ms", duration);

        // Sprawdź czy produkty zostały usunięte
        List<Product> allProducts = productRepository.findByCategoryAndManufacturer(
            ProductCategory.TILE, manufacturer);
        List<Product> remaining = allProducts.stream()
            .filter(p -> groupName.equals(p.getGroupName()))
            .toList();
        assertTrue(remaining.isEmpty());
    }

    @Test
    void testGetAttributeSuggestions_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getAttributeSuggestions - poprawność");

        // Utwórz atrybuty dla grup produktów (atrybuty są przechowywane w ProductGroupAttributes, nie w Product)
        pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes1 = 
            new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes();
        groupAttributes1.setCategory(ProductCategory.TILE);
        groupAttributes1.setManufacturer("Manufacturer 1");
        groupAttributes1.setGroupName("Group 1");
        groupAttributes1.setAttributes("{\"kolor\":[\"czerwony\",\"brązowy\"],\"kształt\":[\"płaska\"]}");
        productGroupAttributesRepository.save(groupAttributes1);

        pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes2 = 
            new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes();
        groupAttributes2.setCategory(ProductCategory.TILE);
        groupAttributes2.setManufacturer("Manufacturer 2");
        groupAttributes2.setGroupName("Group 2");
        groupAttributes2.setAttributes("{\"kolor\":[\"czarny\"],\"kształt\":[\"karpiówka\"]}");
        productGroupAttributesRepository.save(groupAttributes2);

        Map<String, List<String>> suggestions = productService.getAttributeSuggestions(ProductCategory.TILE);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getAttributeSuggestions: {}ms | znaleziono: {} atrybutów", 
                   duration, suggestions.size());

        assertNotNull(suggestions);
        assertTrue(suggestions.containsKey("kolor"));
        assertTrue(suggestions.containsKey("kształt"));
        
        List<String> colors = suggestions.get("kolor");
        assertTrue(colors.contains("czerwony"));
        assertTrue(colors.contains("brązowy"));
        assertTrue(colors.contains("czarny"));
    }

    @Test
    void testGetGroupAttributes_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getGroupAttributes - poprawność");

        // Utwórz atrybuty dla grupy
        pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes = 
            new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes();
        groupAttributes.setCategory(ProductCategory.TILE);
        groupAttributes.setManufacturer("Test Manufacturer");
        groupAttributes.setGroupName("Test Group");
        groupAttributes.setAttributes("{\"kolor\":[\"czerwony\"],\"kształt\":[\"płaska\"]}");
        productGroupAttributesRepository.save(groupAttributes);

        String attributes = productService.getGroupAttributes(
            ProductCategory.TILE,
            "Test Manufacturer",
            "Test Group"
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getGroupAttributes: {}ms | attributes: {}", duration, attributes);

        assertNotNull(attributes);
        assertTrue(attributes.contains("kolor"));
        assertTrue(attributes.contains("czerwony"));
    }

    @Test
    void testGetGroupAttributes_NotFound() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getGroupAttributes - nie znaleziono");

        String attributes = productService.getGroupAttributes(
            ProductCategory.TILE,
            "Non-existent Manufacturer",
            "Non-existent Group"
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getGroupAttributes (not found): {}ms", duration);

        assertNull(attributes);
    }

    @Test
    void testSaveGroupAttributes_CreateNew_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveGroupAttributes - tworzenie nowych atrybutów");

        GroupAttributesRequest request = new GroupAttributesRequest();
        request.setCategory(ProductCategory.TILE);
        request.setManufacturer("New Manufacturer");
        request.setGroupName("New Group");
        
        Map<String, List<String>> attributesMap = new HashMap<>();
        attributesMap.put("kolor", List.of("czerwony", "brązowy"));
        attributesMap.put("kształt", List.of("płaska"));
        request.setAttributes(attributesMap);

        productService.saveGroupAttributes(request);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveGroupAttributes (create): {}ms", duration);

        // Sprawdź czy atrybuty zostały zapisane
        String savedAttributes = productService.getGroupAttributes(
            ProductCategory.TILE,
            "New Manufacturer",
            "New Group"
        );
        assertNotNull(savedAttributes);
        assertTrue(savedAttributes.contains("kolor"));
        assertTrue(savedAttributes.contains("czerwony"));
    }

    @Test
    void testSaveGroupAttributes_UpdateExisting_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveGroupAttributes - aktualizacja istniejących atrybutów");

        // Utwórz atrybuty
        pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes = 
            new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes();
        groupAttributes.setCategory(ProductCategory.TILE);
        groupAttributes.setManufacturer("Test Manufacturer");
        groupAttributes.setGroupName("Test Group");
        groupAttributes.setAttributes("{\"kolor\":[\"czerwony\"]}");
        productGroupAttributesRepository.save(groupAttributes);

        // Zaktualizuj atrybuty
        GroupAttributesRequest request = new GroupAttributesRequest();
        request.setCategory(ProductCategory.TILE);
        request.setManufacturer("Test Manufacturer");
        request.setGroupName("Test Group");
        
        Map<String, List<String>> attributesMap = new HashMap<>();
        attributesMap.put("kolor", List.of("czarny", "biały"));
        request.setAttributes(attributesMap);

        productService.saveGroupAttributes(request);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveGroupAttributes (update): {}ms", duration);

        // Sprawdź czy atrybuty zostały zaktualizowane
        String savedAttributes = productService.getGroupAttributes(
            ProductCategory.TILE,
            "Test Manufacturer",
            "Test Group"
        );
        assertNotNull(savedAttributes);
        assertTrue(savedAttributes.contains("czarny"));
        assertTrue(savedAttributes.contains("biały"));
        assertFalse(savedAttributes.contains("czerwony"));
    }

    @Test
    void testCheckExistingGroups_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: checkExistingGroups - poprawność");

        // Utwórz produkty z różnymi kombinacjami producent+grupa
        // ⚠️ WAŻNE: Metoda checkExistingGroups sprawdza finalGroupName, który może być kombinacją groupName + " | " + productName
        // Jeśli groupName i productName są różne, używa kombinacji. Jeśli są takie same, używa tylko groupName.
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setCategory(ProductCategory.TILE);
        product1.setManufacturer("Manufacturer A");
        product1.setGroupName("Group A"); // groupName = "Group A"
        product1.setRetailPrice(100.0);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setCategory(ProductCategory.TILE);
        product2.setManufacturer("Manufacturer B");
        product2.setGroupName("Group B"); // groupName = "Group B"
        product2.setRetailPrice(100.0);
        productRepository.save(product2);

        // ⚠️ WAŻNE: Flush, żeby upewnić się, że produkty są zapisane w bazie przed sprawdzeniem
        entityManager.flush();

        // Sprawdź istniejące kombinacje
        // ⚠️ WAŻNE: Jeśli groupName i productName są takie same, metoda używa tylko groupName
        // Jeśli są różne, używa kombinacji "groupName | productName"
        List<CheckExistingGroupsRequest.ManufacturerGroupPair> pairs = new ArrayList<>();
        // Para 1: groupName = "Group A", productName = "Group A" (takie same) -> finalGroupName = "Group A"
        pairs.add(new CheckExistingGroupsRequest.ManufacturerGroupPair("Manufacturer A", "Group A", "Group A"));
        // Para 2: groupName = "Group B", productName = "Group B" (takie same) -> finalGroupName = "Group B"
        pairs.add(new CheckExistingGroupsRequest.ManufacturerGroupPair("Manufacturer B", "Group B", "Group B"));
        // Para 3: nie istnieje
        pairs.add(new CheckExistingGroupsRequest.ManufacturerGroupPair("Manufacturer C", "Group C", "Group C"));

        List<CheckExistingGroupsRequest.ManufacturerGroupPair> existing = 
            productService.checkExistingGroups(ProductCategory.TILE, pairs);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] checkExistingGroups: {}ms | znaleziono: {} istniejących kombinacji", 
                   duration, existing.size());

        assertNotNull(existing);
        assertEquals(2, existing.size());
        assertTrue(existing.stream().anyMatch(p -> 
            p.getManufacturer().equals("Manufacturer A") && p.getGroupName().equals("Group A")));
        assertTrue(existing.stream().anyMatch(p -> 
            p.getManufacturer().equals("Manufacturer B") && p.getGroupName().equals("Group B")));
    }

    @Test
    void testCheckExistingGroups_Performance_ManyCombinations() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST WYDAJNOŚCIOWY: checkExistingGroups - duża liczba kombinacji (579 kombinacji)");

        // Utwórz 579 różnych grup produktowych w bazie
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 579; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setManufacturer("Manufacturer " + (i % 50)); // 50 różnych producentów
            product.setGroupName("Group " + i);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setMapperName("Mapper " + i);
            products.add(product);
        }
        productRepository.saveAll(products);
        entityManager.flush();

        // Utwórz 579 kombinacji do sprawdzenia (wszystkie istnieją w bazie)
        List<CheckExistingGroupsRequest.ManufacturerGroupPair> pairs = new ArrayList<>();
        for (int i = 0; i < 579; i++) {
            pairs.add(new CheckExistingGroupsRequest.ManufacturerGroupPair(
                "Manufacturer " + (i % 50),
                "Group " + i,
                "Group " + i
            ));
        }

        // Sprawdź istniejące kombinacje
        long checkStartTime = System.currentTimeMillis();
        List<CheckExistingGroupsRequest.ManufacturerGroupPair> existing = 
            productService.checkExistingGroups(ProductCategory.TILE, pairs);
        long checkEndTime = System.currentTimeMillis();
        long checkDuration = checkEndTime - checkStartTime;

        long totalDuration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] checkExistingGroups - 579 kombinacji: {}ms ({}s)", 
                   checkDuration, checkDuration / 1000.0);
        logger.info("⏱️ [PERFORMANCE] Całkowity czas testu: {}ms ({}s)", 
                   totalDuration, totalDuration / 1000.0);

        assertNotNull(existing);
        assertEquals(579, existing.size(), "Wszystkie 579 kombinacji powinny być znalezione");
        assertTrue(checkDuration < 3000, 
                  "Sprawdzanie 579 kombinacji powinno zakończyć się w ciągu 3 sekund (zoptymalizowane: 1 zapytanie SQL zamiast 579)");
    }

    @Test
    void testDeleteMultipleGroupsWithProgress_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: deleteMultipleGroupsWithProgress - poprawność");

        // Utwórz produkty w różnych grupach
        String manufacturer1 = "Test Manufacturer 1";
        String manufacturer2 = "Test Manufacturer 2";
        String group1 = "Test Group 1";
        String group2 = "Test Group 2";
        String group3 = "Test Group 3";

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setManufacturer(manufacturer1);
            product.setGroupName(i < 5 ? group1 : group2);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setMapperName("Mapper " + i);
            products.add(product);
        }
        for (int i = 0; i < 5; i++) {
            Product product = new Product();
            product.setName("Product " + (10 + i));
            product.setCategory(ProductCategory.TILE);
            product.setManufacturer(manufacturer2);
            product.setGroupName(group3);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setMapperName("Mapper " + (10 + i));
            products.add(product);
        }
        productRepository.saveAll(products);
        entityManager.flush();

        // Przygotuj listę grup do usunięcia
        List<java.util.Map<String, String>> groupsToDelete = new ArrayList<>();
        java.util.Map<String, String> group1Map = new java.util.HashMap<>();
        group1Map.put("manufacturer", manufacturer1);
        group1Map.put("groupName", group1);
        groupsToDelete.add(group1Map);
        
        java.util.Map<String, String> group3Map = new java.util.HashMap<>();
        group3Map.put("manufacturer", manufacturer2);
        group3Map.put("groupName", group3);
        groupsToDelete.add(group3Map);

        // Lista do przechowywania progress updates
        List<ProductService.DeleteProgress> progressUpdates = new ArrayList<>();

        // Usuń grupy z progress tracking
        productService.deleteMultipleGroupsWithProgress(
            ProductCategory.TILE,
            groupsToDelete,
            (progress) -> {
                progressUpdates.add(progress);
                logger.info("📊 Progress update: {}/{} ({}%) - {}", 
                           progress.getProcessedGroups(), 
                           progress.getTotalGroups(),
                           progress.getPercentage(),
                           progress.getCurrentGroup());
            }
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteMultipleGroupsWithProgress: {}ms", duration);

        // Sprawdź czy produkty zostały usunięte
        long countGroup1 = productRepository.findByCategory(ProductCategory.TILE).stream()
                .filter(p -> manufacturer1.equals(p.getManufacturer()) && group1.equals(p.getGroupName()))
                .count();
        long countGroup2 = productRepository.findByCategory(ProductCategory.TILE).stream()
                .filter(p -> manufacturer1.equals(p.getManufacturer()) && group2.equals(p.getGroupName()))
                .count();
        long countGroup3 = productRepository.findByCategory(ProductCategory.TILE).stream()
                .filter(p -> manufacturer2.equals(p.getManufacturer()) && group3.equals(p.getGroupName()))
                .count();

        assertEquals(0, countGroup1, "Wszystkie produkty grupy 1 powinny być usunięte");
        assertEquals(5, countGroup2, "Produkty grupy 2 nie powinny być usunięte (nie była w liście)");
        assertEquals(0, countGroup3, "Wszystkie produkty grupy 3 powinny być usunięte");

        // Sprawdź progress updates
        assertFalse(progressUpdates.isEmpty(), "Powinny być wysłane progress updates");
        ProductService.DeleteProgress finalProgress = progressUpdates.get(progressUpdates.size() - 1);
        assertEquals("completed", finalProgress.getStatus(), "Ostatni status powinien być 'completed'");
        assertEquals(100, finalProgress.getPercentage(), "Ostatni progress powinien być 100%");
        assertEquals(2, finalProgress.getProcessedGroups(), "Powinno być przetworzone 2 grupy");
        assertEquals(2, finalProgress.getTotalGroups(), "Powinno być 2 grupy łącznie");
    }

    @Test
    void testDeleteMultipleGroupsWithProgress_Performance_ManyGroups() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST WYDAJNOŚCIOWY: deleteMultipleGroupsWithProgress - wiele grup (216 grup, 3240 produktów)");

        // ⚡ OPTYMALIZACJA: Utwórz 216 grup z ~15 produktami każda (3240 produktów łącznie) używając JDBC batch insert
        long createProductsStart = System.currentTimeMillis();
        List<java.util.Map<String, String>> groupsToDelete = new ArrayList<>();
        
        // Przygotuj dane grup
        for (int groupIndex = 0; groupIndex < 216; groupIndex++) {
            String manufacturer = "Manufacturer " + (groupIndex % 20); // 20 różnych producentów
            String groupName = "Group " + groupIndex;
            
            java.util.Map<String, String> groupMap = new java.util.HashMap<>();
            groupMap.put("manufacturer", manufacturer);
            groupMap.put("groupName", groupName);
            groupsToDelete.add(groupMap);
        }
        
        // ⚡ JDBC batch insert dla szybkości (jak w createProductsBatch)
        String sql = "INSERT INTO products " +
                    "(name, manufacturer, category, group_name, retail_price, purchase_price, " +
                    "selling_price, unit, quantity_converter, quantity, mapper_name, discount, " +
                    "discount_calculation_method, basic_discount, promotion_discount, " +
                    "additional_discount, skonto_discount, margin_percent, accessory_type, " +
                    "product_type, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int totalProducts = 216 * 15; // 3240 produktów
        int batchSize = 1000;
        int totalBatches = (int)Math.ceil((double)totalProducts / batchSize);
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalProducts);
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            int groupIndex = i / 15;
                            int productIndex = i % 15;
                            String manufacturer = "Manufacturer " + (groupIndex % 20);
                            String groupName = "Group " + groupIndex;
                            
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, "Product " + groupIndex + "-" + productIndex);
                            pstmt.setString(paramIndex++, manufacturer);
                            pstmt.setString(paramIndex++, ProductCategory.TILE.name());
                            pstmt.setString(paramIndex++, groupName);
                            pstmt.setDouble(paramIndex++, 100.0 + (groupIndex * 15) + productIndex);
                            pstmt.setDouble(paramIndex++, 80.0 + (groupIndex * 15) + productIndex);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setDouble(paramIndex++, 1.0);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, "Mapper " + groupIndex + "-" + productIndex);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setInt(paramIndex++, 0);
                            pstmt.setDouble(paramIndex++, 0.0);
                            pstmt.setString(paramIndex++, null);
                            pstmt.setString(paramIndex++, "TYPE1");
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        pstmt.executeBatch();
                    }
                } catch (SQLException e) {
                    logger.error("❌ Błąd podczas batch insert produktów: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch insert produktów", e);
                }
            }
        });
        
        entityManager.flush();
        
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] Utworzenie 3240 produktów w 216 grupach (JDBC batch insert): {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        logger.info("📊 Utworzono {} produktów w {} grupach", totalProducts, groupsToDelete.size());

        // Lista do przechowywania progress updates
        List<ProductService.DeleteProgress> progressUpdates = new ArrayList<>();

        // Usuń wszystkie grupy z progress tracking
        long deleteStartTime = System.currentTimeMillis();
        productService.deleteMultipleGroupsWithProgress(
            ProductCategory.TILE,
            groupsToDelete,
            (progress) -> {
                progressUpdates.add(progress);
            }
        );
        long deleteEndTime = System.currentTimeMillis();
        long deleteDuration = deleteEndTime - deleteStartTime;

        long totalDuration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteMultipleGroupsWithProgress - 216 grup, 3240 produktów: {}ms ({}s)", 
                   deleteDuration, deleteDuration / 1000.0);
        logger.info("⏱️ [PERFORMANCE] Całkowity czas testu: {}ms ({}s)", 
                   totalDuration, totalDuration / 1000.0);

        // Sprawdź czy wszystkie produkty zostały usunięte
        long countAfter = productRepository.findByCategory(ProductCategory.TILE).stream()
                .filter(p -> {
                    return groupsToDelete.stream().anyMatch(group -> 
                        group.get("manufacturer").equals(p.getManufacturer()) && 
                        group.get("groupName").equals(p.getGroupName())
                    );
                })
                .count();

        logger.info("📊 Produktów po usunięciu: {}", countAfter);
        
        assertEquals(0, countAfter, "Wszystkie produkty z wybranych grup powinny być usunięte");
        assertTrue(deleteDuration < 10000, 
                  "Usuwanie 3240 produktów z 216 grup powinno zakończyć się w ciągu 10 sekund (zoptymalizowane: 1 batch DELETE)");
        
        // Sprawdź progress updates
        assertFalse(progressUpdates.isEmpty(), "Powinny być wysłane progress updates");
        ProductService.DeleteProgress finalProgress = progressUpdates.get(progressUpdates.size() - 1);
        assertEquals("completed", finalProgress.getStatus(), "Ostatni status powinien być 'completed'");
        assertEquals(100, finalProgress.getPercentage(), "Ostatni progress powinien być 100%");
        assertEquals(216, finalProgress.getProcessedGroups(), "Powinno być przetworzone 216 grup");
        assertEquals(216, finalProgress.getTotalGroups(), "Powinno być 216 grup łącznie");
        assertEquals(3240, finalProgress.getDeletedProducts(), "Powinno być usunięte 3240 produktów");
    }

    // ========== TESTY WYDAJNOŚCIOWE ==========

    @Test
    void testSetGroupOption_Performance_LargeGroup() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: setGroupOption - duża grupa (1000 produktów)");

        // Utwórz dużą grupę z 1000 produktami
        List<Product> largeProducts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setManufacturer("Large Manufacturer");
            product.setGroupName("Large Group");
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setMapperName("Mapper " + i);
            product.setIsMainOption(GroupOption.NONE);
            largeProducts.add(product);
        }
        productRepository.saveAll(largeProducts);

        long operationStart = System.currentTimeMillis();
        List<Product> updated = productService.setGroupOption(
            ProductCategory.TILE,
            "Large Manufacturer",
            "Large Group",
            GroupOption.MAIN
        );
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] setGroupOption (1000 produktów): {}ms ({}s) | zaktualizowano: {}", 
                   duration, duration / 1000.0, updated.size());

        assertNotNull(updated);
        assertEquals(1000, updated.size());
        assertTrue(duration < 5000, "Operacja powinna zakończyć się w ciągu 5 sekund");
    }

    @Test
    void testRenameManufacturer_Performance_LargeBatch() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: renameManufacturer - duża partia (500 produktów)");

        // Utwórz 500 produktów
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setManufacturer("Old Manufacturer");
            product.setGroupName("Group " + (i % 10));
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            products.add(product);
        }
        productRepository.saveAll(products);

        long operationStart = System.currentTimeMillis();
        List<Product> updated = productService.renameManufacturer(
            ProductCategory.TILE,
            "Old Manufacturer",
            "New Manufacturer"
        );
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] renameManufacturer (500 produktów): {}ms ({}s) | zaktualizowano: {}", 
                   duration, duration / 1000.0, updated.size());

        assertNotNull(updated);
        assertEquals(500, updated.size());
        assertTrue(duration < 5000, "Operacja powinna zakończyć się w ciągu 5 sekund");
    }

    @Test
    void testGetAttributeSuggestions_Performance_ManyProducts() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: getAttributeSuggestions - wiele produktów (1000)");

        // Utwórz 1000 atrybutów dla grup produktów
        // ⚠️ WAŻNE: Użyj unikalnych kombinacji category+manufacturer+groupName, żeby uniknąć duplikatów
        // Lub sprawdź czy istnieje przed zapisem
        for (int i = 0; i < 1000; i++) {
            String manufacturer = "Manufacturer " + (i % 10);
            String groupName = "Group " + (i % 20);
            
            // Sprawdź czy już istnieje
            java.util.Optional<pl.koszela.nowoczesnebud.Model.ProductGroupAttributes> existing = 
                productGroupAttributesRepository.findByCategoryAndManufacturerAndGroupName(
                    ProductCategory.TILE, manufacturer, groupName);
            
            if (existing.isPresent()) {
                // Aktualizuj istniejący
                pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes = existing.get();
                groupAttributes.setAttributes("{\"kolor\":[\"kolor" + (i % 5) + "\"],\"kształt\":[\"kształt" + (i % 3) + "\"]}");
                productGroupAttributesRepository.save(groupAttributes);
            } else {
                // Utwórz nowy
                pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes = 
                    new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes();
                groupAttributes.setCategory(ProductCategory.TILE);
                groupAttributes.setManufacturer(manufacturer);
                groupAttributes.setGroupName(groupName);
                groupAttributes.setAttributes("{\"kolor\":[\"kolor" + (i % 5) + "\"],\"kształt\":[\"kształt" + (i % 3) + "\"]}");
                productGroupAttributesRepository.save(groupAttributes);
            }
        }

        long operationStart = System.currentTimeMillis();
        Map<String, List<String>> suggestions = productService.getAttributeSuggestions(ProductCategory.TILE);
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] getAttributeSuggestions (1000 produktów): {}ms ({}s) | znaleziono: {} atrybutów", 
                   duration, duration / 1000.0, suggestions.size());

        assertNotNull(suggestions);
        assertTrue(duration < 3000, "Operacja powinna zakończyć się w ciągu 3 sekund");
    }

    // ========== TESTY PRZYPADKÓW BRZEGOWYCH ==========

    @Test
    void testSetGroupOption_NonExistentGroup() {
        logger.info("🧪 TEST BRZEGOWY: setGroupOption - nieistniejąca grupa");

        List<Product> updated = productService.setGroupOption(
            ProductCategory.TILE,
            "Non-existent Manufacturer",
            "Non-existent Group",
            GroupOption.MAIN
        );

        logger.info("⏱️ [PERFORMANCE] setGroupOption (non-existent): zaktualizowano: {}", updated.size());

        assertNotNull(updated);
        assertTrue(updated.isEmpty());
    }

    @Test
    void testRenameManufacturer_NonExistentManufacturer() {
        logger.info("🧪 TEST BRZEGOWY: renameManufacturer - nieistniejący producent");

        List<Product> updated = productService.renameManufacturer(
            ProductCategory.TILE,
            "Non-existent Manufacturer",
            "New Manufacturer"
        );

        logger.info("⏱️ [PERFORMANCE] renameManufacturer (non-existent): zaktualizowano: {}", updated.size());

        assertNotNull(updated);
        assertTrue(updated.isEmpty());
    }

    @Test
    void testDeleteAllByManufacturer_NonExistentManufacturer() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: deleteAllByManufacturer - nieistniejący producent");

        // Nie powinno rzucić wyjątku
        productService.deleteAllByManufacturer(ProductCategory.TILE, "Non-existent Manufacturer");

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteAllByManufacturer (non-existent): {}ms", duration);
    }

    @Test
    void testGetAttributeSuggestions_NoAttributes() {
        logger.info("🧪 TEST BRZEGOWY: getAttributeSuggestions - brak atrybutów");

        // Nie tworzymy atrybutów dla grupy (brak atrybutów)

        Map<String, List<String>> suggestions = productService.getAttributeSuggestions(ProductCategory.TILE);

        logger.info("⏱️ [PERFORMANCE] getAttributeSuggestions (no attributes): znaleziono: {} atrybutów", 
                   suggestions.size());

        assertNotNull(suggestions);
        // Może być puste lub zawierać atrybuty z innych produktów
    }

    @Test
    void testSaveGroupAttributes_EmptyAttributes() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: saveGroupAttributes - puste atrybuty (usuwanie)");

        // Utwórz atrybuty
        pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes = 
            new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes();
        groupAttributes.setCategory(ProductCategory.TILE);
        groupAttributes.setManufacturer("Test Manufacturer");
        groupAttributes.setGroupName("Test Group");
        groupAttributes.setAttributes("{\"kolor\":[\"czerwony\"]}");
        productGroupAttributesRepository.save(groupAttributes);

        // Usuń atrybuty (przekaż pustą mapę)
        GroupAttributesRequest request = new GroupAttributesRequest();
        request.setCategory(ProductCategory.TILE);
        request.setManufacturer("Test Manufacturer");
        request.setGroupName("Test Group");
        request.setAttributes(new HashMap<>());

        productService.saveGroupAttributes(request);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveGroupAttributes (empty - delete): {}ms", duration);

        // Sprawdź czy atrybuty zostały usunięte
        String savedAttributes = productService.getGroupAttributes(
            ProductCategory.TILE,
            "Test Manufacturer",
            "Test Group"
        );
        assertNull(savedAttributes);
    }
}

