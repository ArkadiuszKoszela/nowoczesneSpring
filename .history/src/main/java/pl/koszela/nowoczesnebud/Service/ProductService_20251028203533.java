package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.OfferItem;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Główny serwis do zarządzania produktami
 * Zastępuje: TilesService, GuttersService, AccessoriesService
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImportService productImportService;
    private final PriceCalculationService priceCalculationService;

    public ProductService(ProductRepository productRepository,
                         ProductImportService productImportService,
                         PriceCalculationService priceCalculationService) {
        this.productRepository = productRepository;
        this.productImportService = productImportService;
        this.priceCalculationService = priceCalculationService;
    }

    /**
     * Pobierz wszystkie produkty
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Pobierz produkty według kategorii
     */
    public List<Product> getProductsByCategory(ProductCategory category) {
        return productRepository.findByCategory(category);
    }

    /**
     * Pobierz produkty według kategorii i producenta
     */
    public List<Product> getProductsByCategoryAndManufacturer(ProductCategory category, String manufacturer) {
        return productRepository.findByCategoryAndManufacturer(category, manufacturer);
    }

    /**
     * Pobierz producentów dla danej kategorii
     */
    public List<String> getManufacturersByCategory(ProductCategory category) {
        return productRepository.findDistinctManufacturersByCategory(category);
    }

    /**
     * Pobierz produkt po ID
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Zapisz produkt
     */
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    /**
     * Zapisz wiele produktów
     */
    public List<Product> saveAllProducts(List<Product> products) {
        return productRepository.saveAll(products);
    }

    /**
     * Import produktów z plików Excel
     * ZASTĘPUJE: TilesService.getAllTile(), GuttersService.importGutters()
     */
    @Transactional
    public List<Product> importProducts(List<MultipartFile> files, ProductCategory category) throws IOException {
        // Usuń stare produkty tej kategorii
        deleteProductsByCategory(category);

        // Importuj nowe
        List<Product> products = productImportService.importProducts(files, category);

        // Zapisz do bazy
        return productRepository.saveAll(products);
    }

    /**
     * Import produktów z niestandardowymi nazwami grup
     */
    @Transactional
    public List<Product> importProductsWithCustomNames(
            List<MultipartFile> files,
            List<String> customNames,
            ProductCategory category) throws IOException {

        deleteProductsByCategory(category);

        List<Product> products = productImportService.importProductsWithCustomNames(files, customNames, category);

        return productRepository.saveAll(products);
    }

    /**
     * Usuń wszystkie produkty danej kategorii
     */
    @Transactional
    public void deleteProductsByCategory(ProductCategory category) {
        List<Product> productsToDelete = productRepository.findByCategory(category);
        productRepository.deleteAll(productsToDelete);
    }

    /**
     * Wypełnij ilości produktów na podstawie inputów użytkownika
     * ZASTĘPUJE: QuantityService.filledQuantityInTiles()
     */
    @Transactional
    public List<Product> fillProductQuantities(List<OfferItem> offerItems, ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product product : products) {
            for (OfferItem item : offerItems) {
                // Dopasowanie po mapperName
                if (product.getMapperName() != null && 
                    product.getMapperName().equalsIgnoreCase(item.getMapperName())) {
                    
                    // Oblicz ilość z konwerterem
                    double quantity = priceCalculationService.calculateProductQuantity(
                        item.getQuantity(), 
                        product.getQuantityConverter()
                    );
                    product.setQuantity(quantity);

                    // Przelicz cenę zakupu (jeśli potrzeba)
                    if (product.getPurchasePrice() == 0.00 && product.getRetailPrice() != 0.00) {
                        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                        product.setPurchasePrice(purchasePrice);
                    }
                }
            }
        }

        return productRepository.saveAll(products);
    }

    /**
     * Aktualizuj rabaty dla produktu
     * ZASTĘPUJE: ProductGroupService.saveDiscounts()
     */
    @Transactional
    public Product updateProductDiscounts(Long productId, 
                                         Integer basicDiscount,
                                         Integer promotionDiscount,
                                         Integer additionalDiscount,
                                         Integer skontoDiscount) {
        
        Optional<Product> optProduct = productRepository.findById(productId);
        if (!optProduct.isPresent()) {
            return null;
        }

        Product product = optProduct.get();
        
        if (basicDiscount != null) product.setBasicDiscount(basicDiscount);
        if (promotionDiscount != null) product.setPromotionDiscount(promotionDiscount);
        if (additionalDiscount != null) product.setAdditionalDiscount(additionalDiscount);
        if (skontoDiscount != null) product.setSkontoDiscount(skontoDiscount);

        // Przelicz cenę zakupu po zmianie rabatów
        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
        product.setPurchasePrice(purchasePrice);

        return productRepository.save(product);
    }

    /**
     * Oblicz marżę dla produktów
     * ZASTĘPUJE: ProductGroupService.calculateMargin()
     */
    @Transactional
    public List<Product> calculateMargin(Integer marginPercent, ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product product : products) {
            if (product.getIsMainOption() != null && product.getIsMainOption()) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithMargin(
                    product, 
                    marginPercent
                );
                product.setSellingPrice(sellingPrice);
            }
        }

        return productRepository.saveAll(products);
    }

    /**
     * Oblicz rabat dla produktów
     * ZASTĘPUJE: ProductGroupService.calculateMargin() z discount
     */
    @Transactional
    public List<Product> calculateDiscount(Integer discountPercent, ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product product : products) {
            if (product.getIsMainOption() != null && product.getIsMainOption()) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithDiscount(
                    product,
                    discountPercent
                );
                product.setSellingPrice(sellingPrice);
            }
        }

        return productRepository.saveAll(products);
    }

    /**
     * Aktualizuj ilość i cenę produktu
     */
    @Transactional
    public Product updateProductQuantityAndPrice(Long productId, Double quantity) {
        Optional<Product> optProduct = productRepository.findById(productId);
        if (!optProduct.isPresent()) {
            return null;
        }

        Product product = optProduct.get();
        product.setQuantity(quantity);

        return productRepository.save(product);
    }
}

