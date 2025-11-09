package pl.koszela.nowoczesnebud.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.OfferItem;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Service.ProductService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Główny kontroler do zarządzania produktami
 * Zastępuje: TilesController, GuttersController, AccessoriesController
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Pobierz wszystkie produkty lub według kategorii
     * GET /api/products?category=TILE
     */
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) ProductCategory category) {
        
        if (category != null) {
            return ResponseEntity.ok(productService.getProductsByCategory(category));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Pobierz produkt po ID
     * GET /api/products/123
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productService.getProductById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Pobierz producentów dla kategorii
     * GET /api/products/manufacturers?category=TILE
     */
    @GetMapping("/manufacturers")
    public ResponseEntity<List<String>> getManufacturers(
            @RequestParam ProductCategory category) {
        return ResponseEntity.ok(productService.getManufacturersByCategory(category));
    }

    /**
     * Pobierz produkty według kategorii i producenta
     * GET /api/products/by-manufacturer?category=TILE&manufacturer=CANTUS
     */
    @GetMapping("/by-manufacturer")
    public ResponseEntity<List<Product>> getProductsByManufacturer(
            @RequestParam ProductCategory category,
            @RequestParam String manufacturer) {
        return ResponseEntity.ok(
            productService.getProductsByCategoryAndManufacturer(category, manufacturer)
        );
    }

    /**
     * Import produktów z plików Excel
     * POST /api/products/import?category=TILE
     * ZASTĘPUJE: /api/tiles/import, /api/gutters/import
     */
    @PostMapping("/import")
    public ResponseEntity<List<Product>> importProducts(
            @RequestParam("file[]") MultipartFile[] files,
            @RequestParam ProductCategory category) {
        
        try {
            List<MultipartFile> fileList = Arrays.asList(files);
            List<Product> products = productService.importProducts(fileList, category);
            return ResponseEntity.ok(products);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import produktów z niestandardowymi nazwami grup
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Wypełnij ilości produktów na podstawie inputów
     * POST /api/products/fill-quantities?category=TILE
     * ZASTĘPUJE: /api/tiles/map
     */
    @PostMapping("/fill-quantities")
    public ResponseEntity<List<Product>> fillQuantities(
            @RequestBody List<OfferItem> offerItems,
            @RequestParam ProductCategory category) {
        
        List<Product> products = productService.fillProductQuantities(offerItems, category);
        return ResponseEntity.ok(products);
    }

    /**
     * Aktualizuj rabaty produktu
     * PUT /api/products/123/discounts
     * ZASTĘPUJE: /api/tiles/saveDiscounts
     */
    @PutMapping("/{id}/discounts")
    public ResponseEntity<Product> updateDiscounts(
            @PathVariable Long id,
            @RequestBody DiscountUpdateRequest request) {
        
        Product product = productService.updateProductDiscounts(
            id,
            request.getBasicDiscount(),
            request.getPromotionDiscount(),
            request.getAdditionalDiscount(),
            request.getSkontoDiscount()
        );

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(product);
    }

    /**
     * Oblicz marżę dla produktów
     * POST /api/products/calculate-margin?category=TILE
     * ZASTĘPUJE: /api/tiles/calculateMargin
     */
    @PostMapping("/calculate-margin")
    public ResponseEntity<List<Product>> calculateMargin(
            @RequestBody Integer marginPercent,
            @RequestParam ProductCategory category) {
        
        List<Product> products = productService.calculateMargin(marginPercent, category);
        return ResponseEntity.ok(products);
    }

    /**
     * Oblicz rabat dla produktów
     * POST /api/products/calculate-discount?category=TILE
     * ZASTĘPUJE: /api/tiles/calculateDiscount
     */
    @PostMapping("/calculate-discount")
    public ResponseEntity<List<Product>> calculateDiscount(
            @RequestBody Integer discountPercent,
            @RequestParam ProductCategory category) {
        
        List<Product> products = productService.calculateDiscount(discountPercent, category);
        return ResponseEntity.ok(products);
    }

    /**
     * Aktualizuj ilość i cenę produktu
     * PUT /api/products/123
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request) {
        
        Product product = productService.updateProductQuantityAndPrice(id, request.getQuantity());
        
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(product);
    }

    /**
     * Usuń produkty danej kategorii
     * DELETE /api/products?category=TILE
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteProducts(@RequestParam ProductCategory category) {
        productService.deleteProductsByCategory(category);
        return ResponseEntity.noContent().build();
    }

    // === DTOs dla requestów ===

    public static class DiscountUpdateRequest {
        private Integer basicDiscount;
        private Integer promotionDiscount;
        private Integer additionalDiscount;
        private Integer skontoDiscount;

        public Integer getBasicDiscount() { return basicDiscount; }
        public void setBasicDiscount(Integer basicDiscount) { this.basicDiscount = basicDiscount; }
        
        public Integer getPromotionDiscount() { return promotionDiscount; }
        public void setPromotionDiscount(Integer promotionDiscount) { this.promotionDiscount = promotionDiscount; }
        
        public Integer getAdditionalDiscount() { return additionalDiscount; }
        public void setAdditionalDiscount(Integer additionalDiscount) { this.additionalDiscount = additionalDiscount; }
        
        public Integer getSkontoDiscount() { return skontoDiscount; }
        public void setSkontoDiscount(Integer skontoDiscount) { this.skontoDiscount = skontoDiscount; }
    }

    public static class ProductUpdateRequest {
        private Double quantity;

        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
    }
}

