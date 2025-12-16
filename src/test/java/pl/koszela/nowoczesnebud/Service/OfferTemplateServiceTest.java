package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.Address;
import pl.koszela.nowoczesnebud.Model.OfferTemplate;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.OfferTemplateRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 TESTY DLA OfferTemplateService
 * 
 * Testuje:
 * - Operacje CRUD na szablonach ofert
 * - Pobieranie domyślnego szablonu
 * - Ustawianie szablonu jako domyślnego
 * - Renderowanie szablonów z danymi projektu
 * - Walidację (nie można usunąć domyślnego szablonu)
 * - Wydajność dla wielu szablonów
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class OfferTemplateServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(OfferTemplateServiceTest.class);

    @Autowired
    private OfferTemplateService offerTemplateService;

    @Autowired
    private OfferTemplateRepository offerTemplateRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private javax.persistence.EntityManager entityManager;

    private OfferTemplate testTemplate;
    private Project testProject;

    @BeforeEach
    void setUp() {
        // ⚠️ WAŻNE: Wyczyść wszystkie domyślne szablony przed testami (mogą pozostać z poprzednich testów)
        List<OfferTemplate> allTemplates = offerTemplateRepository.findAll();
        for (OfferTemplate template : allTemplates) {
            if (template.getIsDefault() != null && template.getIsDefault()) {
                template.setIsDefault(false);
                offerTemplateRepository.save(template);
            }
        }
        // Flush, żeby upewnić się, że zmiany są zapisane przed kolejnym testem
        entityManager.flush();
        
        // Utwórz testowego użytkownika (klienta)
        User testUser = new User();
        testUser.setName("Test");
        testUser.setSurname("Client");
        
        Address address = new Address();
        address.setAddress("Test Address");
        address.setLatitude(52.0);
        address.setLongitude(21.0);
        address.setZoom(10.0);
        testUser.setAddress(address);
        
        testUser = userRepository.save(testUser);

        // Utwórz testowy projekt
        testProject = new Project();
        testProject.setClient(testUser);
        testProject = projectRepository.save(testProject);

        // Utwórz testowy szablon
        testTemplate = new OfferTemplate();
        testTemplate.setName("Test Template");
        testTemplate.setDescription("Test Description");
        testTemplate.setHtmlContent("<h1>Test HTML</h1>");
        testTemplate.setCssContent("body { color: black; }");
        testTemplate.setIsDefault(false);
        testTemplate = offerTemplateRepository.save(testTemplate);
    }

    // ========== TESTY POPRAWNOŚCIOWE ==========

    @Test
    void testGetAllTemplates_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getAllTemplates - poprawność");

        List<OfferTemplate> templates = offerTemplateService.getAllTemplates();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getAllTemplates: {}ms | znaleziono: {} szablonów", 
                   duration, templates.size());

        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        assertTrue(templates.stream().anyMatch(t -> t.getId().equals(testTemplate.getId())));
    }

    @Test
    void testGetTemplateById_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getTemplateById - poprawność");

        Optional<OfferTemplate> found = offerTemplateService.getTemplateById(testTemplate.getId());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getTemplateById: {}ms", duration);

        assertTrue(found.isPresent());
        assertEquals(testTemplate.getId(), found.get().getId());
        assertEquals("Test Template", found.get().getName());
    }

    @Test
    void testGetTemplateById_NotFound() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getTemplateById - nie znaleziono");

        Optional<OfferTemplate> found = offerTemplateService.getTemplateById(999999L);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getTemplateById (not found): {}ms", duration);

        assertFalse(found.isPresent());
    }

    @Test
    void testGetDefaultTemplate_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: getDefaultTemplate - poprawność");

        // ⚠️ WAŻNE: Upewnij się, że nie ma innych domyślnych szablonów
        List<OfferTemplate> allTemplates = offerTemplateRepository.findAll();
        for (OfferTemplate template : allTemplates) {
            if (template.getIsDefault() != null && template.getIsDefault()) {
                template.setIsDefault(false);
                offerTemplateRepository.save(template);
            }
        }
        // Flush, żeby upewnić się, że zmiany są zapisane przed kolejnym testem
        entityManager.flush();

        // Utwórz domyślny szablon
        OfferTemplate defaultTemplate = new OfferTemplate();
        defaultTemplate.setName("Default Template");
        defaultTemplate.setIsDefault(true);
        defaultTemplate = offerTemplateRepository.save(defaultTemplate);

        Optional<OfferTemplate> found = offerTemplateService.getDefaultTemplate();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] getDefaultTemplate: {}ms", duration);

        assertTrue(found.isPresent());
        assertEquals(defaultTemplate.getId(), found.get().getId());
        assertTrue(found.get().getIsDefault());
    }

    @Test
    void testSaveTemplate_CreateNew_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveTemplate - tworzenie nowego");

        OfferTemplate newTemplate = new OfferTemplate();
        newTemplate.setName("New Template");
        newTemplate.setDescription("New Description");
        newTemplate.setHtmlContent("<h1>New HTML</h1>");
        newTemplate.setCssContent("body { color: blue; }");
        newTemplate.setIsDefault(false);

        OfferTemplate saved = offerTemplateService.saveTemplate(newTemplate);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveTemplate (create): {}ms | savedId: {}", duration, saved.getId());

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("New Template", saved.getName());
        assertEquals("New Description", saved.getDescription());
    }

    @Test
    void testSaveTemplate_UpdateExisting_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveTemplate - aktualizacja istniejącego");

        testTemplate.setName("Updated Template");
        testTemplate.setDescription("Updated Description");

        OfferTemplate updated = offerTemplateService.saveTemplate(testTemplate);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveTemplate (update): {}ms", duration);

        assertNotNull(updated);
        assertEquals(testTemplate.getId(), updated.getId());
        assertEquals("Updated Template", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    void testSaveTemplate_SetAsDefault_RemovesOtherDefault() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: saveTemplate - ustawienie jako domyślny usuwa domyślny status z innych");

        // ⚠️ WAŻNE: Upewnij się, że nie ma innych domyślnych szablonów
        List<OfferTemplate> allTemplates = offerTemplateRepository.findAll();
        for (OfferTemplate template : allTemplates) {
            if (template.getIsDefault() != null && template.getIsDefault()) {
                template.setIsDefault(false);
                offerTemplateRepository.save(template);
            }
        }
        // Flush, żeby upewnić się, że zmiany są zapisane przed kolejnym testem
        entityManager.flush();

        // Utwórz domyślny szablon
        OfferTemplate oldDefault = new OfferTemplate();
        oldDefault.setName("Old Default");
        oldDefault.setIsDefault(true);
        oldDefault = offerTemplateRepository.save(oldDefault);

        // Ustaw testTemplate jako domyślny
        testTemplate.setIsDefault(true);
        OfferTemplate newDefault = offerTemplateService.saveTemplate(testTemplate);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] saveTemplate (set default): {}ms", duration);

        assertTrue(newDefault.getIsDefault());

        // Sprawdź czy stary domyślny szablon nie jest już domyślny
        OfferTemplate oldDefaultUpdated = offerTemplateRepository.findById(oldDefault.getId()).orElse(null);
        assertNotNull(oldDefaultUpdated);
        assertFalse(oldDefaultUpdated.getIsDefault());
    }

    @Test
    void testSetDefaultTemplate_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: setDefaultTemplate - poprawność");

        // ⚠️ WAŻNE: Upewnij się, że nie ma innych domyślnych szablonów
        List<OfferTemplate> allTemplates = offerTemplateRepository.findAll();
        for (OfferTemplate template : allTemplates) {
            if (template.getIsDefault() != null && template.getIsDefault()) {
                template.setIsDefault(false);
                offerTemplateRepository.save(template);
            }
        }
        // Flush, żeby upewnić się, że zmiany są zapisane przed kolejnym testem
        entityManager.flush();

        // Utwórz domyślny szablon
        OfferTemplate oldDefault = new OfferTemplate();
        oldDefault.setName("Old Default");
        oldDefault.setIsDefault(true);
        oldDefault = offerTemplateRepository.save(oldDefault);

        OfferTemplate newDefault = offerTemplateService.setDefaultTemplate(testTemplate.getId());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] setDefaultTemplate: {}ms", duration);

        assertNotNull(newDefault);
        assertTrue(newDefault.getIsDefault());
        assertEquals(testTemplate.getId(), newDefault.getId());

        // Sprawdź czy stary domyślny szablon nie jest już domyślny
        OfferTemplate oldDefaultUpdated = offerTemplateRepository.findById(oldDefault.getId()).orElse(null);
        assertNotNull(oldDefaultUpdated);
        assertFalse(oldDefaultUpdated.getIsDefault());
    }

    @Test
    void testSetDefaultTemplate_NotFound() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: setDefaultTemplate - nie znaleziono");

        assertThrows(IllegalArgumentException.class, () -> {
            offerTemplateService.setDefaultTemplate(999999L);
        });

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] setDefaultTemplate (not found): {}ms", duration);
    }

    @Test
    void testDeleteTemplate_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: deleteTemplate - poprawność");

        offerTemplateService.deleteTemplate(testTemplate.getId());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteTemplate: {}ms", duration);

        Optional<OfferTemplate> deleted = offerTemplateRepository.findById(testTemplate.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    void testDeleteTemplate_NotFound() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: deleteTemplate - nie znaleziono");

        assertThrows(IllegalArgumentException.class, () -> {
            offerTemplateService.deleteTemplate(999999L);
        });

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteTemplate (not found): {}ms", duration);
    }

    @Test
    void testDeleteTemplate_CannotDeleteDefault() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: deleteTemplate - nie można usunąć domyślnego");

        // Utwórz domyślny szablon
        final OfferTemplate defaultTemplate = new OfferTemplate();
        defaultTemplate.setName("Default Template");
        defaultTemplate.setIsDefault(true);
        offerTemplateRepository.save(defaultTemplate);

        assertThrows(IllegalStateException.class, () -> {
            offerTemplateService.deleteTemplate(defaultTemplate.getId());
        });

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] deleteTemplate (cannot delete default): {}ms", duration);

        // Sprawdź czy szablon nadal istnieje
        Optional<OfferTemplate> stillExists = offerTemplateRepository.findById(defaultTemplate.getId());
        assertTrue(stillExists.isPresent());
    }

    @Test
    void testRenderTemplate_Correctness() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: renderTemplate - poprawność");

        OfferTemplate template = new OfferTemplate();
        template.setHtmlContent("<h1 th:text=\"${project.client.name}\">Client Name</h1>");
        template.setCssContent("h1 { color: red; }");

        String rendered = offerTemplateService.renderTemplate(template, testProject);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] renderTemplate: {}ms | rendered length: {}", 
                   duration, rendered != null ? rendered.length() : 0);

        assertNotNull(rendered);
        assertTrue(rendered.contains("<style>"));
        assertTrue(rendered.contains("h1 { color: red; }"));
    }

    @Test
    void testRenderTemplate_EmptyHtml() {
        long startTime = System.currentTimeMillis();
        logger.info("🧪 TEST: renderTemplate - pusty HTML");

        OfferTemplate template = new OfferTemplate();
        template.setHtmlContent(null);

        String rendered = offerTemplateService.renderTemplate(template, testProject);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] renderTemplate (empty HTML): {}ms", duration);

        assertNotNull(rendered);
        assertTrue(rendered.contains("Szablon nie ma zawartości HTML"));
    }

    // ========== TESTY WYDAJNOŚCIOWE ==========

    @Test
    void testGetAllTemplates_Performance_ManyTemplates() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: getAllTemplates - wiele szablonów (500)");

        // Utwórz 500 szablonów
        for (int i = 0; i < 500; i++) {
            OfferTemplate template = new OfferTemplate();
            template.setName("Template " + i);
            template.setDescription("Description " + i);
            template.setHtmlContent("<h1>Template " + i + "</h1>");
            template.setIsDefault(false);
            offerTemplateRepository.save(template);
        }

        long operationStart = System.currentTimeMillis();
        List<OfferTemplate> templates = offerTemplateService.getAllTemplates();
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] getAllTemplates (500 szablonów): {}ms ({}s) | znaleziono: {}", 
                   duration, duration / 1000.0, templates.size());

        assertNotNull(templates);
        assertTrue(templates.size() >= 500);
        assertTrue(duration < 2000, "Operacja powinna zakończyć się w ciągu 2 sekund");
    }

    @Test
    void testRenderTemplate_Performance_LargeTemplate() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: renderTemplate - duży szablon (100KB HTML)");

        // Utwórz duży szablon HTML
        StringBuilder largeHtml = new StringBuilder();
        largeHtml.append("<html><body>");
        for (int i = 0; i < 10000; i++) {
            largeHtml.append("<p>Paragraph ").append(i).append("</p>");
        }
        largeHtml.append("</body></html>");

        OfferTemplate template = new OfferTemplate();
        template.setHtmlContent(largeHtml.toString());
        template.setCssContent("body { margin: 0; }");

        long operationStart = System.currentTimeMillis();
        String rendered = offerTemplateService.renderTemplate(template, testProject);
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] renderTemplate (100KB HTML): {}ms ({}s) | rendered length: {}", 
                   duration, duration / 1000.0, rendered != null ? rendered.length() : 0);

        assertNotNull(rendered);
        assertTrue(duration < 5000, "Renderowanie powinno zakończyć się w ciągu 5 sekund");
    }

    @Test
    void testSaveTemplate_Performance_ManySaves() {
        logger.info("🧪 TEST WYDAJNOŚCIOWY: saveTemplate - wiele zapisów (100)");

        long operationStart = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            OfferTemplate template = new OfferTemplate();
            template.setName("Template " + i);
            template.setDescription("Description " + i);
            template.setHtmlContent("<h1>Template " + i + "</h1>");
            template.setIsDefault(false);
            offerTemplateService.saveTemplate(template);
        }
        long operationEnd = System.currentTimeMillis();

        long duration = operationEnd - operationStart;
        logger.info("⏱️ [PERFORMANCE] saveTemplate (100 zapisów): {}ms ({}s)", 
                   duration, duration / 1000.0);

        assertTrue(duration < 10000, "100 zapisów powinno zakończyć się w ciągu 10 sekund");
    }

    // ========== TESTY PRZYPADKÓW BRZEGOWYCH ==========

    @Test
    void testSaveTemplate_NullFields() {
        logger.info("🧪 TEST BRZEGOWY: saveTemplate - null fields (name jest NOT NULL)");

        OfferTemplate template = new OfferTemplate();
        template.setName(null); // Kolumna name jest NOT NULL w bazie
        template.setDescription(null);
        template.setHtmlContent(null);
        template.setCssContent(null);
        template.setIsDefault(false);

        // Kolumna name jest NOT NULL, więc powinien rzucić wyjątek
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            offerTemplateService.saveTemplate(template);
        });

        logger.info("⏱️ [PERFORMANCE] saveTemplate (null fields): wyjątek rzucony poprawnie");
    }

    @Test
    void testRenderTemplate_NullProject() {
        logger.info("🧪 TEST BRZEGOWY: renderTemplate - null project");

        OfferTemplate template = new OfferTemplate();
        template.setHtmlContent("<h1>Test</h1>");

        // Sprawdź czy metoda obsługuje null project (może rzucić wyjątek lub zwrócić pusty HTML)
        assertThrows(Exception.class, () -> {
            offerTemplateService.renderTemplate(template, null);
        });

        logger.info("⏱️ [PERFORMANCE] renderTemplate (null project): wyjątek rzucony poprawnie");
    }

    @Test
    void testSetDefaultTemplate_MultipleCalls() {
        logger.info("🧪 TEST BRZEGOWY: setDefaultTemplate - wielokrotne wywołania");

        // Utwórz kilka szablonów
        OfferTemplate template1 = new OfferTemplate();
        template1.setName("Template 1");
        template1.setIsDefault(false);
        template1 = offerTemplateRepository.save(template1);

        OfferTemplate template2 = new OfferTemplate();
        template2.setName("Template 2");
        template2.setIsDefault(false);
        template2 = offerTemplateRepository.save(template2);

        // Ustaw template1 jako domyślny
        offerTemplateService.setDefaultTemplate(template1.getId());
        assertTrue(offerTemplateRepository.findById(template1.getId()).get().getIsDefault());

        // Ustaw template2 jako domyślny
        offerTemplateService.setDefaultTemplate(template2.getId());
        assertTrue(offerTemplateRepository.findById(template2.getId()).get().getIsDefault());
        assertFalse(offerTemplateRepository.findById(template1.getId()).get().getIsDefault());

        logger.info("⏱️ [PERFORMANCE] setDefaultTemplate (multiple calls): zakończone poprawnie");
    }
}

