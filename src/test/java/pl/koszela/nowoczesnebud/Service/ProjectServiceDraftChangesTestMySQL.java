package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.koszela.nowoczesnebud.DTO.DraftChangeDTO;
import pl.koszela.nowoczesnebud.DTO.SaveDraftChangesRequest;
import pl.koszela.nowoczesnebud.DTO.SaveProjectDataRequest;
import pl.koszela.nowoczesnebud.DTO.UpdateGroupOptionBatchRequest;
import pl.koszela.nowoczesnebud.Model.GroupOption;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.PriceChangeSource;
import pl.koszela.nowoczesnebud.Model.ProjectDraftChange;
import pl.koszela.nowoczesnebud.Model.ProjectProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🎯 TESTY DRAFT CHANGES - Duże ilości danych (MySQL)
 * 
 * Testuje zapisywanie draft changes z dużą liczbą danych:
 * - Podstawowy test
 * - 8685 zmian (realny scenariusz produkcyjny)
 * - 2000 zmian (weryfikacja connection z EntityManager)
 * 
 * ⚡ WYDAJNOŚĆ: Każdy test loguje czas wykonania dla identyfikacji wąskich gardeł
 */
@DisplayName("Testy logiki zapisywania draft changes - MySQL (duże ilości danych)")
class ProjectServiceDraftChangesTestMySQL extends BaseProjectServiceTest {

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    // ========== TEST 1: Podstawowy test zapisu draft changes ==========
    @Test
    @DisplayName("TEST 1: Zapisanie draft changes - MySQL")
    void testSaveDraftChanges_Basic() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Brak draft changes
        long checkStart = System.currentTimeMillis();
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED: project_draft_changes_ws - pusta");
        long checkDuration = System.currentTimeMillis() - checkStart;
        logger.info("⏱️ [PERFORMANCE] TEST 1 - Sprawdzenie PRZED: {}ms", checkDuration);

        // WHEN: Zapisujemy draft changes
        SaveDraftChangesRequest request = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 96.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        
        long saveStart = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long saveDuration = System.currentTimeMillis() - saveStart;
        logger.info("⏱️ [PERFORMANCE] TEST 1 - saveDraftChanges: {}ms", saveDuration);

        // THEN: Draft changes zostały zapisane
        long verifyStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 1 - Weryfikacja PO: {}ms", verifyDuration);
        
