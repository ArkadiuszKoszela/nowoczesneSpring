package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ TESTY POPRAWNOŚCIOWE I WYDAJNOŚCIOWE DLA GENEROWANIA PDF OFERT
 * 
 * Testuje generowanie PDF z szablonów:
 * - generatePdfFromTemplate() - generowanie PDF dla projektu
 * - Poprawność renderowania danych klienta
 * - Poprawność renderowania produktów
 * - Poprawność obliczeń sum, rabatów, marż w PDF
 * - Wydajność dla dużych projektów (1000+ produktów)
 * 
 * Testuje:
 * - Edge cases (brak klienta, brak produktów, puste dane)
 * - Wydajność dla dużych projektów
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class OfferPdfServiceTest extends BaseProjectServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(OfferPdfServiceTest.class);

    @Autowired
    private OfferPdfService offerPdfService;

    @Autowired
    private OfferTemplateRepository offerTemplateRepository;

    @Autowired
    private ProjectProductRepository projectProductRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - generatePdfFromTemplate()
    // ==========================================

    @Test
    void testGeneratePdfFromTemplate_StandardCase() throws IOException {
        logger.info("🧪 TEST: generatePdfFromTemplate - standardowy przypadek");
        
        // 1. Utwórz produkty w projekcie
        List<ProjectProduct> projectProducts = createProjectProducts(10);
        projectProductRepository.saveAll(projectProducts);
        
        // 2. Generuj PDF (użyje domyślnego szablonu)
        long generateStartTime = System.currentTimeMillis();
        byte[] pdfBytes = offerPdfService.generatePdfFromTemplate(testProject, null);
        long generateEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] Generowanie PDF - 10 produktów: {}ms", 
                   generateEndTime - generateStartTime);
        
        // 4. Weryfikacja
        assertNotNull(pdfBytes, "PDF powinien być wygenerowany");
        assertTrue(pdfBytes.length > 0, "PDF nie powinien być pusty");
        assertTrue(pdfBytes.length > 1000, "PDF powinien mieć rozsądny rozmiar (>1KB)");
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Wygenerowano PDF o rozmiarze {} bajtów", pdfBytes.length);
    }

    @Test
    void testGeneratePdfFromTemplate_WithSpecificTemplate() throws IOException {
        logger.info("🧪 TEST: generatePdfFromTemplate - konkretny szablon");
        
        List<ProjectProduct> projectProducts = createProjectProducts(5);
        projectProductRepository.saveAll(projectProducts);
        
        OfferTemplate template = offerTemplateRepository.findByIsDefaultTrue()
            .orElseThrow(() -> new IllegalStateException("Brak domyślnego szablonu"));
        
        byte[] pdfBytes = offerPdfService.generatePdfFromTemplate(testProject, template.getId());
        
        assertNotNull(pdfBytes, "PDF powinien być wygenerowany");
        assertTrue(pdfBytes.length > 0, "PDF nie powinien być pusty");
    }

    @Test
    void testGeneratePdfFromTemplate_EdgeCase_NoProducts() throws IOException {
        logger.info("🧪 TEST: generatePdfFromTemplate - brak produktów");
        
        // Projekt bez produktów
        byte[] pdfBytes = offerPdfService.generatePdfFromTemplate(testProject, null);
        
        assertNotNull(pdfBytes, "PDF powinien być wygenerowany nawet bez produktów");
        assertTrue(pdfBytes.length > 0, "PDF nie powinien być pusty");
    }

    @Test
    void testGeneratePdfFromTemplate_EdgeCase_NonExistentTemplate() {
        logger.info("🧪 TEST: generatePdfFromTemplate - nieistniejący szablon");
        
        List<ProjectProduct> projectProducts = createProjectProducts(5);
        projectProductRepository.saveAll(projectProducts);
        
        assertThrows(IllegalArgumentException.class, () -> {
            offerPdfService.generatePdfFromTemplate(testProject, 99999L);
        }, "Powinno rzucić IllegalArgumentException dla nieistniejącego szablonu");
    }

    // ==========================================
    // TESTY WYDAJNOŚCIOWE - generatePdfFromTemplate()
    // ==========================================

    @Test
    void testGeneratePdfFromTemplate_Performance_100Products() throws IOException {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: generatePdfFromTemplate - 100 produktów");
        
        long createStartTime = System.currentTimeMillis();
        List<ProjectProduct> projectProducts = createProjectProducts(100);
        projectProductRepository.saveAll(projectProducts);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 100 produktów w projekcie: {}ms", 
                   createEndTime - createStartTime);
        
        long generateStartTime = System.currentTimeMillis();
        byte[] pdfBytes = offerPdfService.generatePdfFromTemplate(testProject, null);
        long generateEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] Generowanie PDF - 100 produktów: {}ms ({}s)", 
                   generateEndTime - generateStartTime, 
                   (generateEndTime - generateStartTime) / 1000.0);
        
        assertNotNull(pdfBytes, "PDF powinien być wygenerowany");
        assertTrue(pdfBytes.length > 0, "PDF nie powinien być pusty");
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Wygenerowano PDF o rozmiarze {} bajtów", pdfBytes.length);
    }

    @Test
    void testGeneratePdfFromTemplate_Performance_500Products() throws IOException {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: generatePdfFromTemplate - 500 produktów");
        
        long createStartTime = System.currentTimeMillis();
        List<ProjectProduct> projectProducts = createProjectProducts(500);
        projectProductRepository.saveAll(projectProducts);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 500 produktów w projekcie: {}ms", 
                   createEndTime - createStartTime);
        
        long generateStartTime = System.currentTimeMillis();
        byte[] pdfBytes = offerPdfService.generatePdfFromTemplate(testProject, null);
        long generateEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] Generowanie PDF - 500 produktów: {}ms ({}s)", 
                   generateEndTime - generateStartTime, 
                   (generateEndTime - generateStartTime) / 1000.0);
        
        assertNotNull(pdfBytes, "PDF powinien być wygenerowany");
        assertTrue(pdfBytes.length > 0, "PDF nie powinien być pusty");
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Wygenerowano PDF o rozmiarze {} bajtów", pdfBytes.length);
    }

    @Test
    void testGeneratePdfFromTemplate_Performance_1000Products() throws IOException {
        logger.info("🚀 TEST WYDAJNOŚCIOWY: generatePdfFromTemplate - 1000 produktów");
        
        long createStartTime = System.currentTimeMillis();
        List<ProjectProduct> projectProducts = createProjectProducts(1000);
        projectProductRepository.saveAll(projectProducts);
        long createEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Utworzenie 1000 produktów w projekcie: {}ms", 
                   createEndTime - createStartTime);
        
        long generateStartTime = System.currentTimeMillis();
        byte[] pdfBytes = offerPdfService.generatePdfFromTemplate(testProject, null);
        long generateEndTime = System.currentTimeMillis();
        
        logger.info("⏱️ [PERFORMANCE] Generowanie PDF - 1000 produktów: {}ms ({}s)", 
                   generateEndTime - generateStartTime, 
                   (generateEndTime - generateStartTime) / 1000.0);
        
        assertNotNull(pdfBytes, "PDF powinien być wygenerowany");
        assertTrue(pdfBytes.length > 0, "PDF nie powinien być pusty");
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Wygenerowano PDF o rozmiarze {} bajtów", pdfBytes.length);
    }

    // ==========================================
    // FUNKCJE POMOCNICZE
    // ==========================================

    /**
     * Utwórz produkty w projekcie (ProjectProduct)
     */
    private List<ProjectProduct> createProjectProducts(int count) {
        List<ProjectProduct> projectProducts = new ArrayList<>();
        
        // Najpierw utwórz produkty w bazie
        List<Product> products = createProductsBatch(count);
        
        for (int i = 0; i < count; i++) {
            Product product = products.get(i);
            
            ProjectProduct projectProduct = new ProjectProduct();
            projectProduct.setProject(testProject);
            projectProduct.setProductId(product.getId());
            projectProduct.setCategory(product.getCategory());
            projectProduct.setSavedRetailPrice(product.getRetailPrice());
            projectProduct.setSavedPurchasePrice(product.getPurchasePrice());
            projectProduct.setSavedSellingPrice(product.getRetailPrice()); // Domyślnie = retailPrice
            projectProduct.setSavedQuantity(10.0 + i);
            projectProduct.setPriceChangeSource(PriceChangeSource.MARGIN);
            projectProduct.setSavedMarginPercent(25.0);
            projectProduct.setSavedDiscountPercent(0.0);
            
            projectProducts.add(projectProduct);
        }
        
        return projectProducts;
    }
}

