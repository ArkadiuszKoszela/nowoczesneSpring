package pl.koszela.nowoczesnebud.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Service.ProductService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * LEGACY CONTROLLER - Backward compatibility dla starych endpointów
 * Przekierowuje wszystkie wywołania do nowego ProductController
 * 
 * @deprecated Użyj /api/products zamiast /api/tiles
 */
@Deprecated
@RestController
@RequestMapping("/api/tiles")
public class TilesController {

    private final ProductService productService;

    public TilesController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/tiles/getAll -> /api/products?category=TILE
     */
    @GetMapping("/getAll")
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.getAllProductsByCategory(ProductCategory.TILE));
    }

    /**
     * GET /api/tiles/productGroups -> /api/products/manufacturers?category=TILE
     * UWAGA: Zwraca producentów, nie grupy!
     */
    @GetMapping("/productGroups")
    public ResponseEntity<List<String>> getProductGroups() {
        // To był błąd w starym API - "productGroups" faktycznie zwracał producentów
        return ResponseEntity.ok(productService.getManufacturers(ProductCategory.TILE));
    }

    /**
     * GET /api/tiles/productGroup?id={id}
     * LEGACY - nie ma bezpośredniego odpowiednika
     */
    @GetMapping("/productGroup")
    public ResponseEntity<?> getProductGroup(@RequestParam Long id) {
        // Nie mamy tego w nowym API - zwróć not implemented
        return ResponseEntity.status(501).body("Endpoint /api/tiles/productGroup jest deprecated. Użyj /api/products/groups");
    }

    /**
     * POST /api/tiles/map -> /api/products/fill-quantities?category=TILE
     */
    @PostMapping("/map")
    public ResponseEntity<List<Product>> map(@RequestBody List<Input> inputList) {
        return ResponseEntity.ok(productService.fillProductQuantities(inputList, ProductCategory.TILE));
    }

    /**
     * POST /api/tiles/importWithNames -> /api/products/import-with-names?category=TILE
     */
    @PostMapping("/importWithNames")
    public ResponseEntity<List<Product>> importWithNames(
            @RequestParam("file[]") MultipartFile[] files,
            @RequestParam("name[]") String[] names) {
        
        if (files.length != names.length) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<MultipartFile> fileList = Arrays.asList(files);
            List<String> nameList = Arrays.asList(names);
            List<Product> products = productService.importProductsWithCustomNames(
                fileList, nameList, ProductCategory.TILE
            );
            return ResponseEntity.ok(products);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * POST /api/tiles/calculateMargin -> /api/products/calculate-margin?category=TILE
     */
    @PostMapping("/calculateMargin")
    public ResponseEntity<List<Product>> calculateMargin(@RequestBody Integer marginPercent) {
        return ResponseEntity.ok(productService.calculateMargin(marginPercent, ProductCategory.TILE));
    }

    /**
     * POST /api/tiles/calculateDiscount -> /api/products/calculate-discount?category=TILE
     */
    @PostMapping("/calculateDiscount")
    public ResponseEntity<List<Product>> calculateDiscount(@RequestBody Integer discountPercent) {
        return ResponseEntity.ok(productService.calculateDiscount(discountPercent, ProductCategory.TILE));
    }
}

