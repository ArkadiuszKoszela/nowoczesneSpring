package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshot;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshotItem;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Service.PriceListSnapshotService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Kontroler do zarządzania snapshotami cenników
 * Snapshoty są tworzone przy imporcie cennika lub ręcznie przez admina
 */
@RestController
@RequestMapping("/api/price-list-snapshots")
public class PriceListSnapshotController {

    private static final Logger logger = LoggerFactory.getLogger(PriceListSnapshotController.class);
    
    private final PriceListSnapshotService priceListSnapshotService;

    public PriceListSnapshotController(PriceListSnapshotService priceListSnapshotService) {
        this.priceListSnapshotService = priceListSnapshotService;
    }

    /**
     * Ręczne utworzenie snapshotu z aktualnego stanu cennika dla kategorii
     * POST /api/price-list-snapshots/create?category=TILE
     */
    @PostMapping("/create")
    public ResponseEntity<PriceListSnapshot> createSnapshot(
            @RequestParam ProductCategory category) {
        
        logger.info("📸 Ręczne tworzenie snapshotu dla kategorii {}", category);
        
        try {
            PriceListSnapshot snapshot = priceListSnapshotService.createSnapshotForDate(
                LocalDateTime.now(), category
            );
            
            logger.info("✅ Snapshot utworzony: ID={}, kategoria={}, data={}", 
                       snapshot.getId(), snapshot.getCategory(), snapshot.getSnapshotDate());
            
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            logger.error("❌ Błąd tworzenia snapshotu dla kategorii {}: {}", category, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Utwórz snapshoty dla wszystkich kategorii
     * POST /api/price-list-snapshots/create-all
     */
    @PostMapping("/create-all")
    public ResponseEntity<Map<String, Object>> createAllSnapshots() {
        logger.info("📸 Tworzenie snapshotów dla wszystkich kategorii");
        
        int created = 0;
        int errors = 0;
        
        for (ProductCategory category : ProductCategory.values()) {
            try {
                priceListSnapshotService.createSnapshotForDate(LocalDateTime.now(), category);
                created++;
                logger.info("✅ Snapshot utworzony dla kategorii {}", category);
            } catch (Exception e) {
                errors++;
                logger.error("❌ Błąd tworzenia snapshotu dla kategorii {}: {}", category, e.getMessage(), e);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "created", created,
            "errors", errors,
            "totalCategories", ProductCategory.values().length
        ));
    }

    /**
     * Pobierz listę snapshotów dla kategorii
     * GET /api/price-list-snapshots?category=TILE
     */
    @GetMapping
    public ResponseEntity<List<PriceListSnapshot>> getSnapshots(
            @RequestParam ProductCategory category) {
        
        List<PriceListSnapshot> snapshots = priceListSnapshotService.getSnapshotsByCategory(category);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Pobierz produkty ze snapshotu
     * GET /api/price-list-snapshots/{id}/products
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<List<PriceListSnapshotItem>> getSnapshotProducts(
            @PathVariable Long id) {
        
        List<PriceListSnapshotItem> items = priceListSnapshotService.getSnapshotItems(id);
        return ResponseEntity.ok(items);
    }

    /**
     * Znajdź snapshot dla daty i kategorii
     * GET /api/price-list-snapshots/for-date?date=2024-01-15T10:00:00&category=TILE
     */
    @GetMapping("/for-date")
    public ResponseEntity<PriceListSnapshot> getSnapshotForDate(
            @RequestParam String date,
            @RequestParam ProductCategory category) {
        
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(date);
            return priceListSnapshotService.getSnapshotByDateAndCategory(localDateTime, category)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Błąd parsowania daty {}: {}", date, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}








