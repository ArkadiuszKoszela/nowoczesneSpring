package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.DTO.DiscountUpdateRequest;
import pl.koszela.nowoczesnebud.DTO.GroupOptionRequest;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Service.ProductService;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    public ProductController(ProductService productService) {
        this.productService = productService;
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
     * Import produktów z nazwami
     * POST /api/products/import-with-names?category=TILE
     * ZASTĘPUJE: /api/tiles/importWithNames
     */
    @PostMapping("/import-with-names")
    public ResponseEntity<List<Product>> importProductsWithNames(
            @RequestParam("file[]") MultipartFile[] files,
            @RequestParam("name[]") String[] names,
            @RequestParam ProductCategory category) {
        
        if (files.length != names.length) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<MultipartFile> fileList = Arrays.asList(files);
            List<String> nameList = Arrays.asList(names);
            List<Product> products = productService.importProductsWithCustomNames(
                fileList, nameList, category
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
        
        System.out.println("🟢 CONTROLLER: fillQuantities - kategoria: " + category);
        System.out.println("📨 CONTROLLER: Otrzymano inputów: " + inputList.size());
        
        List<Product> products = productService.fillProductQuantities(inputList, category);
        
        System.out.println("📤 CONTROLLER: Zwracam produktów: " + products.size());
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
            @RequestBody DiscountUpdateRequest request) {
        
        Product product = productService.updateProductDiscounts(
            id,
            request.basicDiscount,
            request.promotionDiscount,
            request.additionalDiscount,
            request.skontoDiscount
        );

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

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
     * Body: { "manufacturer": "CANTUS", "groupName": "łupek kryszał głaz NOBLESSE", "category": "TILE", "isMainOption": true }
     */
    @PostMapping("/set-group-option")
    public ResponseEntity<List<Product>> setGroupOption(@RequestBody GroupOptionRequest request) {
        System.out.println("🎯 CONTROLLER: setGroupOption dla grupy: " + request.manufacturer + " / " + request.groupName);
        System.out.println("   Kategoria: " + request.category + ", isMainOption: " + request.isMainOption);
        
        List<Product> updatedProducts = productService.setGroupOption(
            request.category,
            request.manufacturer,
            request.groupName,
            request.isMainOption
        );
        
        System.out.println("✅ CONTROLLER: Zaktualizowano produktów: " + updatedProducts.size());
        return ResponseEntity.ok(updatedProducts);
    }

    // === DTOs ===
    public static class DiscountUpdateRequest {
        public Integer basicDiscount;
        public Integer promotionDiscount;
        public Integer additionalDiscount;
        public Integer skontoDiscount;
    }

    public static class GroupOptionRequest {
        public ProductCategory category;
        public String manufacturer;
        public String groupName;
        public Boolean isMainOption; // null, true, false
    }
}