        assertEquals(1, draftChanges.size(), 
            "✅ PO: project_draft_changes_ws - 1 rekord");
        assertEquals(96.0, draftChanges.get(0).getDraftSellingPrice(), 
            "✅ PO: draftSellingPrice = 96.0");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 1 - CAŁKOWITY CZAS: {}ms (check: {}ms, save: {}ms, verify: {}ms)", 
                   testDuration, checkDuration, saveDuration, verifyDuration);
    }

    // ========== TEST 2: Duża liczba zmian - REALNY SCENARIUSZ ==========
    @Test
    @DisplayName("TEST 2: Duża liczba zmian - 8685 zmian (jak w produkcji) - MySQL")
    void testSaveDraftChanges_LargeBatch_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów (jak w prawdziwym scenariuszu) - BATCH INSERT dla szybkości
        logger.info("🔄 TEST 2: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> testProducts = createProductsBatch(8685);
        long createProductsDuration = System.currentTimeMillis() - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        // WHEN: Zapisujemy 8685 zmian (9 batchy: 8x1000 + 1x685)
        logger.info("🔄 TEST 2: Zapisuję 8685 draft changes...");
        SaveDraftChangesRequest request = createLargeBatchRequest(testProducts, 20.0, 10.0);
        
        long saveStart = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long saveDuration = System.currentTimeMillis() - saveStart;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDuration, saveDuration / 1000.0);

        // THEN: Wszystkie draft changes zostały zapisane
        long verifyStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - Weryfikacja (findByProjectId): {}ms", verifyDuration);
        
        assertEquals(8685, draftChanges.size(), 
            "✅ PO: project_draft_changes_ws - 8685 rekordów (wszystkie zapisane)");
        
        // ✅ SPRAWDZENIE: Operacja nie powinna timeoutować
        assertTrue(saveDuration < 60000, 
            "✅ Operacja powinna zakończyć się w rozsądnym czasie (< 60s). Czas: " + saveDuration + "ms");
        
        // Sprawdź, czy wszystkie produkty są w draft changes
        long checkStart = System.currentTimeMillis();
        Set<Long> savedProductIds = draftChanges.stream()
            .map(ProjectDraftChange::getProductId)
            .collect(Collectors.toSet());
        
        Set<Long> expectedProductIds = testProducts.stream()
            .map(Product::getId)
            .collect(Collectors.toSet());
        
        assertEquals(expectedProductIds.size(), savedProductIds.size(), 
            "✅ Wszystkie produkty powinny być w draft changes");
        assertTrue(savedProductIds.containsAll(expectedProductIds), 
            "✅ Wszystkie productIds powinny być zapisane");
        long checkDuration = System.currentTimeMillis() - checkStart;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - Sprawdzenie productIds: {}ms", checkDuration);
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | save: {}ms | verify: {}ms | check: {}ms", 
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDuration, verifyDuration, checkDuration);
    }

    // ========== TEST 3: Weryfikacja że connection jest z EntityManager ==========
    @Test
    @DisplayName("TEST 3: Weryfikacja że connection jest z EntityManager (nie dataSource) - MySQL")
    void testSaveDraftChanges_ConnectionFromEntityManager() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 2000 produktów (2 batche po 1000) - BATCH INSERT dla szybkości
        logger.info("🔄 TEST 3: Tworzenie 2000 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> testProducts = createProductsBatch(2000);
        long createProductsDuration = System.currentTimeMillis() - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 3 - Utworzenie 2000 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        // WHEN: Zapisujemy 2000 zmian
        SaveDraftChangesRequest request = createLargeBatchRequest(testProducts, 20.0, 10.0);
        
        // ✅ SPRAWDZENIE: Operacja powinna zakończyć się sukcesem (bez timeoutu)
        // Jeśli connection był z dataSource (poza transakcją), mogłyby być problemy z timeoutem
        long saveStart = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long saveDuration = System.currentTimeMillis() - saveStart;
        logger.info("⏱️ [PERFORMANCE] TEST 3 - saveDraftChanges (2000 zmian): {}ms ({}s)", 
                   saveDuration, saveDuration / 1000.0);
        
        // THEN: Wszystkie draft changes zostały zapisane
        long verifyStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 3 - Weryfikacja (findByProjectId): {}ms", verifyDuration);
        
        assertEquals(2000, draftChanges.size(), 
            "✅ PO: project_draft_changes_ws - 2000 rekordów (wszystkie zapisane)");
        
        // ✅ SPRAWDZENIE: Operacja nie powinna trwać zbyt długo
        assertTrue(saveDuration < 30000, 
            "✅ Operacja powinna zakończyć się w rozsądnym czasie (< 30s). Czas: " + saveDuration + "ms");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 3 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | save: {}ms | verify: {}ms", 
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDuration, verifyDuration);
    }

    // ========== TEST 4: Zapisanie projektu - wydajność dla 8685 rekordów (realny scenariusz produkcyjny) ==========
    @Test
    @DisplayName("TEST 4: Zapisanie projektu - wydajność dla 8685 rekordów (realny scenariusz produkcyjny - MySQL)")
    void testSaveProjectData_Performance_8685Records_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów i zapisujemy draft changes
        logger.info("🔄 TEST 4: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 4 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        logger.info("🔄 TEST 4: Zapisuję 8685 draft changes...");
        long saveDraftChangesStart = System.currentTimeMillis();
        SaveDraftChangesRequest draftRequest = createLargeBatchRequest(products, 20.0, 10.0);
        projectService.saveDraftChanges(testProject.getId(), draftRequest);
        long saveDraftChangesEnd = System.currentTimeMillis();
        long saveDraftChangesDuration = saveDraftChangesEnd - saveDraftChangesStart;
        logger.info("⏱️ [PERFORMANCE] TEST 4 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDraftChangesDuration, saveDraftChangesDuration / 1000.0);
        
        // WHEN: Zapisujemy projekt (saveProjectData) - to przenosi draft changes do ProjectProduct
        logger.info("🔄 TEST 4: Zapisuję projekt (saveProjectData) dla 8685 rekordów...");
        long saveProjectDataStart = System.currentTimeMillis();
        SaveProjectDataRequest saveRequest = new SaveProjectDataRequest();
        saveRequest.setTilesMargin(20.0);
        saveRequest.setTilesDiscount(null);
        projectService.saveProjectData(testProject.getId(), saveRequest);
        long saveProjectDataEnd = System.currentTimeMillis();
        long saveProjectDataDuration = saveProjectDataEnd - saveProjectDataStart;
        logger.info("⏱️ [PERFORMANCE] TEST 4 - saveProjectData (8685 rekordów): {}ms ({}s)", 
                   saveProjectDataDuration, saveProjectDataDuration / 1000.0);
        
        // THEN: Wszystkie draft changes zostały przeniesione do ProjectProduct
        long verifyStart = System.currentTimeMillis();
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(),
            "✅ PO saveProjectData: project_draft_changes_ws powinna być pusta (draft changes przeniesione)");
        
        List<ProjectProduct> projectProducts = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(8685, projectProducts.size(),
            "✅ PO saveProjectData: project_products powinna zawierać 8685 rekordów");
        
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 4 - Weryfikacja: {}ms", verifyDuration);
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 4 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveDraftChanges: {}ms | saveProjectData: {}ms | verify: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDraftChangesDuration, 
                   saveProjectDataDuration, verifyDuration);
        
        // ⚠️ WAŻNE: Sprawdź, czy saveProjectData zakończył się w rozsądnym czasie (< 10s dla 8685 rekordów)
        assertTrue(saveProjectDataDuration < 10000,
                  "✅ saveProjectData powinien zakończyć się w < 10s dla 8685 rekordów. Czas: " + saveProjectDataDuration + "ms");
    }

    // ========== TEST 5: Wydajność findByProjectId dla 8685 rekordów ==========
    @Test
    @DisplayName("TEST 5: Wydajność findByProjectId dla 8685 rekordów (realny scenariusz produkcyjny - MySQL)")
    void testFindByProjectId_Performance_8685Records_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów i zapisujemy draft changes
        logger.info("🔄 TEST 5: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 5 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        logger.info("🔄 TEST 5: Zapisuję 8685 draft changes...");
        long saveDraftChangesStart = System.currentTimeMillis();
        SaveDraftChangesRequest draftRequest = createLargeBatchRequest(products, 20.0, 10.0);
        projectService.saveDraftChanges(testProject.getId(), draftRequest);
        long saveDraftChangesEnd = System.currentTimeMillis();
        long saveDraftChangesDuration = saveDraftChangesEnd - saveDraftChangesStart;
        logger.info("⏱️ [PERFORMANCE] TEST 5 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDraftChangesDuration, saveDraftChangesDuration / 1000.0);
        
        // WHEN: Pobieramy draft changes przez findByProjectId (to jest używane w saveProjectData)
        logger.info("🔄 TEST 5: Pobieram 8685 draft changes przez findByProjectId...");
        long findByProjectIdStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long findByProjectIdEnd = System.currentTimeMillis();
        long findByProjectIdDuration = findByProjectIdEnd - findByProjectIdStart;
        logger.info("⏱️ [PERFORMANCE] TEST 5 - findByProjectId (8685 rekordów): {}ms ({}s)", 
                   findByProjectIdDuration, findByProjectIdDuration / 1000.0);
        
        // THEN: Wszystkie rekordy powinny być pobrane
        assertEquals(8685, draftChanges.size(),
            "✅ findByProjectId powinien zwrócić 8685 rekordów");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 5 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveDraftChanges: {}ms | findByProjectId: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDraftChangesDuration, 
                   findByProjectIdDuration);
        
        // ⚠️ WAŻNE: Sprawdź, czy findByProjectId zakończył się w rozsądnym czasie (< 2s dla 8685 rekordów)
        // W produkcji widzieliśmy 798ms-1509ms, więc 2s to bezpieczny limit
        assertTrue(findByProjectIdDuration < 2000,
                  "✅ findByProjectId powinien zakończyć się w < 2s dla 8685 rekordów. Czas: " + findByProjectIdDuration + "ms");
    }
    
    // ========== TEST 6: Batch update opcji grupy - wydajność dla dużej liczby produktów ==========
    @Test
    @DisplayName("TEST 6: Batch update opcji grupy - wydajność dla dużej liczby produktów (MySQL)")
    void testUpdateGroupOptionBatch_Performance_LargeBatch() {
        long testStartTime = System.currentTimeMillis();
        logger.info("🧪 TEST 6: Batch update opcji grupy - wydajność dla dużej liczby produktów");
        
        // 0. Sprawdź, że nie ma draft changes przed testem (dla czystości testu)
        List<ProjectDraftChange> beforeTest = projectDraftChangeRepository.findByProjectIdAndCategory(
            testProject.getId(), "TILE");
        assertEquals(0, beforeTest.size(), "Przed testem nie powinno być draft changes dla kategorii TILE");
        
        // 1. Utwórz dużą liczbę produktów (np. 2000)
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(2000, ProductCategory.TILE);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Utworzenie 2000 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        // 2. Utwórz draft changes z różnymi opcjami
        SaveDraftChangesRequest initialRequest = new SaveDraftChangesRequest();
        initialRequest.setCategory("TILE");
        List<DraftChangeDTO> initialChanges = new ArrayList<>();
        for (Product product : products) {
            DraftChangeDTO dto = new DraftChangeDTO(product.getId(), "TILE");
            dto.setDraftRetailPrice(100.0);
            dto.setDraftPurchasePrice(80.0);
            dto.setDraftSellingPrice(120.0);
            dto.setDraftQuantity(10.0);
            dto.setDraftIsMainOption(GroupOption.NONE);
            initialChanges.add(dto);
        }
        initialRequest.setChanges(initialChanges);
        
        long saveInitialStart = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), initialRequest);
        long saveInitialEnd = System.currentTimeMillis();
        long saveInitialDuration = saveInitialEnd - saveInitialStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Zapisanie początkowych draft changes (2000): {}ms ({}s)", 
                   saveInitialDuration, saveInitialDuration / 1000.0);
        
        // 3. Batch update opcji grupy na OPTIONAL
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        UpdateGroupOptionBatchRequest batchRequest = new UpdateGroupOptionBatchRequest(
            "TILE",
            productIds,
            GroupOption.OPTIONAL
        );
        
        long updateBatchStart = System.currentTimeMillis();
        projectService.updateGroupOptionBatch(testProject.getId(), batchRequest);
        long updateBatchEnd = System.currentTimeMillis();
        long updateBatchDuration = updateBatchEnd - updateBatchStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Batch update opcji grupy (2000 produktów): {}ms ({}s)", 
                   updateBatchDuration, updateBatchDuration / 1000.0);
        
        // 4. Sprawdź poprawność - używaj findByProjectIdAndCategory zamiast findByProjectId
        long verifyStart = System.currentTimeMillis();
        List<ProjectDraftChange> afterUpdate = projectDraftChangeRepository.findByProjectIdAndCategory(
            testProject.getId(), "TILE");
        long verifyEnd = System.currentTimeMillis();
        long verifyDuration = verifyEnd - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - Weryfikacja (findByProjectIdAndCategory): {}ms", verifyDuration);
        
        assertEquals(2000, afterUpdate.size(), "Powinno być 2000 draft changes dla kategorii TILE");
        for (ProjectDraftChange dc : afterUpdate) {
            assertEquals("TILE", dc.getCategory(), "Kategoria powinna być TILE");
            assertEquals(GroupOption.OPTIONAL, dc.getDraftIsMainOption(), "Opcja powinna być OPTIONAL");
            // ⚠️ WAŻNE: Inne pola NIE powinny być zmienione
            assertNotNull(dc.getDraftRetailPrice(), "Cena powinna pozostać");
            assertEquals(100.0, dc.getDraftRetailPrice(), 0.01, "Cena powinna być taka sama (100.0)");
        }
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 6 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveInitial: {}ms | updateBatch: {}ms | verify: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveInitialDuration, updateBatchDuration, verifyDuration);
        
        // ⚠️ WAŻNE: Batch update powinien być szybki (< 3s dla 2000 produktów)
        assertTrue(updateBatchDuration < 3000,
                  "✅ Batch update powinien zakończyć się w < 3s dla 2000 produktów. Czas: " + updateBatchDuration + "ms");
    }
}

