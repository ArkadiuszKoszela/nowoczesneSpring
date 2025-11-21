package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount.DiscountType;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.GlobalDiscountRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Serwis zarządzający rabatami globalnymi
 */
@Service
public class GlobalDiscountService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDiscountService.class);
    
    private final GlobalDiscountRepository discountRepository;

    public GlobalDiscountService(GlobalDiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    /**
     * Pobierz wszystkie rabaty dla kategorii
     */
    public List<GlobalDiscount> getDiscountsByCategory(ProductCategory category) {
        return discountRepository.findByCategory(category);
    }

    /**
     * Pobierz aktualnie ważne rabaty dla kategorii
     */
    public List<GlobalDiscount> getCurrentDiscounts(ProductCategory category) {
        return discountRepository.findCurrentlyValidDiscounts(category, LocalDate.now());
    }

    /**
     * Pobierz aktualny rabat główny dla kategorii
     */
    public Optional<GlobalDiscount> getCurrentMainDiscount(ProductCategory category) {
        return discountRepository.findCurrentMainDiscount(category, LocalDate.now());
    }

    /**
     * Pobierz aktualny rabat opcjonalny dla kategorii
     */
    public Optional<GlobalDiscount> getCurrentOptionalDiscount(ProductCategory category) {
        return discountRepository.findCurrentOptionalDiscount(category, LocalDate.now());
    }

    /**
     * Pobierz rabat po ID
     */
    public Optional<GlobalDiscount> getDiscountById(Long id) {
        return discountRepository.findById(id);
    }

    /**
     * Utwórz nowy rabat
     */
    @Transactional
    public GlobalDiscount createDiscount(GlobalDiscount discount) {
        logger.info("Tworzenie rabatu: {} dla {}", discount.getType(), discount.getCategory());
        
        // Sprawdź czy nie ma już aktywnego rabatu tego samego typu
        Optional<GlobalDiscount> existing = discountRepository.findByCategoryAndTypeAndActiveTrue(
            discount.getCategory(), 
            discount.getType()
        );
        
        if (existing.isPresent()) {
            logger.warn("Istnieje już aktywny rabat {} dla {}", discount.getType(), discount.getCategory());
            // Możesz albo rzucić wyjątek, albo dezaktywować stary
            // Na razie dezaktywujemy stary
            GlobalDiscount old = existing.get();
            old.setActive(false);
            discountRepository.save(old);
            logger.info("Dezaktywowano stary rabat ID: {}", old.getId());
        }
        
        return discountRepository.save(discount);
    }

    /**
     * Zaktualizuj rabat
     */
    @Transactional
    public GlobalDiscount updateDiscount(GlobalDiscount discount) {
        logger.info("Aktualizacja rabatu ID: {}", discount.getId());
        
        if (!discountRepository.existsById(discount.getId())) {
            throw new IllegalArgumentException("Rabat o ID " + discount.getId() + " nie istnieje");
        }
        
        return discountRepository.save(discount);
    }

    /**
     * Dezaktywuj rabat (soft delete)
     */
    @Transactional
    public void deactivateDiscount(Long id) {
        logger.info("Dezaktywacja rabatu ID: {}", id);
        
        GlobalDiscount discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rabat o ID " + id + " nie istnieje"));
        
        discount.setActive(false);
        discountRepository.save(discount);
        
        logger.info("Rabat ID: {} został dezaktywowany", id);
    }

    /**
     * Aktywuj rabat
     */
    @Transactional
    public GlobalDiscount activateDiscount(Long id) {
        logger.info("Aktywacja rabatu ID: {}", id);
        
        GlobalDiscount discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rabat o ID " + id + " nie istnieje"));
        
        // Sprawdź czy nie ma już innego aktywnego rabatu tego samego typu
        Optional<GlobalDiscount> existing = discountRepository.findByCategoryAndTypeAndActiveTrue(
            discount.getCategory(), 
            discount.getType()
        );
        
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            logger.warn("Istnieje już aktywny rabat {} dla {}", discount.getType(), discount.getCategory());
            // Dezaktywuj stary
            GlobalDiscount old = existing.get();
            old.setActive(false);
            discountRepository.save(old);
        }
        
        discount.setActive(true);
        GlobalDiscount activated = discountRepository.save(discount);
        
        logger.info("Rabat ID: {} został aktywowany", id);
        return activated;
    }

    /**
     * Usuń rabat (hard delete) - tylko dla admina
     */
    @Transactional
    public void deleteDiscount(Long id) {
        logger.warn("Usuwanie rabatu ID: {} (hard delete)", id);
        discountRepository.deleteById(id);
    }
}























