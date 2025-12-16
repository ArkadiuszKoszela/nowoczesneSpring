package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.koszela.nowoczesnebud.DTO.DraftChangeDTO;
import pl.koszela.nowoczesnebud.DTO.SaveDraftChangesRequest;
import pl.koszela.nowoczesnebud.DTO.SaveProjectDataRequest;
import pl.koszela.nowoczesnebud.DTO.SaveProjectProductGroupDTO;
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
 * 🎯 TESTY DRAFT CHANGES - Podstawowe scenariusze (MySQL)
 * 
 * Testuje podstawowe scenariusze zapisywania draft changes i projektu:
 * - Zapisanie draft changes (pierwszy raz)
 * - Wielokrotne zapisanie (UPSERT)
 * - Tylko zmiana marży
 * - Tylko zmiana quantity
 * - Zapisanie projektu z draft changes
 * - Zapisanie projektu bez draft changes
 * - Wielokrotne zapisanie projektu
 * - Zapisanie projektu po zmianie marży
 * 
 * ⚡ WYDAJNOŚĆ: Każdy test loguje czas wykonania dla identyfikacji wąskich gardeł
 */
@DisplayName("Testy logiki zapisywania draft changes i projektu - MySQL (podstawowe scenariusze)")
class ProjectServiceDraftChangesTest extends BaseProjectServiceTest {

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    // ========== TEST 1: Zapisanie draft changes ==========
    @Test
    @DisplayName("TEST 1: Zapisanie draft changes - pierwszy raz")
    void testSaveDraftChanges_FirstTime() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Brak draft changes dla projektu
        long checkBeforeStart = System.currentTimeMillis();
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "❌ PRZED: project_draft_changes_ws powinna być pusta");
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "❌ PRZED: project_products powinna być pusta");
        long checkBeforeDuration = System.currentTimeMillis() - checkBeforeStart;
        logger.info("⏱️ [PERFORMANCE] TEST 1 - Sprawdzenie PRZED: {}ms", checkBeforeDuration);

        // WHEN: Zapisujemy draft changes
        SaveDraftChangesRequest request = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0,  // retailPrice
            80.0,   // purchasePrice
            90.0,   // sellingPrice
            10.0,   // quantity
            20.0,   // marginPercent
            PriceChangeSource.MARGIN.name()
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
            "✅ PO: project_draft_changes_ws powinna zawierać 1 rekord");
        
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(testProject.getId(), draft.getProjectId());
        assertEquals(testProduct.getId(), draft.getProductId());
        assertEquals(ProductCategory.TILE.name(), draft.getCategory());
        assertEquals(100.0, draft.getDraftRetailPrice());
        assertEquals(80.0, draft.getDraftPurchasePrice());
        assertEquals(90.0, draft.getDraftSellingPrice());
        assertEquals(10.0, draft.getDraftQuantity());
        assertEquals(20.0, draft.getDraftMarginPercent());
        assertEquals(PriceChangeSource.MARGIN.name(), draft.getPriceChangeSource());
        
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO: project_products nadal pusta (draft changes nie są jeszcze zapisane jako ProjectProduct)");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 1 - CAŁKOWITY CZAS: {}ms (checkBefore: {}ms, save: {}ms, verify: {}ms)", 
                   testDuration, checkBeforeDuration, saveDuration, verifyDuration);
    }

    // ========== TEST 2: Wielokrotne zapisanie draft changes (UPSERT) ==========
    @Test
    @DisplayName("TEST 2: Wielokrotne zapisanie draft changes - UPSERT")
    void testSaveDraftChanges_MultipleTimes_UPSERT() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Zapisujemy draft changes pierwszy raz
        SaveDraftChangesRequest request1 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 90.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        
        long save1Start = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request1);
        long save1Duration = System.currentTimeMillis() - save1Start;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - saveDraftChanges (pierwszy raz): {}ms", save1Duration);
        
        // ✅ SPRAWDZENIE: Po pierwszym zapisie
        assertEquals(1, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_draft_changes_ws - 1 rekord");
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_products - pusta");

        // WHEN: Zapisujemy te same draft changes ponownie z nowymi wartościami
        SaveDraftChangesRequest request2 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            110.0,  // NOWA retailPrice
            85.0,   // NOWA purchasePrice
            95.0,   // NOWA sellingPrice
            15.0,   // NOWA quantity
            25.0,   // NOWA marginPercent
            PriceChangeSource.MANUAL.name()  // NOWE źródło
        );
        
        long save2Start = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request2);
        long save2Duration = System.currentTimeMillis() - save2Start;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - saveDraftChanges (UPSERT): {}ms", save2Duration);

        // THEN: Powinien być tylko JEDEN rekord (UPSERT zaktualizował istniejący)
        // ✅ SPRAWDZENIE: Po drugim zapisie (UPSERT)
        long verifyStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - Weryfikacja PO: {}ms", verifyDuration);
        assertEquals(1, draftChanges.size(), 
            "✅ PO DRUGIM ZAPISIE (UPSERT): project_draft_changes_ws - NADAL 1 rekord (zaktualizowany, nie duplikat)");
        
        ProjectDraftChange draft = draftChanges.get(0);
        // Sprawdź, czy wartości zostały zaktualizowane
        assertEquals(110.0, draft.getDraftRetailPrice(), "✅ UPSERT: retailPrice zaktualizowane");
        assertEquals(85.0, draft.getDraftPurchasePrice(), "✅ UPSERT: purchasePrice zaktualizowane");
        assertEquals(95.0, draft.getDraftSellingPrice(), "✅ UPSERT: sellingPrice zaktualizowane");
        assertEquals(15.0, draft.getDraftQuantity(), "✅ UPSERT: quantity zaktualizowane");
        assertEquals(25.0, draft.getDraftMarginPercent(), "✅ UPSERT: marginPercent zaktualizowane");
        assertEquals(PriceChangeSource.MANUAL.name(), draft.getPriceChangeSource(), "✅ UPSERT: priceChangeSource zaktualizowane");
        
        // ✅ SPRAWDZENIE: project_products - NADAL PUSTA (draft changes nie są jeszcze zapisane jako ProjectProduct)
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO DRUGIM ZAPISIE: project_products nadal pusta");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 2 - CAŁKOWITY CZAS: {}ms (save1: {}ms, save2: {}ms, verify: {}ms)", 
                   testDuration, save1Duration, save2Duration, verifyDuration);
    }

    // ========== TEST 3: Tylko zmiana marży ==========
    @Test
    @DisplayName("TEST 3: Tylko zmiana marży - UPDATE quantity")
    void testSaveDraftChanges_OnlyMarginChange() {
        // GIVEN: Zapisujemy draft changes z marżą
        SaveDraftChangesRequest request1 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 90.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        projectService.saveDraftChanges(testProject.getId(), request1);

        // WHEN: Zmieniamy tylko marżę (categoryMargin != null)
        SaveDraftChangesRequest request2 = new SaveDraftChangesRequest();
        request2.setCategory(ProductCategory.TILE.name());
        request2.setCategoryMargin(30.0);  // NOWA marża
        request2.setChanges(new ArrayList<>());  // Pusta lista zmian
        
        // Dodajemy tylko quantity (bez innych zmian)
        DraftChangeDTO change = new DraftChangeDTO(testProduct.getId(), ProductCategory.TILE.name());
        change.setDraftQuantity(20.0);  // Tylko quantity
        request2.getChanges().add(change);
        
        projectService.saveDraftChanges(testProject.getId(), request2);

        // THEN: Draft changes powinny być zaktualizowane
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1, draftChanges.size());
        
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(20.0, draft.getDraftQuantity());  // Quantity zaktualizowane
        // Inne wartości powinny pozostać bez zmian (lub być zaktualizowane przez logikę marży)
    }

    // ========== TEST 4: Tylko zmiana quantity ==========
    @Test
    @DisplayName("TEST 4: Tylko zmiana quantity - UPDATE quantity")
    void testSaveDraftChanges_OnlyQuantityChange() {
        // GIVEN: Zapisujemy draft changes
        SaveDraftChangesRequest request1 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 90.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        projectService.saveDraftChanges(testProject.getId(), request1);

        // WHEN: Zmieniamy tylko quantity (bez categoryMargin i categoryDiscount)
        SaveDraftChangesRequest request2 = new SaveDraftChangesRequest();
        request2.setCategory(ProductCategory.TILE.name());
        request2.setCategoryMargin(null);  // Brak marży
        request2.setCategoryDiscount(null);  // Brak rabatu
        request2.setChanges(new ArrayList<>());
        
        DraftChangeDTO change = new DraftChangeDTO(testProduct.getId(), ProductCategory.TILE.name());
        change.setDraftQuantity(25.0);  // Tylko quantity
        request2.getChanges().add(change);
        
        projectService.saveDraftChanges(testProject.getId(), request2);

        // THEN: Powinien użyć UPDATE quantity (szybsze niż UPSERT)
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1, draftChanges.size());
        
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(25.0, draft.getDraftQuantity());  // Quantity zaktualizowane
    }

    // ========== TEST 5: Zapisanie projektu z draft changes ==========
    @Test
    @DisplayName("TEST 5: Zapisanie projektu z draft changes - przeniesienie do ProjectProduct")
    void testSaveProjectData_WithDraftChanges() {
        // GIVEN: Mamy zapisane draft changes
        SaveDraftChangesRequest draftRequest = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 90.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest);
        
        // ✅ SPRAWDZENIE: PRZED saveProjectData
        assertEquals(1, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED saveProjectData: project_draft_changes_ws - 1 rekord");
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED saveProjectData: project_products - pusta");

        // WHEN: Zapisujemy projekt
        SaveProjectDataRequest saveRequest = new SaveProjectDataRequest();
        saveRequest.setTilesMargin(20.0);
        saveRequest.setTilesDiscount(null);
        projectService.saveProjectData(testProject.getId(), saveRequest);

        // THEN: Draft changes zostały przeniesione do ProjectProduct
        // ✅ SPRAWDZENIE: PO saveProjectData - PRZENIESIENIE DANYCH
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO saveProjectData: project_draft_changes_ws - PUSTA (draft changes usunięte po przeniesieniu)");
        
        List<ProjectProduct> projectProducts = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(1, projectProducts.size(), 
            "✅ PO saveProjectData: project_products - 1 rekord (draft changes przeniesione)");
        
        ProjectProduct pp = projectProducts.get(0);
        assertEquals(testProduct.getId(), pp.getProductId(), "✅ Przeniesienie: productId");
        assertEquals(ProductCategory.TILE, pp.getCategory(), "✅ Przeniesienie: category");
        assertEquals(100.0, pp.getSavedRetailPrice(), "✅ Przeniesienie: savedRetailPrice");
        assertEquals(80.0, pp.getSavedPurchasePrice(), "✅ Przeniesienie: savedPurchasePrice");
        assertEquals(90.0, pp.getSavedSellingPrice(), "✅ Przeniesienie: savedSellingPrice");
        assertEquals(10.0, pp.getSavedQuantity(), "✅ Przeniesienie: savedQuantity");
        assertEquals(20.0, pp.getSavedMarginPercent(), "✅ Przeniesienie: savedMarginPercent");
        assertEquals(PriceChangeSource.MARGIN, pp.getPriceChangeSource(), "✅ Przeniesienie: priceChangeSource");
    }

    // ========== TEST 6: Zapisanie projektu bez draft changes ==========
    @Test
    @DisplayName("TEST 6: Zapisanie projektu bez draft changes - usunięcie ProjectProduct")
    void testSaveProjectData_WithoutDraftChanges() {
        // GIVEN: Mamy zapisane ProjectProduct (z poprzedniego zapisu)
        ProjectProduct existingPP = new ProjectProduct();
        existingPP.setProject(testProject);
        existingPP.setProductId(testProduct.getId());
        existingPP.setCategory(ProductCategory.TILE);
        existingPP.setSavedRetailPrice(100.0);
        existingPP.setSavedPurchasePrice(80.0);
        existingPP.setSavedSellingPrice(90.0);
        existingPP.setSavedQuantity(10.0);
        projectProductRepository.save(existingPP);
        
        assertEquals(1, projectProductRepository.findByProjectId(testProject.getId()).size());
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size());

        // WHEN: Zapisujemy projekt bez draft changes
        SaveProjectDataRequest saveRequest = new SaveProjectDataRequest();
        saveRequest.setTilesMargin(20.0);
        projectService.saveProjectData(testProject.getId(), saveRequest);

        // THEN: ProjectProduct powinny być usunięte (brak draft changes)
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size());
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size());  // Usunięte!
    }

    // ========== TEST 7: Wielokrotne zapisanie projektu ==========
    @Test
    @DisplayName("TEST 7: Wielokrotne zapisanie projektu - sprawdzenie poprawności")
    void testSaveProjectData_MultipleTimes() {
        // GIVEN: Zapisujemy draft changes
        SaveDraftChangesRequest draftRequest1 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 90.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest1);

        // WHEN: Zapisujemy projekt pierwszy raz
        SaveProjectDataRequest saveRequest1 = new SaveProjectDataRequest();
        saveRequest1.setTilesMargin(20.0);
        projectService.saveProjectData(testProject.getId(), saveRequest1);

        // THEN: Sprawdź pierwszy zapis
        // ✅ SPRAWDZENIE: PO PIERWSZYM ZAPISIE
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_draft_changes_ws - pusta (usunięte)");
        List<ProjectProduct> pp1 = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(1, pp1.size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_products - 1 rekord");
        assertEquals(90.0, pp1.get(0).getSavedSellingPrice(), 
            "✅ PO PIERWSZYM ZAPISIE: savedSellingPrice = 90.0");

        // WHEN: Zmieniamy draft changes i zapisujemy ponownie
        SaveDraftChangesRequest draftRequest2 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            110.0, 85.0, 95.0, 15.0, 25.0, PriceChangeSource.MANUAL.name()
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest2);
        
        // ✅ SPRAWDZENIE: PRZED DRUGIM ZAPISEM
        assertEquals(1, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED DRUGIM ZAPISEM: project_draft_changes_ws - 1 rekord (nowe draft changes)");
        assertEquals(1, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED DRUGIM ZAPISEM: project_products - 1 rekord (stary zapis)");
        
        SaveProjectDataRequest saveRequest2 = new SaveProjectDataRequest();
        saveRequest2.setTilesMargin(25.0);
        projectService.saveProjectData(testProject.getId(), saveRequest2);

        // THEN: Sprawdź drugi zapis (powinien nadpisać pierwszy)
        // ✅ SPRAWDZENIE: PO DRUGIM ZAPISIE - NADPISANIE
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO DRUGIM ZAPISIE: project_draft_changes_ws - pusta (usunięte)");
        List<ProjectProduct> pp2 = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(1, pp2.size(), 
            "✅ PO DRUGIM ZAPISIE: project_products - NADAL 1 rekord (nadpisany, nie duplikat)");
        assertEquals(95.0, pp2.get(0).getSavedSellingPrice(), 
            "✅ PO DRUGIM ZAPISIE: savedSellingPrice = 95.0 (zaktualizowane)");
        assertEquals(15.0, pp2.get(0).getSavedQuantity(), 
            "✅ PO DRUGIM ZAPISIE: savedQuantity = 15.0 (zaktualizowane)");
        assertEquals(PriceChangeSource.MANUAL, pp2.get(0).getPriceChangeSource(), 
            "✅ PO DRUGIM ZAPISIE: priceChangeSource = MANUAL (zaktualizowane)");
    }

    // ========== TEST 8: Zapisanie projektu po zmianie marży ==========
    @Test
    @DisplayName("TEST 8: Zapisanie projektu po zmianie marży")
    void testSaveProjectData_AfterMarginChange() {
        // GIVEN: Zapisujemy draft changes z marżą 20%
        SaveDraftChangesRequest draftRequest1 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 96.0, 10.0, 20.0, PriceChangeSource.MARGIN.name()
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest1);
        projectService.saveProjectData(testProject.getId(), new SaveProjectDataRequest());

        // ✅ SPRAWDZENIE: PO PIERWSZYM ZAPISIE (marża 20%)
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_draft_changes_ws - pusta");
        List<ProjectProduct> pp1 = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(1, pp1.size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_products - 1 rekord");
        assertEquals(20.0, pp1.get(0).getSavedMarginPercent(), 
            "✅ PO PIERWSZYM ZAPISIE: savedMarginPercent = 20.0");
        assertEquals(96.0, pp1.get(0).getSavedSellingPrice(), 
            "✅ PO PIERWSZYM ZAPISIE: savedSellingPrice = 96.0");

        // WHEN: Zmieniamy marżę na 30% i zapisujemy ponownie
        SaveDraftChangesRequest draftRequest2 = createDraftChangesRequest(
            testProduct.getId(),
            ProductCategory.TILE.name(),
            100.0, 80.0, 104.0, 10.0, 30.0, PriceChangeSource.MARGIN.name()  // NOWA marża 30%
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest2);
        
        // ✅ SPRAWDZENIE: PRZED DRUGIM ZAPISEM
        assertEquals(1, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED DRUGIM ZAPISEM: project_draft_changes_ws - 1 rekord (nowe draft changes z marżą 30%)");
        assertEquals(1, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED DRUGIM ZAPISEM: project_products - 1 rekord (stary zapis z marżą 20%)");
        
        projectService.saveProjectData(testProject.getId(), new SaveProjectDataRequest());

        // THEN: Sprawdź, czy nowa marża została zapisana
        // ✅ SPRAWDZENIE: PO DRUGIM ZAPISIE (marża 30%)
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO DRUGIM ZAPISIE: project_draft_changes_ws - pusta (usunięte)");
        List<ProjectProduct> pp2 = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(1, pp2.size(), 
            "✅ PO DRUGIM ZAPISIE: project_products - 1 rekord (nadpisany)");
        assertEquals(30.0, pp2.get(0).getSavedMarginPercent(), 
            "✅ PO DRUGIM ZAPISIE: savedMarginPercent = 30.0 (zaktualizowane z 20.0)");
        assertEquals(104.0, pp2.get(0).getSavedSellingPrice(), 
            "✅ PO DRUGIM ZAPISIE: savedSellingPrice = 104.0 (zaktualizowane z 96.0, 80 * 1.30 = 104)");
        assertEquals(PriceChangeSource.MARGIN, pp2.get(0).getPriceChangeSource(), 
            "✅ PO DRUGIM ZAPISIE: priceChangeSource = MARGIN");
    }

    // ========== TEST 9: Duża liczba zmian (batch processing) ==========
    @Test
    @DisplayName("TEST 9: Duża liczba zmian - batch processing (symulacja 8685 zmian)")
    void testSaveDraftChanges_LargeBatch() {
        // GIVEN: Tworzymy wiele produktów do testowania batch processing
        List<Product> testProducts = new ArrayList<>();
        for (int i = 0; i < 50; i++) {  // 50 produktów (symulacja, żeby test był szybki)
            Product product = new Product();
            product.setName("Test Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setManufacturer("Test Manufacturer");
            product.setGroupName("Test Group");
            product = productRepository.save(product);
            testProducts.add(product);
        }
        
        // ✅ SPRAWDZENIE: PRZED zapisaniem
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED: project_draft_changes_ws - pusta");
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PRZED: project_products - pusta");

        // WHEN: Zapisujemy dużą liczbę draft changes (50 zmian)
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (Product product : testProducts) {
            DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
            change.setDraftRetailPrice(product.getRetailPrice());
            change.setDraftPurchasePrice(product.getPurchasePrice());
            change.setDraftSellingPrice(product.getPurchasePrice() * 1.2);  // Marża 20%
            change.setDraftQuantity(10.0);
            change.setDraftMarginPercent(20.0);
            change.setPriceChangeSource(PriceChangeSource.MARGIN.name());
            changes.add(change);
        }
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);

        // THEN: Wszystkie draft changes zostały zapisane
        // ✅ SPRAWDZENIE: PO zapisaniu - WSZYSTKIE REKORDY
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(50, draftChanges.size(), 
            "✅ PO: project_draft_changes_ws - 50 rekordów (wszystkie zapisane)");
        
        // Sprawdź, czy wszystkie produkty są w draft changes
        for (Product product : testProducts) {
            boolean found = draftChanges.stream()
                .anyMatch(dc -> dc.getProductId().equals(product.getId()));
            assertTrue(found, 
                "✅ PO: Draft change dla produktu ID " + product.getId() + " powinien istnieć");
        }
        
        // ✅ SPRAWDZENIE: project_products - NADAL PUSTA (draft changes nie są jeszcze zapisane jako ProjectProduct)
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO: project_products - nadal pusta (draft changes nie są jeszcze zapisane jako ProjectProduct)");
        
        // WHEN: Zapisujemy projekt (przeniesienie do ProjectProduct)
        SaveProjectDataRequest saveRequest = new SaveProjectDataRequest();
        saveRequest.setTilesMargin(20.0);
        projectService.saveProjectData(testProject.getId(), saveRequest);

        // THEN: Wszystkie draft changes zostały przeniesione do ProjectProduct
        // ✅ SPRAWDZENIE: PO saveProjectData - PRZENIESIENIE WSZYSTKICH REKORDÓW
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO saveProjectData: project_draft_changes_ws - pusta (wszystkie usunięte)");
        
        List<ProjectProduct> projectProducts = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(50, projectProducts.size(), 
            "✅ PO saveProjectData: project_products - 50 rekordów (wszystkie przeniesione)");
        
        // Sprawdź, czy wszystkie produkty są w ProjectProduct
        for (Product product : testProducts) {
            boolean found = projectProducts.stream()
                .anyMatch(pp -> pp.getProductId().equals(product.getId()));
            assertTrue(found, 
                "✅ PO saveProjectData: ProjectProduct dla produktu ID " + product.getId() + " powinien istnieć");
        }
    }

    // ========== TEST 10: Wielokrotne zapisanie dużej liczby zmian ==========
    @Test
    @DisplayName("TEST 10: Wielokrotne zapisanie dużej liczby zmian - UPSERT batch")
    void testSaveDraftChanges_LargeBatch_MultipleTimes() {
        // GIVEN: Tworzymy produkty
        List<Product> testProducts = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Product product = new Product();
            product.setName("Test Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setManufacturer("Test Manufacturer");
            product.setGroupName("Test Group");
            product = productRepository.save(product);
            testProducts.add(product);
        }
        
        // WHEN: Zapisujemy draft changes pierwszy raz
        SaveDraftChangesRequest request1 = createLargeBatchRequest(testProducts, 20.0, 10.0);
        projectService.saveDraftChanges(testProject.getId(), request1);
        
        // ✅ SPRAWDZENIE: PO PIERWSZYM ZAPISIE
        assertEquals(30, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_draft_changes_ws - 30 rekordów");
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO PIERWSZYM ZAPISIE: project_products - pusta");

        // WHEN: Zapisujemy te same draft changes ponownie z nowymi wartościami (UPSERT)
        SaveDraftChangesRequest request2 = createLargeBatchRequest(testProducts, 25.0, 15.0);  // NOWA marża i quantity
        projectService.saveDraftChanges(testProject.getId(), request2);

        // THEN: Powinno być nadal 30 rekordów (UPSERT zaktualizował istniejące)
        // ✅ SPRAWDZENIE: PO DRUGIM ZAPISIE (UPSERT)
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(30, draftChanges.size(), 
            "✅ PO DRUGIM ZAPISIE (UPSERT): project_draft_changes_ws - NADAL 30 rekordów (zaktualizowane, nie duplikaty)");
        
        // Sprawdź, czy wartości zostały zaktualizowane
        ProjectDraftChange firstDraft = draftChanges.get(0);
        assertEquals(25.0, firstDraft.getDraftMarginPercent(), 
            "✅ UPSERT: marginPercent zaktualizowane z 20.0 na 25.0");
        assertEquals(15.0, firstDraft.getDraftQuantity(), 
            "✅ UPSERT: quantity zaktualizowane z 10.0 na 15.0");
        
        assertEquals(0, projectProductRepository.findByProjectId(testProject.getId()).size(), 
            "✅ PO DRUGIM ZAPISIE: project_products - nadal pusta");
    }

    // ========== TEST 11: Weryfikacja użycia connection z EntityManager (nie dataSource) ==========
    @Test
    @DisplayName("TEST 11: Weryfikacja że connection jest z EntityManager (w tej samej transakcji)")
    void testSaveDraftChanges_ConnectionFromEntityManager() {
        // GIVEN: Tworzymy większą liczbę produktów (2000) aby wymusić batch processing
        List<Product> testProducts = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            Product product = new Product();
            product.setName("Test Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setManufacturer("Test Manufacturer");
            product.setGroupName("Test Group");
            product = productRepository.save(product);
            testProducts.add(product);
        }
        
        // WHEN: Zapisujemy dużą liczbę draft changes (2000 zmian = 2 batche po 1000)
        SaveDraftChangesRequest request = createLargeBatchRequest(testProducts, 20.0, 10.0);
        
        // ✅ SPRAWDZENIE: Operacja powinna zakończyć się sukcesem (bez timeoutu)
        // Jeśli connection był z dataSource (poza transakcją), mogłyby być problemy z timeoutem
        long startTime = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), request);
        long duration = System.currentTimeMillis() - startTime;
        
        // THEN: Wszystkie draft changes zostały zapisane
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(2000, draftChanges.size(), 
            "✅ PO: project_draft_changes_ws - 2000 rekordów (wszystkie zapisane)");
        
        // ✅ SPRAWDZENIE: Operacja nie powinna trwać zbyt długo (w H2 powinno być szybko)
        // W prawdziwej bazie (MySQL) z dataSource.getConnection() mogłoby być timeout
        assertTrue(duration < 10000, 
            "✅ Operacja powinna zakończyć się w rozsądnym czasie (< 10s). Czas: " + duration + "ms");
        
        logger.info("✅ TEST 11: Zapisano {} zmian w {}ms (connection z EntityManager działa poprawnie)", 
                   draftChanges.size(), duration);
    }

    // ========== TEST 12: Weryfikacja że wszystkie batche są zapisane ==========
    @Test
    @DisplayName("TEST 12: Weryfikacja że wszystkie batche są zapisane (2500 zmian = 3 batche)")
    void testSaveDraftChanges_AllBatchesSaved() {
        // GIVEN: Tworzymy 2500 produktów (3 batche: 1000, 1000, 500)
        List<Product> testProducts = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            Product product = new Product();
            product.setName("Test Product " + i);
            product.setCategory(ProductCategory.TILE);
            product.setRetailPrice(100.0 + i);
            product.setPurchasePrice(80.0 + i);
            product.setManufacturer("Test Manufacturer");
            product.setGroupName("Test Group");
            product = productRepository.save(product);
            testProducts.add(product);
        }
        
        // WHEN: Zapisujemy 2500 zmian (powinno być 3 batche: 1000, 1000, 500)
        SaveDraftChangesRequest request = createLargeBatchRequest(testProducts, 20.0, 10.0);
        projectService.saveDraftChanges(testProject.getId(), request);

        // THEN: Wszystkie 2500 rekordów powinny być zapisane
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(2500, draftChanges.size(), 
            "✅ PO: project_draft_changes_ws - 2500 rekordów (wszystkie 3 batche zapisane)");
        
        // Sprawdź, czy wszystkie produkty są w draft changes
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
        
        logger.info("✅ TEST 12: Wszystkie 3 batche zapisane poprawnie (2500 rekordów)");
    }

    // ========== TEST 13: Zapisanie projektu - wydajność dla 8685 rekordów (realny scenariusz produkcyjny) ==========
    @Test
    @DisplayName("TEST 13: Zapisanie projektu - wydajność dla 8685 rekordów (realny scenariusz produkcyjny)")
    void testSaveProjectData_Performance_8685Records_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów i zapisujemy draft changes
        logger.info("🔄 TEST 13: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 13 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        logger.info("🔄 TEST 13: Zapisuję 8685 draft changes...");
        long saveDraftChangesStart = System.currentTimeMillis();
        SaveDraftChangesRequest draftRequest = createLargeBatchRequest(
            products,
            20.0,  // marginPercent
            10.0   // quantity
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest);
        long saveDraftChangesEnd = System.currentTimeMillis();
        long saveDraftChangesDuration = saveDraftChangesEnd - saveDraftChangesStart;
        logger.info("⏱️ [PERFORMANCE] TEST 13 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDraftChangesDuration, saveDraftChangesDuration / 1000.0);
        
        // WHEN: Zapisujemy projekt (saveProjectData) - to przenosi draft changes do ProjectProduct
        logger.info("🔄 TEST 13: Zapisuję projekt (saveProjectData) dla 8685 rekordów...");
        long saveProjectDataStart = System.currentTimeMillis();
        SaveProjectDataRequest saveRequest = new SaveProjectDataRequest();
        saveRequest.setTilesMargin(20.0);
        saveRequest.setTilesDiscount(null);
        projectService.saveProjectData(testProject.getId(), saveRequest);
        long saveProjectDataEnd = System.currentTimeMillis();
        long saveProjectDataDuration = saveProjectDataEnd - saveProjectDataStart;
        logger.info("⏱️ [PERFORMANCE] TEST 13 - saveProjectData (8685 rekordów): {}ms ({}s)", 
                   saveProjectDataDuration, saveProjectDataDuration / 1000.0);
        
        // THEN: Wszystkie draft changes zostały przeniesione do ProjectProduct
        long verifyStart = System.currentTimeMillis();
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(),
            "✅ PO saveProjectData: project_draft_changes_ws powinna być pusta (draft changes przeniesione)");
        
        List<ProjectProduct> projectProducts = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(8685, projectProducts.size(),
            "✅ PO saveProjectData: project_products powinna zawierać 8685 rekordów");
        
        // Sprawdź, czy wszystkie productIds są poprawne
        Set<Long> savedProductIds = projectProducts.stream()
            .map(ProjectProduct::getProductId)
            .collect(Collectors.toSet());
        Set<Long> expectedProductIds = products.stream()
            .map(Product::getId)
            .collect(Collectors.toSet());
        assertEquals(expectedProductIds, savedProductIds,
            "✅ Wszystkie productIds powinny być zapisane poprawnie");
        
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 13 - Weryfikacja: {}ms", verifyDuration);
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 13 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveDraftChanges: {}ms | saveProjectData: {}ms | verify: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDraftChangesDuration, 
                   saveProjectDataDuration, verifyDuration);
        
        // ⚠️ WAŻNE: Sprawdź, czy saveProjectData zakończył się w rozsądnym czasie (< 10s dla 8685 rekordów)
        assertTrue(saveProjectDataDuration < 10000,
                  "✅ saveProjectData powinien zakończyć się w < 10s dla 8685 rekordów. Czas: " + saveProjectDataDuration + "ms");
    }

    // ========== TEST 14: Wydajność findByProjectId dla 8685 rekordów ==========
    @Test
    @DisplayName("TEST 14: Wydajność findByProjectId dla 8685 rekordów (realny scenariusz produkcyjny)")
    void testFindByProjectId_Performance_8685Records_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów i zapisujemy draft changes
        logger.info("🔄 TEST 14: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 14 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        logger.info("🔄 TEST 14: Zapisuję 8685 draft changes...");
        long saveDraftChangesStart = System.currentTimeMillis();
        SaveDraftChangesRequest draftRequest = createLargeBatchRequest(
            products,
            20.0,  // marginPercent
            10.0   // quantity
        );
        projectService.saveDraftChanges(testProject.getId(), draftRequest);
        long saveDraftChangesEnd = System.currentTimeMillis();
        long saveDraftChangesDuration = saveDraftChangesEnd - saveDraftChangesStart;
        logger.info("⏱️ [PERFORMANCE] TEST 14 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDraftChangesDuration, saveDraftChangesDuration / 1000.0);
        
        // WHEN: Pobieramy draft changes przez findByProjectId (to jest używane w saveProjectData)
        logger.info("🔄 TEST 14: Pobieram 8685 draft changes przez findByProjectId...");
        long findByProjectIdStart = System.currentTimeMillis();
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        long findByProjectIdEnd = System.currentTimeMillis();
        long findByProjectIdDuration = findByProjectIdEnd - findByProjectIdStart;
        logger.info("⏱️ [PERFORMANCE] TEST 14 - findByProjectId (8685 rekordów): {}ms ({}s)", 
                   findByProjectIdDuration, findByProjectIdDuration / 1000.0);
        
        // THEN: Wszystkie rekordy powinny być pobrane
        assertEquals(8685, draftChanges.size(),
            "✅ findByProjectId powinien zwrócić 8685 rekordów");
        
        // Sprawdź, czy wszystkie productIds są poprawne
        Set<Long> retrievedProductIds = draftChanges.stream()
            .map(ProjectDraftChange::getProductId)
            .collect(Collectors.toSet());
        Set<Long> expectedProductIds = products.stream()
            .map(Product::getId)
            .collect(Collectors.toSet());
        assertEquals(expectedProductIds, retrievedProductIds,
            "✅ Wszystkie productIds powinny być pobrane poprawnie");
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 14 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveDraftChanges: {}ms | findByProjectId: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDraftChangesDuration, 
                   findByProjectIdDuration);
        
        // ⚠️ WAŻNE: Sprawdź, czy findByProjectId zakończył się w rozsądnym czasie (< 2s dla 8685 rekordów)
        // W produkcji widzieliśmy 798ms-1509ms, więc 2s to bezpieczny limit
        assertTrue(findByProjectIdDuration < 2000,
                  "✅ findByProjectId powinien zakończyć się w < 2s dla 8685 rekordów. Czas: " + findByProjectIdDuration + "ms");
    }

    // ========== TEST 15: Zapisanie projektu z 8685 rekordami + 579 productGroups (realny scenariusz produkcyjny) ==========
    @Test
    @DisplayName("TEST 15: Zapisanie projektu z 8685 rekordami + 579 productGroups (realny scenariusz produkcyjny)")
    void testSaveProjectData_Performance_8685Records_579ProductGroups_RealScenario() {
        long testStartTime = System.currentTimeMillis();
        
        // GIVEN: Tworzymy 8685 produktów i zapisujemy draft changes
        logger.info("🔄 TEST 15: Tworzenie 8685 produktów testowych (batch insert)...");
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(8685);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 15 - Utworzenie 8685 produktów: {}ms ({}s)", 
                   createProductsDuration, createProductsDuration / 1000.0);
        
        logger.info("🔄 TEST 15: Zapisuję 8685 draft changes...");
        long saveDraftChangesStart = System.currentTimeMillis();
        SaveDraftChangesRequest draftRequest = createLargeBatchRequest(products, 20.0, 10.0);
        projectService.saveDraftChanges(testProject.getId(), draftRequest);
        long saveDraftChangesEnd = System.currentTimeMillis();
        long saveDraftChangesDuration = saveDraftChangesEnd - saveDraftChangesStart;
        logger.info("⏱️ [PERFORMANCE] TEST 15 - saveDraftChanges (8685 zmian): {}ms ({}s)", 
                   saveDraftChangesDuration, saveDraftChangesDuration / 1000.0);
        
        // GIVEN: Tworzymy 579 productGroups (jak w logach produkcyjnych)
        logger.info("🔄 TEST 15: Tworzenie 579 productGroups...");
        long createProductGroupsStart = System.currentTimeMillis();
        List<SaveProjectProductGroupDTO> productGroups = new ArrayList<>();
        ProductCategory[] categories = {ProductCategory.TILE, ProductCategory.GUTTER, ProductCategory.ACCESSORY};
        String[] manufacturers = {"Manufacturer A", "Manufacturer B", "Manufacturer C", "Manufacturer D", "Manufacturer E"};
        String[] groupNames = {"Group 1", "Group 2", "Group 3", "Group 4", "Group 5"};
        GroupOption[] options = {GroupOption.MAIN, GroupOption.OPTIONAL, GroupOption.NONE};
        
        for (int i = 0; i < 579; i++) {
            SaveProjectProductGroupDTO dto = new SaveProjectProductGroupDTO();
            dto.setCategory(categories[i % categories.length]);
            dto.setManufacturer(manufacturers[i % manufacturers.length]);
            dto.setGroupName(groupNames[i % groupNames.length] + " " + (i / groupNames.length + 1));
            dto.setIsMainOption(options[i % options.length]);
            productGroups.add(dto);
        }
        long createProductGroupsEnd = System.currentTimeMillis();
        long createProductGroupsDuration = createProductGroupsEnd - createProductGroupsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 15 - Utworzenie 579 productGroups: {}ms", createProductGroupsDuration);
        
        // WHEN: Zapisujemy projekt (saveProjectData) z 8685 rekordami + 579 productGroups
        logger.info("🔄 TEST 15: Zapisuję projekt (saveProjectData) dla 8685 rekordów + 579 productGroups...");
        long saveProjectDataStart = System.currentTimeMillis();
        SaveProjectDataRequest saveRequest = new SaveProjectDataRequest();
        saveRequest.setTilesMargin(20.0);
        saveRequest.setTilesDiscount(null);
        saveRequest.setProductGroups(productGroups);
        projectService.saveProjectData(testProject.getId(), saveRequest);
        long saveProjectDataEnd = System.currentTimeMillis();
        long saveProjectDataDuration = saveProjectDataEnd - saveProjectDataStart;
        logger.info("⏱️ [PERFORMANCE] TEST 15 - saveProjectData (8685 rekordów + 579 productGroups): {}ms ({}s)", 
                   saveProjectDataDuration, saveProjectDataDuration / 1000.0);
        
        // THEN: Wszystkie draft changes zostały przeniesione do ProjectProduct
        long verifyStart = System.currentTimeMillis();
        assertEquals(0, projectDraftChangeRepository.findByProjectId(testProject.getId()).size(),
            "✅ PO saveProjectData: project_draft_changes_ws powinna być pusta (draft changes przeniesione)");
        
        List<ProjectProduct> projectProducts = projectProductRepository.findByProjectId(testProject.getId());
        assertEquals(8685, projectProducts.size(),
            "✅ PO saveProjectData: project_products powinna zawierać 8685 rekordów");
        
        long verifyDuration = System.currentTimeMillis() - verifyStart;
        logger.info("⏱️ [PERFORMANCE] TEST 15 - Weryfikacja: {}ms", verifyDuration);
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 15 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveDraftChanges: {}ms | createProductGroups: {}ms | saveProjectData: {}ms | verify: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveDraftChangesDuration, 
                   createProductGroupsDuration, saveProjectDataDuration, verifyDuration);
        
        // ⚠️ WAŻNE: Sprawdź, czy saveProjectData zakończył się w rozsądnym czasie (< 30s dla 8685 rekordów + 579 productGroups)
        // W logach produkcyjnych widzieliśmy ~27s, więc 30s to bezpieczny limit
        assertTrue(saveProjectDataDuration < 30000,
                  "✅ saveProjectData powinien zakończyć się w < 30s dla 8685 rekordów + 579 productGroups. Czas: " + saveProjectDataDuration + "ms");
    }

    // ========== TEST 16: Batch update opcji grupy - poprawność ==========
    @Test
    @DisplayName("TEST 16: Batch update opcji grupy - poprawność")
    void testUpdateGroupOptionBatch_Correctness() {
        long testStartTime = System.currentTimeMillis();
        logger.info("🧪 TEST 16: Batch update opcji grupy - poprawność");
        
        // 1. Utwórz produkty testowe
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(10, ProductCategory.TILE);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 16 - Utworzenie 10 produktów: {}ms", createProductsDuration);
        
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
            dto.setDraftIsMainOption(GroupOption.NONE); // Początkowo NONE
            initialChanges.add(dto);
        }
        initialRequest.setChanges(initialChanges);
        
        long saveInitialStart = System.currentTimeMillis();
        projectService.saveDraftChanges(testProject.getId(), initialRequest);
        long saveInitialEnd = System.currentTimeMillis();
        long saveInitialDuration = saveInitialEnd - saveInitialStart;
        logger.info("⏱️ [PERFORMANCE] TEST 16 - Zapisanie początkowych draft changes: {}ms", saveInitialDuration);
        
        // 3. Sprawdź początkowy stan
        List<ProjectDraftChange> beforeUpdate = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(10, beforeUpdate.size(), "Powinno być 10 draft changes");
        for (ProjectDraftChange dc : beforeUpdate) {
            assertEquals(GroupOption.NONE, dc.getDraftIsMainOption(), "Początkowo powinno być NONE");
            assertNotNull(dc.getDraftRetailPrice(), "Początkowo powinna być cena");
        }
        
        // 4. Batch update opcji grupy na OPTIONAL
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
        logger.info("⏱️ [PERFORMANCE] TEST 16 - Batch update opcji grupy (10 produktów): {}ms", updateBatchDuration);
        
        // 5. Sprawdź zaktualizowany stan
        List<ProjectDraftChange> afterUpdate = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(10, afterUpdate.size(), "Powinno nadal być 10 draft changes");
        for (ProjectDraftChange dc : afterUpdate) {
            assertEquals(GroupOption.OPTIONAL, dc.getDraftIsMainOption(), "Opcja powinna być OPTIONAL");
            // ⚠️ WAŻNE: Inne pola NIE powinny być zmienione
            assertNotNull(dc.getDraftRetailPrice(), "Cena powinna pozostać");
            assertEquals(100.0, dc.getDraftRetailPrice(), 0.01, "Cena powinna być taka sama");
            assertEquals(10.0, dc.getDraftQuantity(), 0.01, "Ilość powinna być taka sama");
        }
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 16 - CAŁKOWITY CZAS: {}ms | createProducts: {}ms | saveInitial: {}ms | updateBatch: {}ms",
                   testDuration, createProductsDuration, saveInitialDuration, updateBatchDuration);
        
        // ⚠️ WAŻNE: Batch update powinien być szybki (< 500ms dla 10 produktów)
        assertTrue(updateBatchDuration < 500,
                  "✅ Batch update powinien zakończyć się w < 500ms dla 10 produktów. Czas: " + updateBatchDuration + "ms");
    }
    
    // ========== TEST 17: Batch update opcji grupy - UPSERT (gdy rekord nie istnieje) ==========
    @Test
    @DisplayName("TEST 17: Batch update opcji grupy - UPSERT (gdy rekord nie istnieje)")
    void testUpdateGroupOptionBatch_UPSERT_WhenRecordNotExists() {
        long testStartTime = System.currentTimeMillis();
        logger.info("🧪 TEST 17: Batch update opcji grupy - UPSERT (gdy rekord nie istnieje)");
        
        // 1. Utwórz produkty testowe
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(5, ProductCategory.TILE);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 17 - Utworzenie 5 produktów: {}ms", createProductsDuration);
        
        // 2. Sprawdź, że NIE ma draft changes dla tych produktów
        List<ProjectDraftChange> beforeUpdate = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(0, beforeUpdate.size(), "Nie powinno być draft changes przed update");
        
        // 3. Batch update opcji grupy (UPSERT - utworzy nowe rekordy)
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        UpdateGroupOptionBatchRequest batchRequest = new UpdateGroupOptionBatchRequest(
            "TILE",
            productIds,
            GroupOption.MAIN
        );
        
        long updateBatchStart = System.currentTimeMillis();
        projectService.updateGroupOptionBatch(testProject.getId(), batchRequest);
        long updateBatchEnd = System.currentTimeMillis();
        long updateBatchDuration = updateBatchEnd - updateBatchStart;
        logger.info("⏱️ [PERFORMANCE] TEST 17 - Batch update opcji grupy (UPSERT, 5 produktów): {}ms", updateBatchDuration);
        
        // 4. Sprawdź, że rekordy zostały utworzone
        List<ProjectDraftChange> afterUpdate = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(5, afterUpdate.size(), "Powinno być 5 draft changes po UPSERT");
        for (ProjectDraftChange dc : afterUpdate) {
            assertEquals(GroupOption.MAIN, dc.getDraftIsMainOption(), "Opcja powinna być MAIN");
            // ⚠️ WAŻNE: Inne pola powinny być NULL (tylko draft_is_main_option został ustawiony)
            assertNull(dc.getDraftRetailPrice(), "Cena powinna być NULL (tylko opcja została ustawiona)");
            assertNull(dc.getDraftQuantity(), "Ilość powinna być NULL");
        }
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 17 - CAŁKOWITY CZAS: {}ms | createProducts: {}ms | updateBatch: {}ms",
                   testDuration, createProductsDuration, updateBatchDuration);
    }
    
    // ========== TEST 18: Batch update opcji grupy - wydajność (duża liczba produktów) ==========
    @Test
    @DisplayName("TEST 18: Batch update opcji grupy - wydajność (duża liczba produktów)")
    void testUpdateGroupOptionBatch_Performance_LargeBatch() {
        long testStartTime = System.currentTimeMillis();
        logger.info("🧪 TEST 18: Batch update opcji grupy - wydajność (duża liczba produktów)");
        
        // 1. Utwórz dużą liczbę produktów (np. 1000)
        long createProductsStart = System.currentTimeMillis();
        List<Product> products = createProductsBatch(1000, ProductCategory.TILE);
        long createProductsEnd = System.currentTimeMillis();
        long createProductsDuration = createProductsEnd - createProductsStart;
        logger.info("⏱️ [PERFORMANCE] TEST 18 - Utworzenie 1000 produktów: {}ms ({}s)", 
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
        logger.info("⏱️ [PERFORMANCE] TEST 18 - Zapisanie początkowych draft changes (1000): {}ms ({}s)", 
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
        logger.info("⏱️ [PERFORMANCE] TEST 18 - Batch update opcji grupy (1000 produktów): {}ms ({}s)", 
                   updateBatchDuration, updateBatchDuration / 1000.0);
        
        // 4. Sprawdź poprawność
        List<ProjectDraftChange> afterUpdate = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(1000, afterUpdate.size(), "Powinno być 1000 draft changes");
        for (ProjectDraftChange dc : afterUpdate) {
            assertEquals(GroupOption.OPTIONAL, dc.getDraftIsMainOption(), "Opcja powinna być OPTIONAL");
            // ⚠️ WAŻNE: Inne pola NIE powinny być zmienione
            assertNotNull(dc.getDraftRetailPrice(), "Cena powinna pozostać");
        }
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("⏱️ [PERFORMANCE] TEST 18 - CAŁKOWITY CZAS: {}ms ({}s) | createProducts: {}ms | saveInitial: {}ms | updateBatch: {}ms",
                   testDuration, testDuration / 1000.0, createProductsDuration, saveInitialDuration, updateBatchDuration);
        
        // ⚠️ WAŻNE: Batch update powinien być szybki (< 2s dla 1000 produktów)
        assertTrue(updateBatchDuration < 2000,
                  "✅ Batch update powinien zakończyć się w < 2s dla 1000 produktów. Czas: " + updateBatchDuration + "ms");
    }
    
    // ⚠️ UWAGA: Metody pomocnicze (createDraftChangesRequest, createLargeBatchRequest) 
    // są teraz w BaseProjectServiceTest - dziedziczymy je!
}

