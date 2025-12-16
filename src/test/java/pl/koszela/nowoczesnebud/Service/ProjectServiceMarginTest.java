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
 * 🎯 TESTY USTAWIANIA MARŻY (MARGIN) - Poprawność obliczania
 * 
 * Testuje ustawianie marży dla produktów:
 * - Podstawowy scenariusz (20% marży)
 * - Marża = 0%
 * - Bardzo duża marża (100%)
 * - Weryfikacja obliczania dla wielu produktów
 * - Testy wydajnościowe
 */
@DisplayName("Testy ustawiania marży (margin) - poprawność obliczania")
class ProjectServiceMarginTest extends BaseProjectServiceTest {

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        setUpBase();
        testProducts = createProductsBatch(100);
    }

    @Test
    @DisplayName("TEST 1: Ustawianie marży - podstawowy scenariusz (20% marży)")
    void testSetMargin_Basic() {
        // GIVEN: Produkty z ceną zakupu 100.0
        Product product = testProducts.get(0);
        product.setPurchasePrice(100.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy marżę 20% dla kategorii
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(20.0);  // 20% marży
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftPurchasePrice(100.0);
        change.setDraftMarginPercent(20.0);
        // Cena sprzedaży = 100.0 * (1 + 20/100) = 100.0 * 1.2 = 120.0
        change.setDraftSellingPrice(120.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być obliczona poprawnie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1, draftChanges.size(), "✅ Draft change powinien być zapisany");
        
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(20.0, draft.getDraftMarginPercent(), "✅ Marża powinna być zapisana");
        assertEquals(120.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być 120.0 (100.0 * 1.2)");
    }

    @Test
    @DisplayName("TEST 2: Ustawianie marży - brzegowy przypadek: marża = 0%")
    void testSetMargin_ZeroMargin() {
        // GIVEN: Produkty z ceną zakupu 100.0
        Product product = testProducts.get(0);
        product.setPurchasePrice(100.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy marżę 0%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(0.0);  // 0% marży
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftPurchasePrice(100.0);
        change.setDraftMarginPercent(0.0);
        // Cena sprzedaży = 100.0 * (1 + 0/100) = 100.0 * 1.0 = 100.0
        change.setDraftSellingPrice(100.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być równa cenie zakupu
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(0.0, draft.getDraftMarginPercent(), "✅ Marża powinna być 0%");
        assertEquals(100.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być równa cenie zakupu (100.0)");
    }

    @Test
    @DisplayName("TEST 3: Ustawianie marży - brzegowy przypadek: bardzo duża marża (100%)")
    void testSetMargin_LargeMargin() {
        // GIVEN: Produkty z ceną zakupu 100.0
        Product product = testProducts.get(0);
        product.setPurchasePrice(100.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy marżę 100%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(100.0);  // 100% marży
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftPurchasePrice(100.0);
        change.setDraftMarginPercent(100.0);
        // Cena sprzedaży = 100.0 * (1 + 100/100) = 100.0 * 2.0 = 200.0
        change.setDraftSellingPrice(200.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być podwojona
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(100.0, draft.getDraftMarginPercent(), "✅ Marża powinna być 100%");
        assertEquals(200.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być podwojona (200.0)");
    }

    @Test
    @DisplayName("TEST 4: Ustawianie marży - weryfikacja obliczania dla wielu produktów")
    void testSetMargin_MultipleProducts() {
        // GIVEN: 10 produktów z różnymi cenami zakupu
        List<Product> products = testProducts.subList(0, 10);
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            product.setPurchasePrice(100.0 + (i * 10.0));  // 100, 110, 120, ..., 190
            product = productRepository.save(product);
        }
        
        // WHEN: Ustawiamy marżę 25% dla wszystkich produktów
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(25.0);  // 25% marży
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftPurchasePrice(product.getPurchasePrice());
            change.setDraftMarginPercent(25.0);
            // Cena sprzedaży = purchasePrice * 1.25
            change.setDraftSellingPrice(product.getPurchasePrice() * 1.25);
            changes.add(change);
        }
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Wszystkie ceny powinny być obliczone poprawnie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(10, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        
        for (int i = 0; i < draftChanges.size(); i++) {
            ProjectDraftChange draft = draftChanges.get(i);
            double expectedPurchasePrice = 100.0 + (i * 10.0);
            double expectedSellingPrice = expectedPurchasePrice * 1.25;
            
            assertEquals(expectedPurchasePrice, draft.getDraftPurchasePrice(), 0.01, 
                        "✅ Cena zakupu powinna być poprawna dla produktu " + i);
            assertEquals(expectedSellingPrice, draft.getDraftSellingPrice(), 0.01, 
                        "✅ Cena sprzedaży powinna być obliczona poprawnie dla produktu " + i);
        }
    }

    @Test
    @DisplayName("TEST 5: Ustawianie marży - wydajność dla 1000 produktów")
    void testSetMargin_Performance_1000Products() {
        // GIVEN: 1000 produktów
        List<Product> products = createProductsBatch(1000);
        
        // WHEN: Ustawiamy marżę 20% dla wszystkich produktów
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(20.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftPurchasePrice(100.0);
            change.setDraftMarginPercent(20.0);
            change.setDraftSellingPrice(120.0);
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
        
        logger.info("✅ TEST 5: Ustawiono marżę dla 1000 produktów w {}ms ({}s)", 
                   duration, duration / 1000.0);
    }

    @Test
    @DisplayName("TEST 6: Ustawianie marży - wydajność dla 8685 produktów (realny scenariusz produkcyjny)")
    void testSetMargin_Performance_8685Products_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów (jak w prawdziwym scenariuszu) - BATCH INSERT dla szybkości
        logger.info("🔄 TEST 6: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsDuration = System.currentTimeMillis() - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        // WHEN: Ustawiamy marżę 20% dla wszystkich produktów
        logger.info("🔄 TEST 6: Ustawiam marżę 20% dla 8685 produktów...");
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(20.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftPurchasePrice(100.0);
            change.setDraftMarginPercent(20.0);
            change.setDraftSellingPrice(120.0);
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

