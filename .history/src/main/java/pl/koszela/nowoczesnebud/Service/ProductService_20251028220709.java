package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * GŁÓWNY SERWIS PRODUKTÓW
 * Zastępuje: TilesService + GuttersService + AccessoriesService
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
     * IMPORT - zastępuje TilesService.getAllTileWithNames()
     */
    @Transactional
    public List<Product> importProductsWithCustomNames(
            List<MultipartFile> files,
            List<String> customNames,
            ProductCategory category) throws IOException {

        // Usuń stare produkty tej kategorii
        List<Product> existingProducts = productRepository.findByCategory(category);
        productRepository.deleteAll(existingProducts);

        // Importuj nowe
        List<Product> products = productImportService.importProductsWithCustomNames(files, customNames, category);

        // Zapisz
        return productRepository.saveAll(products);
    }

    /**
     * Pobierz wszystkie produkty danej kategorii
     */
    public List<Product> getAllProductsByCategory(ProductCategory category) {
        return productRepository.findByCategory(category);
    }

    /**
     * Pobierz producentów dla kategorii
     */
    public List<String> getManufacturers(ProductCategory category) {
        return productRepository.findDistinctManufacturersByCategory(category);
    }

    /**
     * Pobierz grupy dla producenta i kategorii
     */
    public List<String> getGroupNames(ProductCategory category, String manufacturer) {
        return productRepository.findDistinctGroupNamesByCategoryAndManufacturer(category, manufacturer);
    }

    /**
     * Wypełnij ilości na podstawie Input (zastępuje QuantityService.filledQuantityInTiles)
     * POPRAWIONA LOGIKA - oblicza ilości i ceny sprzedaży z marżą!
     */
    @Transactional
    public List<Product> fillProductQuantities(List<Input> inputList, ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product product : products) {
            for (Input input : inputList) {
                if (product.getMapperName() != null && 
                    product.getMapperName().equalsIgnoreCase(input.getMapperName())) {
                    
                    // 1. Oblicz ilość
                    double quantity = priceCalculationService.calculateProductQuantity(
                        input.getQuantity(), 
                        product.getQuantityConverter()
                    );
                    product.setQuantity(quantity);

                    // 2. Przelicz cenę zakupu jeśli nie jest ustawiona
                    if (product.getPurchasePrice() == 0.00 && product.getRetailPrice() != 0.00) {
                        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                        product.setPurchasePrice(purchasePrice);
                    }
                    
                    // 3. Oblicz cenę sprzedaży z marżą (purchasePrice + marginPercent)
                    // ZAWSZE przeliczamy, aby uwzględnić zmiany w cenach/marży
                    if (product.getPurchasePrice() > 0.00) {
                        double sellingPrice = priceCalculationService.calculateRetailPrice(product);
                        product.setSellingPrice(sellingPrice);
                    } else if (product.getRetailPrice() > 0.00) {
                        // Fallback: jeśli nie ma purchasePrice, użyj retailPrice
                        product.setSellingPrice(product.getRetailPrice());
                    }
                }
            }
        }

        return productRepository.saveAll(products);
    }

    /**
     * Aktualizuj rabaty produktu
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

        // Przelicz cenę zakupu
        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
        product.setPurchasePrice(purchasePrice);

        return productRepository.save(product);
    }

    /**
     * Oblicz marżę
     */
    @Transactional
    public List<Product> calculateMargin(Integer marginPercent, ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product product : products) {
            if (Boolean.TRUE.equals(product.getIsMainOption())) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithMargin(
                    product, marginPercent
                );
                product.setSellingPrice(sellingPrice);
            }
        }

        return productRepository.saveAll(products);
    }

    /**
     * Oblicz rabat
     */
    @Transactional
    public List<Product> calculateDiscount(Integer discountPercent, ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product product : products) {
            if (Boolean.TRUE.equals(product.getIsMainOption())) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithDiscount(
                    product, discountPercent
                );
                product.setSellingPrice(sellingPrice);
            }
        }

        return productRepository.saveAll(products);
    }

    /**
     * Zapisz produkt
     */
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    /**
     * Pobierz produkt po ID
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
}

