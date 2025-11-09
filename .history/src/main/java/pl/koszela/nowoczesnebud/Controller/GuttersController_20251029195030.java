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
 * @deprecated Użyj /api/products zamiast /api/gutters
 */
@Deprecated
@RestController
@RequestMapping("/api/gutters")
public class GuttersController {

    private final ProductService productService;

    public GuttersController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/gutters/getAll -> /api/products?category=GUTTER
     */
    @GetMapping("/getAll")
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.getAllProductsByCategory(ProductCategory.GUTTER));
    }

    /**
     * GET /api/gutters/productGroups -> /api/products/manufacturers?category=GUTTER
     */
    @GetMapping("/productGroups")
    public ResponseEntity<List<String>> getProductGroups() {
        return ResponseEntity.ok(productService.getManufacturers(ProductCategory.GUTTER));
    }

    /**
     * GET /api/gutters/productGroup?id={id}
     * LEGACY - nie ma bezpośredniego odpowiednika
     */
    @GetMapping("/productGroup")
    public ResponseEntity<?> getProductGroup(@RequestParam Long id) {
        return ResponseEntity.status(501).body("Endpoint /api/gutters/productGroup jest deprecated. Użyj /api/products/groups");
    }

    /**
     * POST /api/gutters/map -> /api/products/fill-quantities?category=GUTTER
     */
    @PostMapping("/map")
    public ResponseEntity<List<Product>> map(@RequestBody List<Input> inputList) {
        return ResponseEntity.ok(productService.fillProductQuantities(inputList, ProductCategory.GUTTER));
    }

    /**
     * POST /api/gutters/importWithNames -> /api/products/import-with-names?category=GUTTER
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
                fileList, nameList, ProductCategory.GUTTER
            );
            return ResponseEntity.ok(products);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * POST /api/gutters/calculateMargin -> /api/products/calculate-margin?category=GUTTER
     */
    @PostMapping("/calculateMargin")
    public ResponseEntity<List<Product>> calculateMargin(@RequestBody Integer marginPercent) {
        return ResponseEntity.ok(productService.calculateMargin(marginPercent, ProductCategory.GUTTER));
    }

    /**
     * POST /api/gutters/calculateDiscount -> /api/products/calculate-discount?category=GUTTER
     */
    @PostMapping("/calculateDiscount")
    public ResponseEntity<List<Product>> calculateDiscount(@RequestBody Integer discountPercent) {
        return ResponseEntity.ok(productService.calculateDiscount(discountPercent, ProductCategory.GUTTER));
    }
}

