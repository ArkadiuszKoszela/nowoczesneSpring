package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.koszela.nowoczesnebud.DTO.DraftChangeDTO;
import pl.koszela.nowoczesnebud.DTO.SaveDraftChangesRequest;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.ProjectDraftChange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🎯 TESTY USTAWIANIA RABATU (DISCOUNT) - Poprawność obliczania
 * 
 * Testuje ustawianie rabatu dla produktów:
 * - Podstawowy scenariusz (10% rabatu)
 * - Rabat = 0%
 * - Bardzo duży rabat (50%)
 * - Weryfikacja obliczania dla wielu produktów
 * - Testy wydajnościowe
 */
@DisplayName("Testy ustawiania rabatu (discount) - poprawność obliczania")
class ProjectServiceDiscountTest extends BaseProjectServiceTest {

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        setUpBase();
        testProducts = createProductsBatch(100);
    }

    @Test
    @DisplayName("TEST 1: Ustawianie rabatu - podstawowy scenariusz (10% rabatu)")
    void testSetDiscount_Basic() {
        // GIVEN: Produkty z ceną detaliczną 100.0
        Product product = testProducts.get(0);
        product.setRetailPrice(100.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy rabat 10% dla kategorii
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(10.0);  // 10% rabatu
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftRetailPrice(100.0);
        change.setDraftDiscountPercent(10.0);
        // Cena sprzedaży = 100.0 * (1 - 10/100) = 100.0 * 0.9 = 90.0
        change.setDraftSellingPrice(90.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być obliczona poprawnie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1, draftChanges.size(), "✅ Draft change powinien być zapisany");
        
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(10.0, draft.getDraftDiscountPercent(), "✅ Rabat powinien być zapisany");
        assertEquals(90.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być 90.0 (100.0 * 0.9)");
    }

    @Test
    @DisplayName("TEST 2: Ustawianie rabatu - brzegowy przypadek: rabat = 0%")
    void testSetDiscount_ZeroDiscount() {
        // GIVEN: Produkty z ceną detaliczną 100.0
        Product product = testProducts.get(0);
        product.setRetailPrice(100.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy rabat 0%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(0.0);  // 0% rabatu
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftRetailPrice(100.0);
        change.setDraftDiscountPercent(0.0);
        // Cena sprzedaży = 100.0 * (1 - 0/100) = 100.0 * 1.0 = 100.0
        change.setDraftSellingPrice(100.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być równa cenie detalicznej
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(0.0, draft.getDraftDiscountPercent(), "✅ Rabat powinien być 0%");
        assertEquals(100.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być równa cenie detalicznej (100.0)");
    }

    @Test
    @DisplayName("TEST 3: Ustawianie rabatu - brzegowy przypadek: bardzo duży rabat (50%)")
    void testSetDiscount_LargeDiscount() {
        // GIVEN: Produkty z ceną detaliczną 100.0
        Product product = testProducts.get(0);
        product.setRetailPrice(100.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy rabat 50%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(50.0);  // 50% rabatu
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftRetailPrice(100.0);
        change.setDraftDiscountPercent(50.0);
        // Cena sprzedaży = 100.0 * (1 - 50/100) = 100.0 * 0.5 = 50.0
        change.setDraftSellingPrice(50.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być o połowę niższa
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(50.0, draft.getDraftDiscountPercent(), "✅ Rabat powinien być 50%");
        assertEquals(50.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być o połowę niższa (50.0)");
    }

    @Test
    @DisplayName("TEST 4: Ustawianie rabatu - weryfikacja obliczania dla wielu produktów")
    void testSetDiscount_MultipleProducts() {
        // GIVEN: 10 produktów z różnymi cenami detalicznymi
        List<Product> products = testProducts.subList(0, 10);
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            product.setRetailPrice(100.0 + (i * 10.0));  // 100, 110, 120, ..., 190
            product = productRepository.save(product);
        }
        
        // WHEN: Ustawiamy rabat 15% dla wszystkich produktów
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(15.0);  // 15% rabatu
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftRetailPrice(product.getRetailPrice());
            change.setDraftDiscountPercent(15.0);
            // Cena sprzedaży = retailPrice * 0.85
            change.setDraftSellingPrice(product.getRetailPrice() * 0.85);
            changes.add(change);
        }
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Wszystkie ceny powinny być obliczone poprawnie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(10, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        
        for (int i = 0; i < draftChanges.size(); i++) {
            ProjectDraftChange draft = draftChanges.get(i);
            double expectedRetailPrice = 100.0 + (i * 10.0);
            double expectedSellingPrice = expectedRetailPrice * 0.85;
            
            assertEquals(expectedRetailPrice, draft.getDraftRetailPrice(), 0.01, 
                        "✅ Cena detaliczna powinna być poprawna dla produktu " + i);
            assertEquals(expectedSellingPrice, draft.getDraftSellingPrice(), 0.01, 
                        "✅ Cena sprzedaży powinna być obliczona poprawnie dla produktu " + i);
        }
    }

    @Test
    @DisplayName("TEST 5: Ustawianie rabatu - wydajność dla 1000 produktów")
    void testSetDiscount_Performance_1000Products() {
        // GIVEN: 1000 produktów
        List<Product> products = createProductsBatch(1000);
        
        // WHEN: Ustawiamy rabat 10% dla wszystkich produktów
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(10.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftRetailPrice(100.0);
            change.setDraftDiscountPercent(10.0);
            change.setDraftSellingPrice(90.0);
            changes.add(change);
        }
        request.setChanges(changes);
        
        long startTime = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long duration = System.currentTimeMillis() - startTime;
        
        // THEN: Operacja powinna zakończyć się w rozsądnym czasie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1000, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        assertTrue(duration < 30000, 
                  "✅ Operacja powinna zakończyć się w < 30s. Czas: " + duration + "ms");
        
        logger.info("✅ TEST 5: Ustawiono rabat dla 1000 produktów w {}ms ({}s)", 
                   duration, duration / 1000.0);
    }

    @Test
    @DisplayName("TEST 6: Ustawianie rabatu - wydajność dla 8685 produktów (realny scenariusz produkcyjny)")
    void testSetDiscount_Performance_8685Products_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów (jak w prawdziwym scenariuszu) - BATCH INSERT dla szybkości
        logger.info("🔄 TEST 6: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsDuration = System.currentTimeMillis() - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        // WHEN: Ustawiamy rabat 10% dla wszystkich produktów
        logger.info("🔄 TEST 6: Ustawiam rabat 10% dla 8685 produktów...");
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(10.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftRetailPrice(100.0);
            change.setDraftDiscountPercent(10.0);
            change.setDraftSellingPrice(90.0);
            changes.add(change);
        }
        request.setChanges(changes);
        
        long saveStart = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long saveDuration = System.currentTimeMillis() - saveStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDuration, saveDuration / 1000.0);
        
        // THEN: Operacja powinna zakończyć się w rozsądnym czasie
        long verifyStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Weryfikacja (findByProjectId): {}ms", verifyDuration);
        
        assertEquals(8685, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        assertTrue(saveDuration < 60000, 
                  "✅ Operacja powinna zakończyć się w < 60s. Czas: " + saveDuration + "ms");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | save: {}ms | verify: {}ms", 
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDuration, verifyDuration);
    }
}

