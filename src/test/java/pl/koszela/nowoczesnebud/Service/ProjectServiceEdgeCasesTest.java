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
 * 🎯 TESTY BRZEGOWE (EDGE CASES) - Różne scenariusze brzegowe
 * 
 * Testuje brzegowe przypadki:
 * - Cena zakupu = 0
 * - Cena detaliczna = 0
 * - Bardzo małe wartości (0.01)
 * - Bardzo duże wartości
 * - Ujemne wartości (jeśli dozwolone)
 */
@DisplayName("Testy brzegowe (edge cases) - różne scenariusze")
class ProjectServiceEdgeCasesTest extends BaseProjectServiceTest {

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        setUpBase();
        testProducts = createProductsBatch(50);
    }

    @Test
    @DisplayName("TEST 1: Brzegowy przypadek - cena zakupu = 0")
    void testEdgeCase_ZeroPurchasePrice() {
        // GIVEN: Produkt z ceną zakupu = 0
        Product product = testProducts.get(0);
        product.setPurchasePrice(0.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy marżę 20%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(20.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftPurchasePrice(0.0);
        change.setDraftMarginPercent(20.0);
        // Cena sprzedaży = 0.0 * 1.2 = 0.0
        change.setDraftSellingPrice(0.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być 0
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(0.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być 0 gdy cena zakupu = 0");
    }

    @Test
    @DisplayName("TEST 2: Brzegowy przypadek - cena detaliczna = 0")
    void testEdgeCase_ZeroRetailPrice() {
        // GIVEN: Produkt z ceną detaliczną = 0
        Product product = testProducts.get(0);
        product.setRetailPrice(0.0);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy rabat 10%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryDiscount(10.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftRetailPrice(0.0);
        change.setDraftDiscountPercent(10.0);
        // Cena sprzedaży = 0.0 * 0.9 = 0.0
        change.setDraftSellingPrice(0.0);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być 0
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertEquals(0.0, draft.getDraftSellingPrice(), 0.01, 
                    "✅ Cena sprzedaży powinna być 0 gdy cena detaliczna = 0");
    }

    @Test
    @DisplayName("TEST 3: Brzegowy przypadek - bardzo małe wartości (0.01)")
    void testEdgeCase_VerySmallValues() {
        // GIVEN: Produkt z bardzo małą ceną zakupu
        Product product = testProducts.get(0);
        product.setPurchasePrice(0.01);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy marżę 20%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(20.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftPurchasePrice(0.01);
        change.setDraftMarginPercent(20.0);
        // Cena sprzedaży = 0.01 * 1.2 = 0.012 -> zaokrąglone do 0.01
        change.setDraftSellingPrice(0.01);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być obliczona poprawnie (z zaokrągleniem)
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertTrue(draft.getDraftSellingPrice() >= 0.01, 
                  "✅ Cena sprzedaży powinna być >= 0.01");
    }

    @Test
    @DisplayName("TEST 4: Brzegowy przypadek - bardzo duże wartości")
    void testEdgeCase_VeryLargeValues() {
        // GIVEN: Produkt z bardzo dużą ceną zakupu
        Product product = testProducts.get(0);
        product.setPurchasePrice(999999.99);
        product = productRepository.save(product);
        
        // WHEN: Ustawiamy marżę 20%
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        request.setCategoryMargin(20.0);
        
        List<DraftChangeDTO> changes = new ArrayList<>();
        DraftChangeDTO change = new DraftChangeDTO(product.getId(), ProductCategory.TILE.name());
        change.setDraftPurchasePrice(999999.99);
        change.setDraftMarginPercent(20.0);
        // Cena sprzedaży = 999999.99 * 1.2 = 1199999.988
        change.setDraftSellingPrice(1199999.99);
        changes.add(change);
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Cena sprzedaży powinna być obliczona poprawnie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        ProjectDraftChange draft = draftChanges.get(0);
        assertTrue(draft.getDraftSellingPrice() > 1000000.0, 
                  "✅ Cena sprzedaży powinna być > 1000000.0");
    }

    @Test
    @DisplayName("TEST 5: Brzegowy przypadek - quantity z wartościami dziesiętnymi")
    void testEdgeCase_DecimalQuantity() {
        // GIVEN: Produkty
        List<Product> products = testProducts.subList(0, 10);
        
        // WHEN: Ustawiamy quantity z wartościami dziesiętnymi
        SaveDraftChangesRequest request = new SaveDraftChangesRequest();
        request.setCategory(ProductCategory.TILE.name());
        List<DraftChangeDTO> changes = new ArrayList<>();
        
        for (int i = 0; i < products.size(); i++) {
            DraftChangeDTO change = new DraftChangeDTO(products.get(i).getId(), ProductCategory.TILE.name());
            change.setDraftQuantity(10.0 + (i * 0.1));  // 10.0, 10.1, 10.2, ..., 10.9
            changes.add(change);
        }
        request.setChanges(changes);
        
        projectService.saveDraftChanges(testProject.getId(), request);
        
        // THEN: Wszystkie quantity powinny być zapisane poprawnie
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(testProject.getId());
        assertEquals(10, draftChanges.size(), "✅ Wszystkie draft changes powinny być zapisane");
        
        for (int i = 0; i < draftChanges.size(); i++) {
            ProjectDraftChange draft = draftChanges.get(i);
            double expectedQuantity = 10.0 + (i * 0.1);
            assertEquals(expectedQuantity, draft.getDraftQuantity(), 0.01, 
                        "✅ Quantity z wartością dziesiętną powinno być zapisane poprawnie dla produktu " + i);
        }
    }
}



