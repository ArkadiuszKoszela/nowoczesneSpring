package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.DTO.BulkDiscountRequest;
import pl.koszela.nowoczesnebud.DTO.DiscountUpdateRequest;
import pl.koszela.nowoczesnebud.DTO.GroupOptionRequest;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Service.ProductExportService;
import pl.koszela.nowoczesnebud.Service.ProductService;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kontroler produktów - zastępuje TilesController + GuttersController + AccessoriesController
 * CORS zarządzany globalnie przez WebConfig
 */
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    private final ProductService productService;
    private final ProductExportService productExportService;

    public ProductController(ProductService productService, ProductExportService productExportService) {
        this.productService = productService;
        this.productExportService = productExportService;
    }

    /**
     * Pobierz produkty według kategorii
     * GET /api/products?category=TILE
     */
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) ProductCategory category) {
        
        if (category == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Product> products = productService.getAllProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * Pobierz pojedynczy produkt po ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        logger.debug("Pobieranie produktu ID: {}", id);
        
        return productService.getProductById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Pobierz producentów
     * GET /api/products/manufacturers?category=TILE
     */
    @GetMapping("/manufacturers")
    public ResponseEntity<List<String>> getManufacturers(
            @RequestParam ProductCategory category) {
        return ResponseEntity.ok(productService.getManufacturers(category));
    }

    /**
     * Pobierz grupy produktowe
     * GET /api/products/groups?category=TILE&manufacturer=CANTUS
     */
    @GetMapping("/groups")
    public ResponseEntity<List<String>> getGroups(
            @RequestParam ProductCategory category,
            @RequestParam String manufacturer) {
        return ResponseEntity.ok(productService.getGroupNames(category, manufacturer));
    }

    /**
     * Pobierz słownik sugestii atrybutów dla autouzupełniania
     * GET /api/products/attribute-suggestions?category=TILE
     * 
     * Zwraca mapę: {"kolor": ["czerwony","brązowy","czarny"], "kształt": ["płaska","karpiówka"]}
     * - Parsuje attributes JSON ze wszystkich GRUP PRODUKTOWYCH danej kategorii
     * - Zbiera unikalne klucze i wartości atrybutów
     */
    @GetMapping("/attribute-suggestions")
    public ResponseEntity<Map<String, List<String>>> getAttributeSuggestions(
            @RequestParam ProductCategory category) {
        
        logger.debug("Pobieranie słownika atrybutów dla kategorii: {}", category);
        
        try {
            Map<String, List<String>> suggestions = productService.getAttributeSuggestions(category);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania słownika atrybutów dla kategorii {}: {}", category, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pobierz atrybuty dla konkretnej grupy produktowej
     * GET /api/products/group-attributes?category=TILE&manufacturer=CANTUS&groupName=Nuance
     * 
     * Zwraca: {"attributes": "{\"kolor\":[\"czerwony\"],\"kształt\":[\"płaska\"]}"}
     * lub {"attributes": null} jeśli grupa nie ma atrybutów (200 OK z pustym obiektem, nie 404)
     */
    @GetMapping("/group-attributes")
    public ResponseEntity<Map<String, String>> getGroupAttributes(
            @RequestParam ProductCategory category,
            @RequestParam String manufacturer,
            @RequestParam String groupName) {
        
        logger.debug("Pobieranie atrybutów dla grupy: {}/{}/{}", category, manufacturer, groupName);
        
        try {
            String attributes = productService.getGroupAttributes(category, manufacturer, groupName);
            Map<String, String> response = new HashMap<>();
            
            if (attributes != null) {
                response.put("attributes", attributes);
            } else {
                // Zwróć 200 OK z null zamiast 404 - brak atrybutów to normalny stan
                response.put("attributes", null);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania atrybutów dla grupy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Zapisz/zaktualizuj atrybuty dla grupy produktowej
     * PUT /api/products/group-attributes
     * 
     * Request Body:
     * {
     *   "category": "TILE",
     *   "manufacturer": "CANTUS",
     *   "groupName": "Nuance",
     *   "attributes": {"kolor":["czerwony","brązowy"],"kształt":["płaska"]}
     * }
     */
    @PutMapping("/group-attributes")
    public ResponseEntity<?> saveGroupAttributes(@RequestBody @Valid pl.koszela.nowoczesnebud.DTO.GroupAttributesRequest request) {
        logger.debug("Zapisywanie atrybutów dla grupy: {}/{}/{}", request.getCategory(), request.getManufacturer(), request.getGroupName());
        
        try {
            productService.saveGroupAttributes(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Błąd podczas zapisywania atrybutów dla grupy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Import produktów z nazwami, producentami i grupami
     * POST /api/products/import-with-names?category=TILE
     * ZASTĘPUJE: /api/tiles/importWithNames
     */
    @PostMapping("/import-with-names")
    public ResponseEntity<List<Product>> importProductsWithNames(
            @RequestParam("file[]") MultipartFile[] files,
            @RequestParam("name[]") String[] names,
            @RequestParam(value = "manufacturer[]", required = false) String[] manufacturers,
            @RequestParam(value = "groupName[]", required = false) String[] groupNames,
            @RequestParam ProductCategory category) {
        
        logger.info("📥 Import produktów - kategoria: {}, plików: {}", category, files.length);
        if (manufacturers != null) {
            logger.info("   → Producenci z frontendu: {}", Arrays.toString(manufacturers));
        } else {
            logger.info("   → Producenci z frontendu: BRAK (null)");
        }
        if (groupNames != null) {
            logger.info("   → Grupy z frontendu: {}", Arrays.toString(groupNames));
        } else {
            logger.info("   → Grupy z frontendu: BRAK (null)");
        }
        
        if (files.length != names.length) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<MultipartFile> fileList = Arrays.asList(files);
            List<String> nameList = Arrays.asList(names);
            List<String> manufacturerList = manufacturers != null ? Arrays.asList(manufacturers) : null;
            List<String> groupNameList = groupNames != null ? Arrays.asList(groupNames) : null;
            
            List<Product> products = productService.importProductsWithCustomNames(
                fileList, nameList, manufacturerList, groupNameList, category
            );
            return ResponseEntity.ok(products);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Wypełnij ilości na podstawie inputów
     * POST /api/products/fill-quantities?category=TILE
     * ZASTĘPUJE: /api/tiles/map
     */
    @PostMapping("/fill-quantities")
    public ResponseEntity<List<Product>> fillQuantities(
            @RequestBody List<Input> inputList,
            @RequestParam ProductCategory category) {
        // ⏱️ PERFORMANCE LOG: Start kontrolera "Przelicz produkty"
        long startTime = System.currentTimeMillis();
        logger.info("⏱️ [Przelicz produkty] POST /fill-quantities - START (kategoria: {}, inputów: {})", category, inputList.size());
        
        List<Product> products = productService.fillProductQuantities(inputList, category);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("⏱️ [Przelicz produkty] POST /fill-quantities - END: {} produktów w {}ms", products.size(), duration);
        return ResponseEntity.ok(products);
    }

    /**
     * Aktualizuj cały produkt
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        
        if (!id.equals(product.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Product savedProduct = productService.saveProduct(product);
        return ResponseEntity.ok(savedProduct);
    }

    /**
     * Aktualizuj rabaty produktu
     * PUT /api/products/{id}/discounts
     */
    @PutMapping("/{id}/discounts")
    public ResponseEntity<Product> updateDiscounts(
            @PathVariable Long id,
            @Valid @RequestBody DiscountUpdateRequest request) {
        
        logger.info("Aktualizacja rabatów dla produktu ID: {}", id);
        
        Product product = productService.updateProductDiscounts(
            id,
            request.getBasicDiscount(),
            request.getPromotionDiscount(),
            request.getAdditionalDiscount(),
            request.getSkontoDiscount(),
            request.getDiscountCalculationMethod()
        );

        if (product == null) {
            logger.warn("Produkt o ID {} nie został znaleziony", id);
            return ResponseEntity.notFound().build();
        }

        logger.info("Rabaty zaktualizowane pomyślnie dla produktu ID: {}", id);
        return ResponseEntity.ok(product);
    }

    /**
     * Oblicz marżę
     * POST /api/products/calculate-margin?category=TILE
     */
    @PostMapping("/calculate-margin")
    public ResponseEntity<List<Product>> calculateMargin(
            @RequestBody Integer marginPercent,
            @RequestParam ProductCategory category) {
        
        List<Product> products = productService.calculateMargin(marginPercent, category);
        return ResponseEntity.ok(products);
    }

    /**
     * Oblicz rabat
     * POST /api/products/calculate-discount?category=TILE
     */
    @PostMapping("/calculate-discount")
    public ResponseEntity<List<Product>> calculateDiscount(
            @RequestBody Integer discountPercent,
            @RequestParam ProductCategory category) {
        
        List<Product> products = productService.calculateDiscount(discountPercent, category);
        return ResponseEntity.ok(products);
    }

    /**
     * Ustaw opcję (Główna/Opcjonalna/Brak) dla CAŁEJ GRUPY produktów
     * POST /api/products/set-group-option
     */
    @PostMapping("/set-group-option")
    public ResponseEntity<List<Product>> setGroupOption(@Valid @RequestBody GroupOptionRequest request) {
        logger.info("setGroupOption dla grupy: {} / {}", request.getManufacturer(), request.getGroupName());
        logger.debug("Kategoria: {}, isMainOption: {}", request.getCategory(), request.getIsMainOption());
        
        List<Product> updatedProducts = productService.setGroupOption(
            request.getCategory(),
            request.getManufacturer(),
            request.getGroupName(),
            request.getIsMainOption()
        );
        
        logger.info("Zaktualizowano {} produktów", updatedProducts.size());
        return ResponseEntity.ok(updatedProducts);
    }

    /**
     * BATCH UPDATE - aktualizuj wiele produktów naraz
     * PUT /api/products/batch
     */
    @PutMapping("/batch")
    public ResponseEntity<List<Product>> updateProductsBatch(@RequestBody List<Product> products) {
        logger.info("📦 Batch update request: {} produktów", products.size());
        
        try {
            List<Product> savedProducts = productService.updateProductsBatch(products);
            return ResponseEntity.ok(savedProducts);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Błąd walidacji: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * BULK DISCOUNT UPDATE - ustaw rabaty dla całej grupy
     * POST /api/products/bulk-discount
     */
    @PostMapping("/bulk-discount")
    public ResponseEntity<List<Product>> updateBulkDiscounts(@Valid @RequestBody BulkDiscountRequest request) {
        logger.info("🎯 Bulk discount update: {} / {}", request.getManufacturer(), request.getGroupName());
        
        List<Product> updatedProducts = productService.updateGroupDiscounts(
            request.getCategory(),
            request.getManufacturer(),
            request.getGroupName(),
            request.getBasicDiscount(),
            request.getAdditionalDiscount(),
            request.getPromotionDiscount(),
            request.getSkontoDiscount(),
            request.getProductType(),
            request.getDiscountCalculationMethod() // Metoda obliczania rabatu
        );
        
        return ResponseEntity.ok(updatedProducts);
    }

    /**
     * Usuń wszystkie produkty producenta (reset cennika)
     * DELETE /api/products/manufacturer?category=TILE&manufacturer=CANTUS
     */
    @DeleteMapping("/manufacturer")
    public ResponseEntity<Void> deleteAllByManufacturer(
            @RequestParam ProductCategory category,
            @RequestParam String manufacturer) {
        
        logger.warn("🗑️ Request usunięcia wszystkich produktów: {} / {}", category, manufacturer);
        productService.deleteAllByManufacturer(category, manufacturer);
        return ResponseEntity.ok().build();
    }

    /**
     * Usuń wszystkie produkty grupy produktowej
     * DELETE /api/products/group?category=TILE&manufacturer=CANTUS&groupName=NUANE
     */
    @DeleteMapping("/group")
    public ResponseEntity<Void> deleteAllByGroup(
            @RequestParam ProductCategory category,
            @RequestParam String manufacturer,
            @RequestParam String groupName) {
        
        logger.warn("🗑️ Request usunięcia grupy produktów: {} / {} / {}", category, manufacturer, groupName);
        productService.deleteAllByGroup(category, manufacturer, groupName);
        return ResponseEntity.ok().build();
    }

    /**
     * Usuń wszystkie produkty dla kategorii (dla testów E2E)
     * DELETE /api/products/all?category=TILE
     */
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllByCategory(@RequestParam ProductCategory category) {
        logger.info("🗑️ Usuwanie wszystkich produktów dla kategorii: {}", category);
        productService.deleteAllByCategory(category);
        logger.info("✅ Wszystkie produkty kategorii {} zostały usunięte", category);
        return ResponseEntity.noContent().build();
    }

    /**
     * Usuń pojedynczy produkt
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("🗑️ Usuwanie produktu ID: {}", id);
        
        if (productService.getProductById(id).isEmpty()) {
            logger.warn("⚠️ Produkt ID {} nie istnieje w bazie", id);
            return ResponseEntity.notFound().build();
        }
        
        // Usuń produkt z bazy
        productService.deleteProductById(id);
        logger.info("✅ Produkt ID {} został usunięty z bazy", id);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Usuń wiele produktów jednocześnie (batch delete)
     * DELETE /api/products/batch
     * Body: List<Long> - lista ID produktów do usunięcia
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteProductsBatch(@RequestBody List<Long> productIds) {
        logger.info("🗑️ Batch delete: usuwanie {} produktów", productIds.size());
        
        if (productIds == null || productIds.isEmpty()) {
            logger.warn("⚠️ Pusta lista ID produktów");
            return ResponseEntity.badRequest().build();
        }
        
        Map<String, Object> result = productService.deleteProductsByIds(productIds);
        
        int deletedCount = (Integer) result.get("deletedCount");
        int notFoundCount = (Integer) result.get("notFoundCount");
        
        logger.info("✅ Batch delete zakończony: usunięto {}, nie znaleziono {}", deletedCount, notFoundCount);
        
        return ResponseEntity.ok(result);
    }

    /**
     * BULK RENAME MANUFACTURER - zmień nazwę producenta dla wszystkich jego produktów
     * PUT /api/products/rename-manufacturer
     */
    @PutMapping("/rename-manufacturer")
    public ResponseEntity<List<Product>> renameManufacturer(
            @RequestParam ProductCategory category,
            @RequestParam String oldManufacturer,
            @RequestParam String newManufacturer) {
        
        logger.info("📝 Bulk rename manufacturer: {} → {}", oldManufacturer, newManufacturer);
        
        List<Product> updatedProducts = productService.renameManufacturer(
            category, oldManufacturer, newManufacturer
        );
        
        return ResponseEntity.ok(updatedProducts);
    }

    /**
     * BULK RENAME GROUP - zmień nazwę grupy dla wszystkich produktów w tej grupie
     * PUT /api/products/rename-group
     */
    @PutMapping("/rename-group")
    public ResponseEntity<List<Product>> renameGroup(
            @RequestParam ProductCategory category,
            @RequestParam String manufacturer,
            @RequestParam String oldGroupName,
            @RequestParam String newGroupName) {
        
        logger.info("📝 Bulk rename group: {} / {} → {}", 
                   manufacturer, oldGroupName, newGroupName);
        
        List<Product> updatedProducts = productService.renameGroup(
            category, manufacturer, oldGroupName, newGroupName
        );
        
        return ResponseEntity.ok(updatedProducts);
    }

    /**
     * Eksport produktów do Excel (ZIP z plikami Excel)
     * GET /api/products/export?category=TILE
     * 
     * Zwraca ZIP z plikami Excel, każdy plik = jedna grupa produktów (Manufacturer-GroupName.xlsx)
     * Format zgodny z importem - można zaimportować z powrotem
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam ProductCategory category) {
        logger.info("📤 Eksport produktów dla kategorii: {}", category);
        
        try {
            // Pobierz wszystkie produkty dla kategorii
            List<Product> products = productService.getAllProductsByCategory(category);
            
            if (products.isEmpty()) {
                logger.warn("Brak produktów do eksportu dla kategorii: {}", category);
                return ResponseEntity.noContent().build();
            }
            
            logger.info("Eksportowanie {} produktów", products.size());
            
            // Eksportuj do ZIP
            byte[] zipBytes = productExportService.exportToExcelZip(products);
            
            // Przygotuj nagłówki HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            String fileName = "cenniki_" + category.name().toLowerCase() + ".zip";
            headers.setContentDispositionFormData("attachment", fileName);
            
            logger.info("✅ Eksport zakończony: {} bajtów", zipBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);
                    
        } catch (IOException e) {
            logger.error("❌ Błąd eksportu do Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            logger.error("❌ Błąd walidacji eksportu: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

