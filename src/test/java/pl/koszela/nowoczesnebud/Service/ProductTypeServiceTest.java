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
 * 🧪 TESTY DLA ProductTypeService
 * 
 * Testuje:
 * - Operacje CRUD na typach produktów
 * - Obliczenia ceny zakupu (calculatePurchasePrice)
 * - Obliczenia ceny detalowej (calculateDetalPrice)
 * - Pobieranie typów produktów według grupy
 * - Wydajność dla dużych zbiorów typów produktów
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class ProductTypeServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductTypeServiceTest.class);

    @Autowired
    private ProductTypeService productTypeService;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private ProductGroupRepository productGroupRepository;

    private ProductGroup testProductGroup;
    private ProductType testProductType;

    @BeforeEach
    void setUp() {
        // Utwórz testową grupę produktów
        testProductGroup = new ProductGroup();
        testProductGroup.setTypeName("Test Group");
        testProductGroup = productGroupRepository.save(testProductGroup);

        // Utwórz testowy typ produktu
        testProductType = new ProductType();
        testProductType.setName("Test Product Type");
        testProductType.setDetalPrice(100.0);
        testProductType.setQuantity(2.0);
        testProductType.setPurchasePrice(80.0);
        testProductType.setBasicDiscount(10);
        testProductType.setAdditionalDiscount(5);
        testProductType.setPromotionDiscount(0);
        testProductType.setSkontoDiscount(2);
        testProductType.setMapperName("Test Mapper");
        testProductType.setQuantityConverter(1.0);
        testProductType.setMarginUnitDetalPrice(25.0);
        testProductType = productTypeRepository.save(testProductType);

        // Powiąż z grupą
        testProductGroup.getProductTypeList().add(testProductType);
        testProductGroup = productGroupRepository.save(testProductGroup);
    }

    // ========== TESTY POPRAWNOŚCIOWE ==========

    @Test
    void testFindProductTypesByProductGroupId_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: findProductTypesByProductGroupId - poprawność");

        List<ProductType> types = productTypeService.findProductTypesByProductGroupId(testProductGroup.getId());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] findProductTypesByProductGroupId: {}ms | znaleziono: {} typów", 
                   duration, types.size());

        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertTrue(types.stream().anyMatch(t -> t.getId() == testProductType.getId()));
    }

    @Test
    void testGetProductType_Value1_PowierzchniaPolaci() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getProductType - value=1 (Powierzchnia polaci)");

        // Utwórz typ produktu z mapperName "Powierzchnia polaci"
        ProductType powierzchniaType = new ProductType();
        powierzchniaType.setName("Powierzchnia polaci");
        powierzchniaType.setMapperName("Powierzchnia polaci");
        powierzchniaType.setDetalPrice(200.0);
        powierzchniaType.setQuantity(1.0);
        powierzchniaType.setQuantityConverter(1.0);
        powierzchniaType = productTypeRepository.save(powierzchniaType);

        testProductGroup.getProductTypeList().add(powierzchniaType);
        testProductGroup = productGroupRepository.save(testProductGroup);

        ProductType found = productTypeService.getProductType(1, testProductGroup);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getProductType (value=1): {}ms", duration);

        assertNotNull(found);
        assertEquals("Powierzchnia polaci", found.getMapperName());
    }

    @Test
    void testGetProductType_Value2_NotPowierzchniaPolaci() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getProductType - value=2 (nie Powierzchnia polaci)");

        ProductType found = productTypeService.getProductType(2, testProductGroup);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getProductType (value=2): {}ms", duration);

        assertNotNull(found);
        assertNotEquals("Powierzchnia polaci", found.getMapperName());
    }

    @Test
    void testFindIdGroupOfType_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: findIdGroupOfType - poprawność");

        long groupId = productTypeService.findIdGroupOfType(testProductType.getId());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] findIdGroupOfType: {}ms | groupId: {}", duration, groupId);

        assertEquals(testProductGroup.getId(), groupId);
    }

    @Test
    void testCalculatePurchasePrice_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: calculatePurchasePrice - poprawność");

        double detalPrice = 100.0;
        ProductType productType = new ProductType();
        productType.setBasicDiscount(10);
        productType.setAdditionalDiscount(5);
        productType.setPromotionDiscount(0);
        productType.setSkontoDiscount(2);

        double purchasePrice = productTypeService.calculatePurchasePrice(detalPrice, productType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculatePurchasePrice: {}ms | detalPrice: {}, purchasePrice: {}", 
                   duration, detalPrice, purchasePrice);

        // Cena zakupu = detalPrice * (100-10)/100 * (100-5)/100 * (100-0)/100 * (100-2)/100
        // = 100 * 0.9 * 0.95 * 1.0 * 0.98 = 83.79
        double expectedPrice = detalPrice * 0.9 * 0.95 * 1.0 * 0.98;
        assertEquals(expectedPrice, purchasePrice, 0.01);
    }

    @Test
    void testCalculateDetalPrice_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: calculateDetalPrice - poprawność");

        ProductType productType = new ProductType();
        productType.setPurchasePrice(80.0);
        productType.setMarginUnitDetalPrice(25.0);

        double detalPrice = productTypeService.calculateDetalPrice(productType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateDetalPrice: {}ms | purchasePrice: {}, detalPrice: {}", 
                   duration, productType.getPurchasePrice(), detalPrice);

        // Cena detalowa = purchasePrice * (100 + marginUnitDetalPrice) / 100
        // = 80 * 1.25 = 100.0
        double expectedPrice = 80.0 * 1.25;
        assertEquals(expectedPrice, detalPrice, 0.01);
    }

    @Test
    void testSave_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: save - poprawność");

        ProductType newType = new ProductType();
        newType.setName("New Product Type");
        newType.setDetalPrice(150.0);
        newType.setQuantity(1.0);
        newType.setMapperName("New Mapper");
        newType.setQuantityConverter(1.0);

        ProductType saved = productTypeService.save(newType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] save: {}ms | savedId: {}", duration, saved.getId());

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("New Product Type", saved.getName());
    }

    @Test
    void testSaveAll_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveAll - poprawność");

        List<ProductType> types = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ProductType type = new ProductType();
            type.setName("Type " + i);
            type.setDetalPrice(100.0 + i * 10);
            type.setQuantity(1.0 + i);
            type.setMapperName("Mapper " + i);
            type.setQuantityConverter(1.0);
            types.add(type);
        }

        List<ProductType> saved = productTypeService.saveAll(types);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveAll: {}ms | zapisano: {} typów", duration, saved.size());

        assertNotNull(saved);
        assertEquals(5, saved.size());
        saved.forEach(t -> assertNotNull(t.getId()));
    }

    // ========== TESTY WYDAJNOŚCIOWE ==========

    @Test
    void testFindProductTypesByProductGroupId_Performance_LargeGroup() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST WYDAJNOŚCIOWY: findProductTypesByProductGroupId - duża grupa (500 typów)");

        // Utwórz dużą grupę z 500 typami produktów
        ProductGroup largeGroup = new ProductGroup();
        largeGroup.setTypeName("Large Group");
        largeGroup = productGroupRepository.save(largeGroup);

        List<ProductType> largeProductTypes = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            ProductType productType = new ProductType();
            productType.setName("Product Type " + i);
            productType.setDetalPrice(100.0 + i);
            productType.setQuantity(1.0 + i * 0.1);
            productType.setMapperName("Mapper " + i);
            productType.setQuantityConverter(1.0);
            largeProductTypes.add(productType);
        }

        largeProductTypes = productTypeRepository.saveAll(largeProductTypes);
        largeGroup.setProductTypeList(largeProductTypes);
        largeGroup = productGroupRepository.save(largeGroup);

        long operationStart = System.currentTimeMillis();
        List<ProductType> result = productTypeService.findProductTypesByProductGroupId(largeGroup.getId());
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] findProductTypesByProductGroupId (500 typów): {}ms ({}s)", 
                   duration, duration / 1000.0);

        assertNotNull(result);
        assertEquals(500, result.size());
        assertTrue(duration < 3000, "Operacja powinna zakończyć się w ciągu 3 sekund");
    }

    @Test
    void testCalculatePurchasePrice_Performance_ManyCalculations() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST WYDAJNOŚCIOWY: calculatePurchasePrice - wiele obliczeń (1000)");

        ProductType productType = new ProductType();
        productType.setBasicDiscount(10);
        productType.setAdditionalDiscount(5);
        productType.setPromotionDiscount(0);
        productType.setSkontoDiscount(2);

        long operationStart = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            double detalPrice = 100.0 + i;
            productTypeService.calculatePurchasePrice(detalPrice, productType);
        }
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] calculatePurchasePrice (1000 obliczeń): {}ms ({}s)", 
                   duration, duration / 1000.0);

        assertTrue(duration < 1000, "1000 obliczeń powinno zakończyć się w ciągu 1 sekundy");
    }

    @Test
    void testSaveAll_Performance_LargeBatch() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST WYDAJNOŚCIOWY: saveAll - duża partia (1000 typów)");

        List<ProductType> types = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ProductType type = new ProductType();
            type.setName("Type " + i);
            type.setDetalPrice(100.0 + i);
            type.setQuantity(1.0 + i * 0.1);
            type.setMapperName("Mapper " + i);
            type.setQuantityConverter(1.0);
            types.add(type);
        }

        long operationStart = System.currentTimeMillis();
        List<ProductType> saved = productTypeService.saveAll(types);
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] saveAll (1000 typów): {}ms ({}s)", duration, duration / 1000.0);

        assertNotNull(saved);
        assertEquals(1000, saved.size());
        assertTrue(duration < 10000, "Operacja powinna zakończyć się w ciągu 10 sekund");
    }

    // ========== TESTY PRZYPADKÓW BRZEGOWYCH ==========

    @Test
    void testCalculatePurchasePrice_ZeroDiscounts() {
        logger.info("🧪 TEST BRZEGOWY: calculatePurchasePrice - wszystkie rabaty = 0");

        double detalPrice = 100.0;
        ProductType productType = new ProductType();
        productType.setBasicDiscount(0);
        productType.setAdditionalDiscount(0);
        productType.setPromotionDiscount(0);
        productType.setSkontoDiscount(0);

        double purchasePrice = productTypeService.calculatePurchasePrice(detalPrice, productType);

        logger.info("⏱️ [PERFORMANCE] calculatePurchasePrice (zero discounts): purchasePrice={}", purchasePrice);

        // Cena zakupu = detalPrice * 1.0 * 1.0 * 1.0 * 1.0 = detalPrice
        assertEquals(detalPrice, purchasePrice, 0.01);
    }

    @Test
    void testCalculatePurchasePrice_MaxDiscounts() {
        logger.info("🧪 TEST BRZEGOWY: calculatePurchasePrice - maksymalne rabaty (100%)");

        double detalPrice = 100.0;
        ProductType productType = new ProductType();
        productType.setBasicDiscount(100);
        productType.setAdditionalDiscount(100);
        productType.setPromotionDiscount(100);
        productType.setSkontoDiscount(100);

        double purchasePrice = productTypeService.calculatePurchasePrice(detalPrice, productType);

        logger.info("⏱️ [PERFORMANCE] calculatePurchasePrice (max discounts): purchasePrice: {}", purchasePrice);

        // Cena zakupu = detalPrice * 0.0 * 0.0 * 0.0 * 0.0 = 0.0
        assertEquals(0.0, purchasePrice, 0.01);
    }

    @Test
    void testCalculateDetalPrice_ZeroMargin() {
        logger.info("🧪 TEST BRZEGOWY: calculateDetalPrice - marża = 0%");

        ProductType productType = new ProductType();
        productType.setPurchasePrice(80.0);
        productType.setMarginUnitDetalPrice(0.0);

        double detalPrice = productTypeService.calculateDetalPrice(productType);

        logger.info("⏱️ [PERFORMANCE] calculateDetalPrice (0% margin): detalPrice={}", detalPrice);

        // Cena detalowa = purchasePrice * 1.0 = purchasePrice
        assertEquals(80.0, detalPrice, 0.01);
    }

    @Test
    void testCalculateDetalPrice_VeryHighMargin() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST BRZEGOWY: calculateDetalPrice - bardzo wysoka marża (200%)");

        ProductType productType = new ProductType();
        productType.setPurchasePrice(80.0);
        productType.setMarginUnitDetalPrice(200.0);

        double detalPrice = productTypeService.calculateDetalPrice(productType);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] calculateDetalPrice (200% margin): {}ms | detalPrice: {}", 
                   duration, detalPrice);

        // Cena detalowa = purchasePrice * 3.0 = 240.0
        assertEquals(240.0, detalPrice, 0.01);
    }

    @Test
    void testFindProductTypesByProductGroupId_EmptyGroup() {
        logger.info("🧪 TEST BRZEGOWY: findProductTypesByProductGroupId - pusta grupa");

        ProductGroup emptyGroup = new ProductGroup();
        emptyGroup.setTypeName("Empty Group");
        emptyGroup = productGroupRepository.save(emptyGroup);

        List<ProductType> types = productTypeService.findProductTypesByProductGroupId(emptyGroup.getId());

        logger.info("⏱️ [PERFORMANCE] findProductTypesByProductGroupId (empty group): znaleziono: {} typów", types.size());

        assertNotNull(types);
        assertTrue(types.isEmpty());
    }

    @Test
    void testGetProductType_NotFound() {
        logger.info("🧪 TEST BRZEGOWY: getProductType - nie znaleziono");

        ProductGroup emptyGroup = new ProductGroup();
        emptyGroup.setTypeName("Empty Group");
        emptyGroup = productGroupRepository.save(emptyGroup);

        ProductType found1 = productTypeService.getProductType(1, emptyGroup);
        ProductType found2 = productTypeService.getProductType(2, emptyGroup);

        logger.info("⏱️ [PERFORMANCE] getProductType (not found): found1={}, found2={}", found1, found2);

        assertNull(found1);
        assertNull(found2);
    }
}

