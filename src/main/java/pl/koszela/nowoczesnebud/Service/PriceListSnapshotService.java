package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshot;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshotItem;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Repository.PriceListSnapshotItemRepository;
import pl.koszela.nowoczesnebud.Repository.PriceListSnapshotRepository;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serwis do zarządzania snapshotami cenników
 * Snapshoty przechowują stan cennika z określonej daty
 */
@Service
public class PriceListSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(PriceListSnapshotService.class);

    private final PriceListSnapshotRepository snapshotRepository;
    private final PriceListSnapshotItemRepository snapshotItemRepository;
    private final ProductRepository productRepository;
    private final ProjectRepository projectRepository;

    public PriceListSnapshotService(
            PriceListSnapshotRepository snapshotRepository,
            PriceListSnapshotItemRepository snapshotItemRepository,
            ProductRepository productRepository,
            ProjectRepository projectRepository) {
        this.snapshotRepository = snapshotRepository;
        this.snapshotItemRepository = snapshotItemRepository;
        this.productRepository = productRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Utwórz nowy snapshot z aktualnego stanu cennika dla kategorii
     */
    @Transactional
    public PriceListSnapshot createSnapshotForDate(LocalDateTime date, ProductCategory category) {
        logger.info("📸 Tworzenie snapshotu dla kategorii {} i daty {}", category, date);
        
        // Sprawdź czy snapshot już istnieje dla tej daty i kategorii
        Optional<PriceListSnapshot> existing = snapshotRepository.findByCategoryAndDate(category, date);
        if (existing.isPresent()) {
            logger.info("  Snapshot już istnieje dla kategorii {} i daty {} - pomijam tworzenie", category, date);
            return existing.get();
        }

        // Pobierz wszystkie produkty z kategorii
        List<Product> products = productRepository.findByCategory(category);
        logger.info("  Znaleziono {} produktów w kategorii {}", products.size(), category);

        // Utwórz snapshot
        PriceListSnapshot newSnapshot = new PriceListSnapshot();
        newSnapshot.setSnapshotDate(date);
        newSnapshot.setCategory(category);
        PriceListSnapshot snapshot = snapshotRepository.save(newSnapshot);
        logger.info("  ✅ Utworzono snapshot ID: {}", snapshot.getId());

        // Utwórz pozycje snapshotu dla każdego produktu
        final PriceListSnapshot finalSnapshot = snapshot; // final dla stream
        List<PriceListSnapshotItem> items = products.stream()
                .map(product -> createSnapshotItem(finalSnapshot, product))
                .collect(Collectors.toList());

        snapshotItemRepository.saveAll(items);
        snapshot.setItems(items);

        logger.info("  ✅ Utworzono {} pozycji w snapshotcie", items.size());
        return snapshot;
    }

    /**
     * Utwórz pozycję snapshotu z produktu
     */
    private PriceListSnapshotItem createSnapshotItem(PriceListSnapshot snapshot, Product product) {
        PriceListSnapshotItem item = new PriceListSnapshotItem();
        item.setPriceListSnapshot(snapshot);
        item.setProductId(product.getId());
        item.setName(product.getName());
        item.setManufacturer(product.getManufacturer());
        item.setGroupName(product.getGroupName());
        item.setCategory(product.getCategory());
        item.setMapperName(product.getMapperName());
        item.setRetailPrice(product.getRetailPrice());
        item.setPurchasePrice(product.getPurchasePrice());
        item.setSellingPrice(product.getSellingPrice());
        item.setBasicDiscount(product.getBasicDiscount() != null ? product.getBasicDiscount() : 0);
        item.setPromotionDiscount(product.getPromotionDiscount() != null ? product.getPromotionDiscount() : 0);
        item.setAdditionalDiscount(product.getAdditionalDiscount() != null ? product.getAdditionalDiscount() : 0);
        item.setSkontoDiscount(product.getSkontoDiscount() != null ? product.getSkontoDiscount() : 0);
        item.setMarginPercent(product.getMarginPercent() != null ? product.getMarginPercent() : 0.0);
        item.setUnit(product.getUnit());
        item.setQuantityConverter(product.getQuantityConverter() != null ? product.getQuantityConverter() : 1.0);
        item.setIsMainOption(product.getIsMainOption());
        
        // Kopiuj accessoryType (dla ACCESSORY)
        if (product.getCategory() == ProductCategory.ACCESSORY) {
            item.setAccessoryType(product.getAccessoryType());
        }
        
        return item;
    }

    /**
     * Znajdź snapshot dla daty lub najbliższy wcześniejszy
     * Używane przez projekty - szukają snapshotu dla daty utworzenia projektu
     */
    public Optional<PriceListSnapshot> findSnapshotForDate(LocalDateTime date, ProductCategory category) {
        // Najpierw spróbuj znaleźć dokładną datę
        Optional<PriceListSnapshot> exact = snapshotRepository.findByCategoryAndDate(category, date);
        if (exact.isPresent()) {
            return exact;
        }

        // Jeśli nie ma dokładnej daty, znajdź najbliższy wcześniejszy
        List<PriceListSnapshot> earlier = snapshotRepository.findClosestEarlierSnapshot(category, date);
        if (!earlier.isEmpty()) {
            return Optional.of(earlier.get(0)); // Pierwszy to najbliższy (posortowane DESC)
        }

        return Optional.empty();
    }

    /**
     * Pobierz dokładny snapshot dla daty i kategorii
     */
    public Optional<PriceListSnapshot> getSnapshotByDateAndCategory(LocalDateTime date, ProductCategory category) {
        return snapshotRepository.findByCategoryAndDate(category, date);
    }

    /**
     * Pobierz wszystkie snapshoty dla kategorii
     */
    public List<PriceListSnapshot> getSnapshotsByCategory(ProductCategory category) {
        return snapshotRepository.findByCategory(category);
    }

    /**
     * Pobierz snapshot z załadowanymi pozycjami
     */
    public Optional<PriceListSnapshot> getSnapshotWithItems(Long snapshotId) {
        return snapshotRepository.findByIdWithItems(snapshotId);
    }

    /**
     * Pobierz pozycje snapshotu
     */
    public List<PriceListSnapshotItem> getSnapshotItems(Long snapshotId) {
        return snapshotItemRepository.findBySnapshotId(snapshotId);
    }

    /**
     * Usuwa nieużywane snapshoty (nie przypisane do żadnego projektu)
     * Snapshot jest używany jeśli istnieje projekt dla którego findSnapshotForDate zwróci ten snapshot
     * @param excludeSnapshotIds Lista ID snapshotów do wykluczenia z usuwania (np. nowo utworzone)
     */
    @Transactional
    public void deleteUnusedSnapshots(List<Long> excludeSnapshotIds) {
        List<PriceListSnapshot> allSnapshots = snapshotRepository.findAll();
        List<PriceListSnapshot> toDelete = new ArrayList<>();
        
        // Pobierz wszystkie projekty z snapshotDate
        List<Project> projects = projectRepository.findAll().stream()
                .filter(p -> p.getSnapshotDate() != null)
                .collect(Collectors.toList());
        
        for (PriceListSnapshot snapshot : allSnapshots) {
            // Wyklucz snapshoty z listy wykluczeń (np. nowo utworzone)
            if (excludeSnapshotIds != null && excludeSnapshotIds.contains(snapshot.getId())) {
                continue;
            }
            
            boolean isUsed = false;
            
            // Sprawdź czy istnieje projekt dla którego ten snapshot zostałby użyty
            for (Project project : projects) {
                Optional<PriceListSnapshot> usedSnapshot = findSnapshotForDate(
                    project.getSnapshotDate(), 
                    snapshot.getCategory()
                );
                
                // Jeśli findSnapshotForDate zwróci ten snapshot, to jest używany
                if (usedSnapshot.isPresent() && usedSnapshot.get().getId().equals(snapshot.getId())) {
                    isUsed = true;
                    break;
                }
            }
            
            if (!isUsed) {
                toDelete.add(snapshot);
            }
        }
        
        if (!toDelete.isEmpty()) {
            // Usuń snapshoty - orphanRemoval automatycznie usunie pozycje (PriceListSnapshotItem)
            snapshotRepository.deleteAll(toDelete);
            logger.info("🗑️ Usunięto {} nieużywanych snapshotów", toDelete.size());
        }
    }
    
    /**
     * Usuwa nieużywane snapshoty (bez wykluczeń)
     */
    @Transactional
    public void deleteUnusedSnapshots() {
        deleteUnusedSnapshots(null);
    }
}

