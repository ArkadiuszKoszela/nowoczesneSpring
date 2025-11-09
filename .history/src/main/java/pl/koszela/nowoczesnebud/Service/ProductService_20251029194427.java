package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
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
        logger.info("fillProductQuantities START - kategoria: {}", category);
        logger.debug("Liczba inputów: {}", inputList.size());
        
        if (logger.isDebugEnabled()) {
            logger.debug("Lista inputów:");
            for (Input input : inputList) {
                logger.debug("  - name: '{}', mapperName: '{}', quantity: {}", 
                    input.getName(), input.getMapperName(), input.getQuantity());
            }
        }
        
        List<Product> products = productRepository.findByCategory(category);
        logger.info("Liczba produktów w kategorii {}: {}", category, products.size());
        
        if (logger.isDebugEnabled()) {
            logger.debug("Pierwsze 10 produktów (mapperName):");
            for (int i = 0; i < Math.min(10, products.size()); i++) {
                Product p = products.get(i);
                logger.debug("  - {} | mapperName: '{}'", p.getName(), p.getMapperName());
            }
        }

        int updatedCount = 0;
        for (Product product : products) {
            for (Input input : inputList) {
                if (product.getMapperName() != null && 
                    product.getMapperName().equalsIgnoreCase(input.getMapperName())) {
                    
                    logger.debug("MATCH: {} (Input: {})", product.getMapperName(), input.getQuantity());
                    
                    // 1. Oblicz ilość
                    double quantity = priceCalculationService.calculateProductQuantity(
                        input.getQuantity(), 
                        product.getQuantityConverter()
                    );
                    product.setQuantity(quantity);
                    logger.debug("  Ilość obliczona: {}", quantity);

                    // 2. Przelicz cenę zakupu jeśli nie jest ustawiona
                    if (product.getPurchasePrice() == 0.00 && product.getRetailPrice() != 0.00) {
                        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                        product.setPurchasePrice(purchasePrice);
                        logger.debug("  Cena zakupu obliczona: {}", purchasePrice);
                    }
                    
                    // 3. Oblicz cenę sprzedaży z marżą (purchasePrice + marginPercent)
                    if (product.getPurchasePrice() > 0.00) {
                        double sellingPrice = priceCalculationService.calculateRetailPrice(product);
                        product.setSellingPrice(sellingPrice);
                        logger.debug("  Cena sprzedaży: {} (marża: {}%)", sellingPrice, product.getMarginPercent());
                    } else if (product.getRetailPrice() > 0.00) {
                        product.setSellingPrice(product.getRetailPrice());
                        logger.debug("  Cena sprzedaży = retailPrice: {}", product.getRetailPrice());
                    }
                    
                    updatedCount++;
                }
            }
        }

        logger.info("Zaktualizowano produktów: {}", updatedCount);
        List<Product> saved = productRepository.saveAll(products);
        logger.info("Zapisano produktów: {}", saved.size());
        logger.info("fillProductQuantities KONIEC");
        
        return saved;
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
     * Oblicz marżę - NAPRAWIONE: dla WSZYSTKICH produktów!
     */
    @Transactional
    public List<Product> calculateMargin(Integer marginPercent, ProductCategory category) {
        logger.info("calculateMargin: marża={}%, kategoria={}", marginPercent, category);
        
        List<Product> products = productRepository.findByCategory(category);
        int updatedCount = 0;

        for (Product product : products) {
            // Zapisz marginPercent w produkcie
            product.setMarginPercent(marginPercent.doubleValue());
            
            // Oblicz nową cenę sprzedaży (purchasePrice + marża)
            if (product.getPurchasePrice() > 0) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithMargin(
                    product, marginPercent
                );
                product.setSellingPrice(sellingPrice);
                updatedCount++;
                logger.debug("  {} - {}: {} → {}", product.getName(), product.getPurchasePrice(), sellingPrice);
            }
        }

        logger.info("Zaktualizowano {} produktów", updatedCount);
        return productRepository.saveAll(products);
    }

    /**
     * Oblicz rabat - NAPRAWIONE: dla WSZYSTKICH produktów!
     */
    @Transactional
    public List<Product> calculateDiscount(Integer discountPercent, ProductCategory category) {
        logger.info("calculateDiscount: rabat={}%, kategoria={}", discountPercent, category);
        
        List<Product> products = productRepository.findByCategory(category);
        int updatedCount = 0;

        for (Product product : products) {
            // Oblicz nową cenę sprzedaży (retailPrice - rabat)
            if (product.getRetailPrice() > 0) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithDiscount(
                    product, discountPercent
                );
                product.setSellingPrice(sellingPrice);
                updatedCount++;
                logger.debug("  {}: {} → {}", product.getName(), product.getRetailPrice(), sellingPrice);
            }
        }

        logger.info("Zaktualizowano {} produktów", updatedCount);
        return productRepository.saveAll(products);
    }

    /**
     * Ustaw opcję (Główna/Opcjonalna/Brak) dla CAŁEJ GRUPY produktów
     * 
     * @param category Kategoria produktu (TILE, GUTTER, ACCESSORY)
     * @param manufacturer Producent (np. "CANTUS")
     * @param groupName Nazwa grupy (np. "łupek kryszał głaz NOBLESSE")
     * @param isMainOption true = Główna, false = Opcjonalna, null = Nie wybrano
     */
    @Transactional
    public List<Product> setGroupOption(
            ProductCategory category,
            String manufacturer,
            String groupName,
            Boolean isMainOption) {
        
        logger.info("setGroupOption:");
        logger.info("  Kategoria: {}", category);
        logger.info("  Producent: {}", manufacturer);
        logger.info("  Grupa: {}", groupName);
        logger.info("  isMainOption: {}", isMainOption);
        
        // Pobierz wszystkie produkty tej grupy
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .filter(p -> groupName.equals(p.getGroupName()))
                .toList();
        
        logger.info("Znaleziono {} produktów w grupie", products.size());
        
        // Ustaw opcję dla wszystkich
        for (Product product : products) {
            product.setIsMainOption(isMainOption);
            logger.debug("  {} → isMainOption: {}", product.getName(), isMainOption);
        }
        
        // Zapisz
        List<Product> saved = productRepository.saveAll(products);
        logger.info("Zapisano {} produktów", saved.size());
        
        return saved;
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

