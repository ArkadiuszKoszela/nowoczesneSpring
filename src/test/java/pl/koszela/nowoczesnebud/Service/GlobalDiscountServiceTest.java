package pl.koszela.nowoczesnebud.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount.DiscountType;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.GlobalDiscountRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ TESTY POPRAWNOŚCIOWE DLA RABATÓW GLOBALNYCH
 * 
 * Testuje operacje CRUD na rabatach globalnych:
 * - createDiscount() - tworzenie rabatów
 * - updateDiscount() - aktualizacja rabatów
 * - activateDiscount() / deactivateDiscount() - aktywacja/dezaktywacja
 * - deleteDiscount() - usuwanie rabatów
 * - getCurrentMainDiscount() / getCurrentOptionalDiscount() - pobieranie aktualnych rabatów
 * 
 * Testuje:
 * - Poprawność operacji CRUD
 * - Edge cases (duplikaty, nieistniejące rabaty, daty ważności)
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class GlobalDiscountServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDiscountServiceTest.class);

    @Autowired
    private GlobalDiscountService globalDiscountService;

    @Autowired
    private GlobalDiscountRepository globalDiscountRepository;

    @BeforeEach
    void setUp() {
        // Wyczyść rabaty przed testem
        globalDiscountRepository.deleteAll();
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - createDiscount()
    // ==========================================

    @Test
    void testCreateDiscount_StandardCase() {
        logger.info("🧪 TEST: createDiscount - standardowy przypadek");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        discount.setValidFrom(LocalDate.now());
        discount.setValidTo(LocalDate.now().plusMonths(1));
        discount.setActive(true);
        
        GlobalDiscount created = globalDiscountService.createDiscount(discount);
        
        assertNotNull(created.getId(), "Rabat powinien mieć ID po utworzeniu");
        assertEquals(ProductCategory.TILE, created.getCategory());
        assertEquals(DiscountType.MAIN, created.getType());
        assertEquals(25.0, created.getDiscountPercent());
        assertTrue(created.getActive());
    }

    @Test
    void testCreateDiscount_DeactivatesExistingActive() {
        logger.info("🧪 TEST: createDiscount - dezaktywuje istniejący aktywny rabat");
        
        // Utwórz pierwszy aktywny rabat
        GlobalDiscount discount1 = new GlobalDiscount();
        discount1.setCategory(ProductCategory.TILE);
        discount1.setType(DiscountType.MAIN);
        discount1.setDiscountPercent(20.0);
        discount1.setValidFrom(LocalDate.now());
        discount1.setValidTo(LocalDate.now().plusMonths(1));
        discount1.setActive(true);
        globalDiscountService.createDiscount(discount1);
        
        // Utwórz drugi aktywny rabat tego samego typu
        GlobalDiscount discount2 = new GlobalDiscount();
        discount2.setCategory(ProductCategory.TILE);
        discount2.setType(DiscountType.MAIN);
        discount2.setDiscountPercent(30.0);
        discount2.setValidFrom(LocalDate.now());
        discount2.setValidTo(LocalDate.now().plusMonths(1));
        discount2.setActive(true);
        GlobalDiscount created2 = globalDiscountService.createDiscount(discount2);
        
        // Sprawdź czy pierwszy został dezaktywowany
        Optional<GlobalDiscount> firstDiscount = globalDiscountRepository.findById(discount1.getId());
        assertTrue(firstDiscount.isPresent());
        assertFalse(firstDiscount.get().getActive(), "Pierwszy rabat powinien być dezaktywowany");
        
        assertTrue(created2.getActive(), "Drugi rabat powinien być aktywny");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - updateDiscount()
    // ==========================================

    @Test
    void testUpdateDiscount_StandardCase() {
        logger.info("🧪 TEST: updateDiscount - standardowy przypadek");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        discount.setValidFrom(LocalDate.now());
        discount.setValidTo(LocalDate.now().plusMonths(1));
        discount.setActive(true);
        GlobalDiscount created = globalDiscountService.createDiscount(discount);
        
        // Zaktualizuj rabat
        created.setDiscountPercent(30.0);
        GlobalDiscount updated = globalDiscountService.updateDiscount(created);
        
        assertEquals(30.0, updated.getDiscountPercent(), "Rabat powinien być zaktualizowany");
        assertEquals(created.getId(), updated.getId(), "ID powinno pozostać takie samo");
    }

    @Test
    void testUpdateDiscount_NonExistent() {
        logger.info("🧪 TEST: updateDiscount - nieistniejący rabat");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setId(99999L);
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        
        assertThrows(IllegalArgumentException.class, () -> {
            globalDiscountService.updateDiscount(discount);
        }, "Powinno rzucić IllegalArgumentException dla nieistniejącego rabatu");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - activateDiscount() / deactivateDiscount()
    // ==========================================

    @Test
    void testActivateDeactivateDiscount() {
        logger.info("🧪 TEST: activateDiscount / deactivateDiscount");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        discount.setValidFrom(LocalDate.now());
        discount.setValidTo(LocalDate.now().plusMonths(1));
        discount.setActive(false);
        GlobalDiscount created = globalDiscountService.createDiscount(discount);
        
        // Dezaktywuj
        globalDiscountService.deactivateDiscount(created.getId());
        Optional<GlobalDiscount> deactivated = globalDiscountService.getDiscountById(created.getId());
        assertTrue(deactivated.isPresent());
        assertFalse(deactivated.get().getActive(), "Rabat powinien być dezaktywowany");
        
        // Aktywuj
        GlobalDiscount activated = globalDiscountService.activateDiscount(created.getId());
        assertTrue(activated.getActive(), "Rabat powinien być aktywny");
    }

    @Test
    void testActivateDiscount_DeactivatesOtherActive() {
        logger.info("🧪 TEST: activateDiscount - dezaktywuje inny aktywny rabat");
        
        // Utwórz pierwszy aktywny rabat
        GlobalDiscount discount1 = new GlobalDiscount();
        discount1.setCategory(ProductCategory.TILE);
        discount1.setType(DiscountType.MAIN);
        discount1.setDiscountPercent(20.0);
        discount1.setValidFrom(LocalDate.now());
        discount1.setValidTo(LocalDate.now().plusMonths(1));
        discount1.setActive(true);
        GlobalDiscount created1 = globalDiscountService.createDiscount(discount1);
        
        // Utwórz drugi nieaktywny rabat
        GlobalDiscount discount2 = new GlobalDiscount();
        discount2.setCategory(ProductCategory.TILE);
        discount2.setType(DiscountType.MAIN);
        discount2.setDiscountPercent(30.0);
        discount2.setValidFrom(LocalDate.now());
        discount2.setValidTo(LocalDate.now().plusMonths(1));
        discount2.setActive(false);
        GlobalDiscount created2 = globalDiscountService.createDiscount(discount2);
        
        // Aktywuj drugi rabat
        globalDiscountService.activateDiscount(created2.getId());
        
        // Sprawdź czy pierwszy został dezaktywowany
        Optional<GlobalDiscount> firstDiscount = globalDiscountService.getDiscountById(created1.getId());
        assertTrue(firstDiscount.isPresent());
        assertFalse(firstDiscount.get().getActive(), "Pierwszy rabat powinien być dezaktywowany");
        
        // Sprawdź czy drugi jest aktywny
        Optional<GlobalDiscount> secondDiscount = globalDiscountService.getDiscountById(created2.getId());
        assertTrue(secondDiscount.isPresent());
        assertTrue(secondDiscount.get().getActive(), "Drugi rabat powinien być aktywny");
    }

    // ==========================================
    // TESTY POPRAWNOŚCIOWE - getCurrentMainDiscount() / getCurrentOptionalDiscount()
    // ==========================================

    @Test
    void testGetCurrentMainDiscount_StandardCase() {
        logger.info("🧪 TEST: getCurrentMainDiscount - standardowy przypadek");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        discount.setValidFrom(LocalDate.now().minusDays(1));
        discount.setValidTo(LocalDate.now().plusMonths(1));
        discount.setActive(true);
        globalDiscountService.createDiscount(discount);
        
        Optional<GlobalDiscount> current = globalDiscountService.getCurrentMainDiscount(ProductCategory.TILE);
        
        assertTrue(current.isPresent(), "Powinien znaleźć aktualny rabat główny");
        assertEquals(25.0, current.get().getDiscountPercent());
    }

    @Test
    void testGetCurrentMainDiscount_ExpiredDiscount() {
        logger.info("🧪 TEST: getCurrentMainDiscount - wygasły rabat");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        discount.setValidFrom(LocalDate.now().minusMonths(2));
        discount.setValidTo(LocalDate.now().minusDays(1)); // Wygasł wczoraj
        discount.setActive(true);
        globalDiscountService.createDiscount(discount);
        
        Optional<GlobalDiscount> current = globalDiscountService.getCurrentMainDiscount(ProductCategory.TILE);
        
        assertFalse(current.isPresent(), "Nie powinien znaleźć wygasłego rabatu");
    }

    @Test
    void testGetCurrentOptionalDiscount_StandardCase() {
        logger.info("🧪 TEST: getCurrentOptionalDiscount - standardowy przypadek");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.OPTIONAL);
        discount.setDiscountPercent(15.0);
        discount.setValidFrom(LocalDate.now().minusDays(1));
        discount.setValidTo(LocalDate.now().plusMonths(1));
        discount.setActive(true);
        globalDiscountService.createDiscount(discount);
        
        Optional<GlobalDiscount> current = globalDiscountService.getCurrentOptionalDiscount(ProductCategory.TILE);
        
        assertTrue(current.isPresent(), "Powinien znaleźć aktualny rabat opcjonalny");
        assertEquals(15.0, current.get().getDiscountPercent());
    }

    // ==========================================
    // TESTY EDGE CASES
    // ==========================================

    @Test
    void testDeactivateDiscount_NonExistent() {
        logger.info("🧪 TEST: deactivateDiscount - nieistniejący rabat");
        
        assertThrows(IllegalArgumentException.class, () -> {
            globalDiscountService.deactivateDiscount(99999L);
        }, "Powinno rzucić IllegalArgumentException dla nieistniejącego rabatu");
    }

    @Test
    void testActivateDiscount_NonExistent() {
        logger.info("🧪 TEST: activateDiscount - nieistniejący rabat");
        
        assertThrows(IllegalArgumentException.class, () -> {
            globalDiscountService.activateDiscount(99999L);
        }, "Powinno rzucić IllegalArgumentException dla nieistniejącego rabatu");
    }

    @Test
    void testDeleteDiscount_StandardCase() {
        logger.info("🧪 TEST: deleteDiscount - standardowy przypadek");
        
        GlobalDiscount discount = new GlobalDiscount();
        discount.setCategory(ProductCategory.TILE);
        discount.setType(DiscountType.MAIN);
        discount.setDiscountPercent(25.0);
        discount.setValidFrom(LocalDate.now());
        discount.setValidTo(LocalDate.now().plusMonths(1));
        discount.setActive(true);
        GlobalDiscount created = globalDiscountService.createDiscount(discount);
        
        globalDiscountService.deleteDiscount(created.getId());
        
        Optional<GlobalDiscount> deleted = globalDiscountService.getDiscountById(created.getId());
        assertFalse(deleted.isPresent(), "Rabat powinien być usunięty");
    }
}

