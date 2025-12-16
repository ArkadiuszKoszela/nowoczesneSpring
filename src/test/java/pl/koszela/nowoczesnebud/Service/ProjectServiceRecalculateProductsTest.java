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
 * 🎯 TESTY PRZELICZANIA PRODUKTÓW (RECALCULATE)
 * 
 * Testuje funkcjonalność "Przelicz produkty":
 * - Podstawowy scenariusz (tylko quantity)
 * - Quantity = 0
 * - Bardzo duża quantity
 * - Wielokrotne przeliczanie
 */
@DisplayName("Testy przeliczania produktów (recalculate)")
class ProjectServiceRecalculateProductsTest extends BaseProjectServiceTest {

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        setUpBase();
        testProducts = createProductsBatch(100);
    }

    @Test
    @DisplayName("TEST 1: Przelicz produkty - podstawowy scenariusz (tylko quantity)")
    void testRecalculateProducts_Basic() {
        // GIVEN: Mamy zapisane draft changes z quantity
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : testProducts) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(10.0);  // Tylko quantity (bez innych zmian)
            changes.add(change);
        }
        request.setChanges(changes);
        
        // WHEN: Zapisujemy draft changes (to symuluje "Przelicz produkty")
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Wszystkie draft changes zostały zapisane z quantity
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(100, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        
        for (ProjectDraftChange draft : draftChanges) {
            assertEquals(10.0, draft.getDraftQuantity(), 
                         "✅ Quantity powinno być zapisane dla produktu ID: " + draft.getProductId());
        }
    }

    @Test
    @DisplayName("TEST 2: Przelicz produkty - brzegowy przypadek: quantity = 0")
    void testRecalculateProducts_ZeroQuantity() {
        // GIVEN: Draft changes z quantity = 0
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : testProducts) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(0.0);  // Quantity = 0
            changes.add(change);
        }
        request.setChanges(changes);
        
        // WHEN: Zapisujemy draft changes
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Wszystkie draft changes zostały zapisane z quantity = 0
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(100, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        
        for (ProjectDraftChange draft : draftChanges) {
            assertEquals(0.0, draft.getDraftQuantity(), 
                         "✅ Quantity = 0 powinno być zapisane dla produktu ID: " + draft.getProductId());
        }
    }

    @Test
    @DisplayName("TEST 3: Przelicz produkty - brzegowy przypadek: bardzo duża quantity")
    void testRecalculateProducts_LargeQuantity() {
        // GIVEN: Draft changes z bardzo dużą quantity
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : testProducts) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(999999.99);  // Bardzo duża quantity
            changes.add(change);
        }
        request.setChanges(changes);
        
        // WHEN: Zapisujemy draft changes
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Wszystkie draft changes zostały zapisane z dużą quantity
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(100, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        
        for (ProjectDraftChange draft : draftChanges) {
            assertEquals(999999.99, draft.getDraftQuantity(), 0.01, 
                         "✅ Duża quantity powinna być zapisana dla produktu ID: " + draft.getProductId());
        }
    }

    @Test
    @DisplayName("TEST 4: Przelicz produkty - wielokrotne przeliczanie (UPDATE quantity)")
    void testRecalculateProducts_MultipleRecalculations() {
        // GIVEN: Pierwsze przeliczanie z quantity = 10.0
        SaveDraftChangesRequest request1 = new SaveDraftChangesRequest();
        request1.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes1 = new ArrayList<>();
        
        for (Product product : testProducts) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(10.0);
            changes1.add(change);
        }
        request1.setChanges(changes1);
        projectService.saveDraftChanges(testProject.getId(), request1);
        
        // WHEN: Drugie przeliczanie z quantity = 20.0 (powinno zaktualizować istniejące)
        SaveDraftChangesRequest request2 = new SaveDraftChangesRequest();
        request2.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes2 = new ArrayList<>();
        
        for (Product product : testProducts) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(20.0);  // Nowa quantity
            changes2.add(change);
        }
        request2.setChanges(changes2);
        projectService.saveDraftChanges(testProject.getId(), request2);
        
        // THEN: Wszystkie draft changes powinny mieć zaktualizowaną quantity = 20.0
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(100, draftChanges.size(), "✅ Powinno być nadal 100 draft changes (UPDATE, nie duplikaty)");
        
        for (ProjectDraftChange draft : draftChanges) {
            assertEquals(20.0, draft.getDraftQuantity(), 
                         "✅ Quantity powinno być zaktualizowane na 20.0 dla produktu ID: " + draft.getProductId());
        }
    }

    @Test
    @DisplayName("TEST 5: Przelicz produkty - wydajność dla 1000 produktów")
    void testRecalculateProducts_Performance_1000Products() {
        // GIVEN: 1000 produktów
        List<Product> products = createProductsBatch(1000);
        
        // WHEN: Przeliczamy quantity dla wszystkich produktów
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(15.0);
            changes.add(change);
        }
        request.setChanges(changes);
        
        long startTime = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long duration = System.currentTimeMillis() - startTime;
        
        // THEN: Operacja powinna zakończyć się w rozsądnym czasie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1000, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        assertTrue(duration < 10000, 
                  "✅ Operacja powinna zakończyć się w < 10s. Czas: " + duration + "ms");
        
        logger.info("✅ TEST 5: Przeliczono quantity dla 1000 produktów w {}ms ({}s)", 
                   duration, duration / 1000.0);
    }

    @Test
    @DisplayName("TEST 6: Przelicz produkty - wydajność dla 8685 produktów (realny scenariusz produkcyjny)")
    void testRecalculateProducts_Performance_8685Products_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów (jak w prawdziwym scenariuszu) - BATCH INSERT dla szybkości
        logger.info("🔄 TEST 6: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsDuration = System.currentTimeMillis() - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        // WHEN: Przeliczamy quantity dla wszystkich produktów
        logger.info("🔄 TEST 6: Przeliczam quantity dla 8685 produktów...");
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : products) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(15.0);
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

