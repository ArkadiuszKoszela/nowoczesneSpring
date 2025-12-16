package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Repository.ProductGroupRepository;
import pl.koszela.nowoczesnebud.Repository.ProductTypeRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 TESTY DLA ProductGroupService
 * 
 * Testuje:
 * - Operacje CRUD na grupach produktów
 * - Obliczenia marży i rabatów dla grup
 * - Ustawianie opcji (główna/opcjonalna)
 * - Obliczenia cen całkowitych (totalPriceDetal, totalPriceAfterDiscount, totalProfit, totalSellingPrice)
 * - Zapisywanie rabatów dla grup produktów
 * - Wydajność dla dużych grup produktów
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class ProductGroupServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductGroupServiceTest.class);

    @Autowired
    private ProductGroupService productGroupService;

    @Autowired
    private ProductGroupRepository productGroupRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    private ProductGroup testProductGroup;
    private List<ProductType> testProductTypes;

    @BeforeEach
    void setUp() {
        // Utwórz testową grupę produktów
        testProductGroup = new ProductGroup();
        testProductGroup.setTypeName("Test Group");
        testProductGroup.setOption(false);
        testProductGroup = productGroupRepository.save(testProductGroup);

        // Utwórz testowe typy produktów
        testProductTypes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ProductType productType = new ProductType();
            productType.setName("Product Type " + i);
            productType.setDetalPrice(100.0 + i * 10);
            productType.setQuantity(2.0 + i);
            productType.setPurchasePrice(80.0 + i * 8);
            productType.setBasicDiscount(10);
            productType.setAdditionalDiscount(5);
            productType.setPromotionDiscount(0);
            productType.setSkontoDiscount(2);
            productType.setMapperName("Mapper " + i);
            productType.setQuantityConverter(1.0);
            testProductTypes.add(productType);
        }

        // Zapisz typy produktów i powiąż z grupą
        testProductTypes = productTypeRepository.saveAll(testProductTypes);
        testProductGroup.setProductTypeList(testProductTypes);
        testProductGroup = productGroupRepository.save(testProductGroup);
    }

    // ========== TESTY POPRAWNOŚCIOWE ==========

    @Test
    void testFindById_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: findById - poprawność");

        ProductGroup found = productGroupService.findById(testProductGroup.getId());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] findById: {}ms", duration);

        assertNotNull(found);
        assertEquals(testProductGroup.getId(), found.getId());
        assertEquals("Test Group", found.getTypeName());
    }

    @Test
    void testFindById_NotFound() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: findById - nie znaleziono");

        ProductGroup found = productGroupService.findById(999999L);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] findById (not found): {}ms", duration);

        assertNull(found);
    }

    @Test
    void testSetOption_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: setOption - poprawność");

        ProductGroup updated = productGroupService.setOption(testProductGroup);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] setOption: {}ms", duration);

        assertNotNull(updated);
        assertNotNull(updated.getProductTypeList());
        assertEquals(testProductGroup.getId(), updated.getId());
    }

    @Test
    void testSaveDiscounts_WithPowierzchniaPolaci_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveDiscounts - z 'Powierzchnia polaci'");

        // Utwórz typ produktu z mapperName "Powierzchnia polaci"
        ProductType powierzchniaType = new ProductType();
        powierzchniaType.setName("Powierzchnia polaci");
        powierzchniaType.setMapperName("Powierzchnia polaci");
        powierzchniaType.setDetalPrice(200.0);
        powierzchniaType.setQuantity(1.0);
        powierzchniaType.setBasicDiscount(15);
        powierzchniaType.setAdditionalDiscount(5);
        powierzchniaType.setPromotionDiscount(0);
        powierzchniaType.setSkontoDiscount(2);
        powierzchniaType.setQuantityConverter(1.0);
        powierzchniaType = productTypeRepository.save(powierzchniaType);

        // Dodaj do grupy
        testProductGroup.getProductTypeList().add(powierzchniaType);
        testProductGroup = productGroupRepository.save(testProductGroup);

        List<ProductGroup> result = productGroupService.saveDiscounts(powierzchniaType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveDiscounts (z Powierzchnia polaci): {}ms", duration);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Sprawdź czy cena zakupu została obliczona
        ProductType savedType = productTypeRepository.findById(powierzchniaType.getId()).orElse(null);
        assertNotNull(savedType);
        assertTrue(savedType.getPurchasePrice() > 0);
    }

    @Test
    void testSaveDiscounts_WithoutPowierzchniaPolaci_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveDiscounts - bez 'Powierzchnia polaci'");

        // Utwórz typ produktu bez mapperName "Powierzchnia polaci"
        ProductType normalType = new ProductType();
        normalType.setName("Normal Product");
        normalType.setMapperName("Normal Mapper");
        normalType.setDetalPrice(150.0);
        normalType.setQuantity(3.0);
        normalType.setBasicDiscount(20);
        normalType.setAdditionalDiscount(10);
        normalType.setPromotionDiscount(5);
        normalType.setSkontoDiscount(3);
        normalType.setQuantityConverter(1.0);
        normalType = productTypeRepository.save(normalType);

        // Dodaj do grupy
        testProductGroup.getProductTypeList().add(normalType);
        testProductGroup = productGroupRepository.save(testProductGroup);

        List<ProductGroup> result = productGroupService.saveDiscounts(normalType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveDiscounts (bez Powierzchnia polaci): {}ms", duration);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Sprawdź czy wszystkie typy produktów (oprócz Powierzchnia polaci) mają zaktualizowane rabaty
        ProductType savedType = productTypeRepository.findById(normalType.getId()).orElse(null);
        assertNotNull(savedType);
        assertEquals(20, savedType.getBasicDiscount());
        assertEquals(10, savedType.getAdditionalDiscount());
        assertEquals(5, savedType.getPromotionDiscount());
        assertEquals(3, savedType.getSkontoDiscount());
        assertTrue(savedType.getPurchasePrice() > 0);
    }

    @Test
    void testCalculateMargin_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: calculateMargin - poprawność");

        Integer marginPercent = 25;
        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(marginPercent, null, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateMargin: {}ms", duration);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        ProductGroup updatedGroup = result.get(0);
        assertNotNull(updatedGroup.getTotalSellingPrice());
        assertTrue(updatedGroup.getTotalSellingPrice() > 0);

        // Sprawdź czy ceny sprzedaży zostały obliczone dla typów produktów
        for (ProductType productType : updatedGroup.getProductTypeList()) {
            assertTrue(productType.getSellingPrice() > 0);
            // Cena sprzedaży = cena zakupu * (100 + marża) / 100
            double expectedSellingPrice = productType.getPurchasePrice() * (100 + marginPercent) / 100;
            assertEquals(expectedSellingPrice, productType.getSellingPrice(), 0.01);
        }
    }

    @Test
    void testCalculateDiscount_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: calculateDiscount - poprawność");

        Integer discountPercent = 15;
        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(null, discountPercent, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateDiscount: {}ms", duration);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        ProductGroup updatedGroup = result.get(0);
        assertNotNull(updatedGroup.getTotalSellingPrice());
        assertTrue(updatedGroup.getTotalSellingPrice() > 0);

        // Sprawdź czy ceny sprzedaży zostały obliczone dla typów produktów
        for (ProductType productType : updatedGroup.getProductTypeList()) {
            assertTrue(productType.getSellingPrice() > 0);
            // Cena sprzedaży = cena detal * (100 - rabat) / 100
            double expectedSellingPrice = productType.getDetalPrice() * (100 - discountPercent) / 100;
            assertEquals(expectedSellingPrice, productType.getSellingPrice(), 0.01);
        }
    }

    @Test
    void testGetAllProductGroups_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getAllProductGroups - poprawność");

        List<ProductGroup> allGroups = productGroupService.getAllProductGroups();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getAllProductGroups: {}ms | znaleziono: {} grup", duration, allGroups.size());

        assertNotNull(allGroups);
        assertTrue(allGroups.size() > 0);
        assertTrue(allGroups.stream().anyMatch(g -> g.getId() == testProductGroup.getId()));
    }

    @Test
    void testFindMainProductGroup_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: findMainProductGroup - poprawność");

        // Utwórz grupę główną
        ProductGroup mainGroup = new ProductGroup();
        mainGroup.setTypeName("Main Group");
        mainGroup.setOption(true);
        mainGroup = productGroupRepository.save(mainGroup);

        ProductGroup found = productGroupService.findMainProductGroup();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] findMainProductGroup: {}ms", duration);

        assertNotNull(found);
        assertEquals(mainGroup.getId(), found.getId());
        assertTrue(found.getOption());
    }

    @Test
    void testHasOnlyOneMainProductGroup_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: hasOnlyOneMainProductGroup - poprawność");

        // Utwórz dwie grupy główne
        ProductGroup mainGroup1 = new ProductGroup();
        mainGroup1.setTypeName("Main Group 1");
        mainGroup1.setOption(true);
        mainGroup1 = productGroupRepository.save(mainGroup1);

        ProductGroup mainGroup2 = new ProductGroup();
        mainGroup2.setTypeName("Main Group 2");
        mainGroup2.setOption(true);
        mainGroup2 = productGroupRepository.save(mainGroup2);

        boolean hasMultiple = productGroupService.hasOnlyOneMainProductGroup();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] hasOnlyOneMainProductGroup: {}ms", duration);

        assertTrue(hasMultiple);
    }

    @Test
    void testFindOptionProductGroups_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: findOptionProductGroups - poprawność");

        // Utwórz grupę opcjonalną
        final ProductGroup optionGroup = new ProductGroup();
        optionGroup.setTypeName("Option Group");
        optionGroup.setOption(false);
        productGroupRepository.save(optionGroup);

        List<ProductGroup> optionGroups = productGroupService.findOptionProductGroups();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] findOptionProductGroups: {}ms | znaleziono: {} grup", duration, optionGroups.size());

        assertNotNull(optionGroups);
        final long optionGroupId = optionGroup.getId();
        assertTrue(optionGroups.stream().anyMatch(g -> g.getId() == optionGroupId));
    }

    @Test
    void testFinCheapestOption_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: finCheapestOption - poprawność");

        // ⚠️ WAŻNE: Upewnij się, że wszystkie istniejące grupy opcjonalne mają ustawione totalSellingPrice > 0
        // (żeby nie wpływały na wynik testu)
        List<ProductGroup> allOptionGroups = productGroupService.findOptionProductGroups();
        for (ProductGroup group : allOptionGroups) {
            if (group.getTotalSellingPrice() == null || group.getTotalSellingPrice() == 0.0) {
                group.setTotalSellingPrice(2000.0); // Wyższa niż nowe grupy, żeby nie wpływała na wynik
                productGroupRepository.save(group);
            }
        }

        // Utwórz dwie grupy opcjonalne z różnymi cenami
        final ProductGroup optionGroup1 = new ProductGroup();
        optionGroup1.setTypeName("Option Group 1");
        optionGroup1.setOption(false);
        optionGroup1.setTotalSellingPrice(1000.0);
        productGroupRepository.save(optionGroup1);

        final ProductGroup optionGroup2 = new ProductGroup();
        optionGroup2.setTypeName("Option Group 2");
        optionGroup2.setOption(false);
        optionGroup2.setTotalSellingPrice(500.0);
        productGroupRepository.save(optionGroup2);

        double cheapest = productGroupService.finCheapestOption();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] finCheapestOption: {}ms | najtańsza: {}", duration, cheapest);

        // Najtańsza powinna być 500.0 (z optionGroup2)
        assertEquals(500.0, cheapest, 0.01);
    }

    // ========== TESTY WYDAJNOŚCIOWE ==========

    @Test
    void testSaveDiscounts_Performance_LargeGroup() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: saveDiscounts - duża grupa produktów (100 typów)");

        // Utwórz dużą grupę z 100 typami produktów
        ProductGroup largeGroup = new ProductGroup();
        largeGroup.setTypeName("Large Group");
        largeGroup = productGroupRepository.save(largeGroup);

        List<ProductType> largeProductTypes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ProductType productType = new ProductType();
            productType.setName("Product Type " + i);
            productType.setDetalPrice(100.0 + i);
            productType.setQuantity(1.0 + i * 0.1);
            productType.setBasicDiscount(10 + i % 20);
            productType.setAdditionalDiscount(5);
            productType.setPromotionDiscount(0);
            productType.setSkontoDiscount(2);
            productType.setMapperName("Mapper " + i);
            productType.setQuantityConverter(1.0);
            largeProductTypes.add(productType);
        }

        largeProductTypes = productTypeRepository.saveAll(largeProductTypes);
        largeGroup.setProductTypeList(largeProductTypes);
        largeGroup = productGroupRepository.save(largeGroup);

        ProductType firstType = largeProductTypes.get(0);
        firstType.setBasicDiscount(25);
        firstType.setAdditionalDiscount(10);

        long operationStart = System.currentTimeMillis();
        List<ProductGroup> result = productGroupService.saveDiscounts(firstType);
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] saveDiscounts (100 typów): {}ms ({}s)", duration, duration / 1000.0);

        assertNotNull(result);
        assertTrue(duration < 5000, "Operacja powinna zakończyć się w ciągu 5 sekund");
    }

    @Test
    void testCalculateMargin_Performance_MultipleGroups() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: calculateMargin - wiele grup (50 grup × 10 typów)");

        // Utwórz 50 grup z 10 typami produktów każda
        List<ProductGroup> groups = new ArrayList<>();
        for (int g = 0; g < 50; g++) {
            ProductGroup group = new ProductGroup();
            group.setTypeName("Group " + g);
            group = productGroupRepository.save(group);

            List<ProductType> productTypes = new ArrayList<>();
            for (int t = 0; t < 10; t++) {
                ProductType productType = new ProductType();
                productType.setName("Type " + g + "-" + t);
                productType.setDetalPrice(100.0 + g * 10 + t);
                productType.setQuantity(1.0 + t * 0.1);
                productType.setPurchasePrice(80.0 + g * 8 + t);
                productType.setBasicDiscount(10);
                productType.setMapperName("Mapper " + g + "-" + t);
                productType.setQuantityConverter(1.0);
                productTypes.add(productType);
            }

            productTypes = productTypeRepository.saveAll(productTypes);
            group.setProductTypeList(productTypes);
            group = productGroupRepository.save(group);
            groups.add(group);
        }

        Integer marginPercent = 20;
        long operationStart = System.currentTimeMillis();
        List<ProductGroup> result = productGroupService.calculateMargin(marginPercent, null, groups);
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] calculateMargin (50 grup × 10 typów = 500 typów): {}ms ({}s)", 
                   duration, duration / 1000.0);

        assertNotNull(result);
        assertEquals(50, result.size());
        assertTrue(duration < 10000, "Operacja powinna zakończyć się w ciągu 10 sekund");
    }

    // ========== TESTY PRZYPADKÓW BRZEGOWYCH ==========

    @Test
    void testSaveDiscounts_EmptyGroup() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: saveDiscounts - pusta grupa");

        ProductGroup emptyGroup = new ProductGroup();
        emptyGroup.setTypeName("Empty Group");
        emptyGroup = productGroupRepository.save(emptyGroup);

        ProductType productType = new ProductType();
        productType.setName("Test Type");
        productType.setMapperName("Test Mapper");
        productType.setDetalPrice(100.0);
        productType.setQuantity(1.0);
        productType.setBasicDiscount(10);
        productType.setQuantityConverter(1.0);
        productType = productTypeRepository.save(productType);

        emptyGroup.getProductTypeList().add(productType);
        emptyGroup = productGroupRepository.save(emptyGroup);

        List<ProductGroup> result = productGroupService.saveDiscounts(productType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveDiscounts (pusta grupa): {}ms", duration);

        assertNotNull(result);
    }

    @Test
    void testCalculateMargin_NullMargin() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: calculateMargin - null margin");

        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(null, null, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateMargin (null margin): {}ms", duration);

        assertNotNull(result);
        // Ceny sprzedaży nie powinny się zmienić jeśli margin i discount są null
    }

    @Test
    void testCalculateMargin_ZeroMargin() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: calculateMargin - marża = 0%");

        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(0, null, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateMargin (0% margin): {}ms", duration);

        assertNotNull(result);
        ProductGroup updatedGroup = result.get(0);
        for (ProductType productType : updatedGroup.getProductTypeList()) {
            // Cena sprzedaży = cena zakupu * (100 + 0) / 100 = cena zakupu
            assertEquals(productType.getPurchasePrice(), productType.getSellingPrice(), 0.01);
        }
    }

    @Test
    void testCalculateDiscount_ZeroDiscount() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: calculateDiscount - rabat = 0%");

        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(null, 0, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateDiscount (0% discount): {}ms", duration);

        assertNotNull(result);
        ProductGroup updatedGroup = result.get(0);
        for (ProductType productType : updatedGroup.getProductTypeList()) {
            // Cena sprzedaży = cena detal * (100 - 0) / 100 = cena detal
            assertEquals(productType.getDetalPrice(), productType.getSellingPrice(), 0.01);
        }
    }

    @Test
    void testCalculateMargin_VeryHighMargin() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: calculateMargin - bardzo wysoka marża (200%)");

        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(200, null, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateMargin (200% margin): {}ms", duration);

        assertNotNull(result);
        ProductGroup updatedGroup = result.get(0);
        for (ProductType productType : updatedGroup.getProductTypeList()) {
            double expectedSellingPrice = productType.getPurchasePrice() * 3.0; // 100 + 200 = 300%
            assertEquals(expectedSellingPrice, productType.getSellingPrice(), 0.01);
        }
    }

    @Test
    void testCalculateDiscount_VeryHighDiscount() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: calculateDiscount - bardzo wysoki rabat (90%)");

        List<ProductGroup> groups = new ArrayList<>();
        groups.add(testProductGroup);

        List<ProductGroup> result = productGroupService.calculateMargin(null, 90, groups);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateDiscount (90% discount): {}ms", duration);

        assertNotNull(result);
        ProductGroup updatedGroup = result.get(0);
        for (ProductType productType : updatedGroup.getProductTypeList()) {
            double expectedSellingPrice = productType.getDetalPrice() * 0.1; // 100 - 90 = 10%
            assertEquals(expectedSellingPrice, productType.getSellingPrice(), 0.01);
        }
    }
}

