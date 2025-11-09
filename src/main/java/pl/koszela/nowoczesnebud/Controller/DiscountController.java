package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Service.GlobalDiscountService;

import javax.validation.Valid;
import java.util.List;

/**
 * Kontroler zarządzający rabatami globalnymi
 * CORS zarządzany globalnie przez WebConfig
 */
@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private static final Logger logger = LoggerFactory.getLogger(DiscountController.class);
    
    private final GlobalDiscountService discountService;

    public DiscountController(GlobalDiscountService discountService) {
        this.discountService = discountService;
    }

    /**
     * Pobierz wszystkie rabaty dla kategorii
     * GET /api/discounts?category=TILE
     */
    @GetMapping
    public ResponseEntity<List<GlobalDiscount>> getAllDiscounts(
            @RequestParam ProductCategory category) {
        
        logger.info("Pobieranie rabatów dla kategorii: {}", category);
        List<GlobalDiscount> discounts = discountService.getDiscountsByCategory(category);
        return ResponseEntity.ok(discounts);
    }

    /**
     * Pobierz aktualnie ważne rabaty dla kategorii
     * GET /api/discounts/current?category=TILE
     */
    @GetMapping("/current")
    public ResponseEntity<List<GlobalDiscount>> getCurrentDiscounts(
            @RequestParam ProductCategory category) {
        
        logger.info("Pobieranie aktualnych rabatów dla kategorii: {}", category);
        List<GlobalDiscount> discounts = discountService.getCurrentDiscounts(category);
        return ResponseEntity.ok(discounts);
    }

    /**
     * Pobierz rabat po ID
     * GET /api/discounts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalDiscount> getDiscount(@PathVariable Long id) {
        logger.info("Pobieranie rabatu ID: {}", id);
        return discountService.getDiscountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Utwórz nowy rabat globalny
     * POST /api/discounts
     */
    @PostMapping
    public ResponseEntity<GlobalDiscount> createDiscount(
            @Valid @RequestBody GlobalDiscount discount) {
        
        logger.info("Tworzenie nowego rabatu: {} dla kategorii: {}", 
                   discount.getType(), discount.getCategory());
        
        GlobalDiscount created = discountService.createDiscount(discount);
        return ResponseEntity.ok(created);
    }

    /**
     * Zaktualizuj istniejący rabat
     * PUT /api/discounts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlobalDiscount> updateDiscount(
            @PathVariable Long id,
            @Valid @RequestBody GlobalDiscount discount) {
        
        logger.info("Aktualizacja rabatu ID: {}", id);
        
        if (!id.equals(discount.getId())) {
            return ResponseEntity.badRequest().build();
        }

        GlobalDiscount updated = discountService.updateDiscount(discount);
        return ResponseEntity.ok(updated);
    }

    /**
     * Dezaktywuj rabat (soft delete)
     * DELETE /api/discounts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateDiscount(@PathVariable Long id) {
        logger.info("Dezaktywacja rabatu ID: {}", id);
        discountService.deactivateDiscount(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Aktywuj rabat
     * POST /api/discounts/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<GlobalDiscount> activateDiscount(@PathVariable Long id) {
        logger.info("Aktywacja rabatu ID: {}", id);
        GlobalDiscount activated = discountService.activateDiscount(id);
        return ResponseEntity.ok(activated);
    }
}














