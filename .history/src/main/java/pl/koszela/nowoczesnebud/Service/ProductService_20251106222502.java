package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshot;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final GlobalDiscountService globalDiscountService;
    private final ProductValidationService productValidationService;
    private final PriceListSnapshotService priceListSnapshotService;

    public ProductService(ProductRepository productRepository,
                         ProductImportService productImportService,
                         PriceCalculationService priceCalculationService,
                         GlobalDiscountService globalDiscountService,
                         ProductValidationService productValidationService,
                         PriceListSnapshotService priceListSnapshotService) {
        this.productRepository = productRepository;
        this.productImportService = productImportService;
        this.priceCalculationService = priceCalculationService;
        this.globalDiscountService = globalDiscountService;
        this.productValidationService = productValidationService;
        this.priceListSnapshotService = priceListSnapshotService;
    }

    /**
     * IMPORT - zastępuje TilesService.getAllTileWithNames()
     * DODAJE nowe produkty bez usuwania istniejących
     */
    @Transactional
    public List<Product> importProductsWithCustomNames(
            List<MultipartFile> files,
            List<String> customNames,
            ProductCategory category) throws IOException {

        // Pobierz istniejące produkty tej kategorii (do sprawdzenia duplikatów)
        List<Product> existingProducts = productRepository.findByCategory(category);

        // Importuj nowe produkty z plików
        List<Product> importedProducts = productImportService.importProductsWithCustomNames(files, customNames, category);

        // Sprawdź duplikaty i filtruj tylko nowe produkty
        List<Product> newProducts = new ArrayList<>();
        for (Product importedProduct : importedProducts) {
            // Sprawdź czy produkt już istnieje (po nazwie, producencie i grupie)
            boolean isDuplicate = existingProducts.stream().anyMatch(existing -> 
                existing.getName() != null && importedProduct.getName() != null &&
                existing.getName().equals(importedProduct.getName()) &&
                existing.getManufacturer() != null && importedProduct.getManufacturer() != null &&
                existing.getManufacturer().equals(importedProduct.getManufacturer()) &&
                existing.getGroupName() != null && importedProduct.getGroupName() != null &&
                existing.getGroupName().equals(importedProduct.getGroupName())
            );
            
            if (!isDuplicate) {
                newProducts.add(importedProduct);
                System.out.println("✅ Dodawanie nowego produktu: " + importedProduct.getName() + " (" + importedProduct.getManufacturer() + " - " + importedProduct.getGroupName() + ")");
            } else {
                System.out.println("⏭️  Pominięto duplikat: " + importedProduct.getName() + " (" + importedProduct.getManufacturer() + " - " + importedProduct.getGroupName() + ")");
            }
        }

        // Zapisz tylko nowe produkty (bez duplikatów)
        if (!newProducts.isEmpty()) {
            List<Product> savedProducts = productRepository.saveAll(newProducts);
            System.out.println("📦 Zaimportowano " + savedProducts.size() + " nowych produktów (pominięto " + (importedProducts.size() - newProducts.size()) + " duplikatów)");
            
            // ⚠️ WAŻNE: Po imporcie utwórz snapshot cennika dla kategorii
            // Snapshot będzie używany przez projekty utworzone po tym imporcie
            logger.info("📸 Tworzenie snapshotu cennika dla kategorii {} po imporcie", category);
            try {
                priceListSnapshotService.createSnapshotForDate(java.time.LocalDateTime.now(), category);
                logger.info("✅ Snapshot cennika utworzony dla kategorii {}", category);
            } catch (Exception e) {
                logger.error("❌ Błąd tworzenia snapshotu cennika dla kategorii {}: {}", category, e.getMessage(), e);
                // Nie przerywamy - import się powiódł, tylko snapshot się nie utworzył
            }
            
            return savedProducts;
        } else {
            System.out.println("⚠️  Wszystkie produkty były duplikatami - nic nie dodano");
            return new ArrayList<>();
        }
    }

    /**
     * Pobierz wszystkie produkty danej kategorii
     * UWAGA: Wypełnia rabaty globalne!
     */
    public List<Product> getAllProductsByCategory(ProductCategory category) {
        List<Product> products = productRepository.findByCategory(category);
        fillGlobalDiscounts(products, category);
        return products;
    }

    /**
     * Wypełnia produkty informacjami o rabatach globalnych
     */
    private void fillGlobalDiscounts(List<Product> products, ProductCategory category) {
        // Pobierz aktualne rabaty globalne
        Optional<GlobalDiscount> mainDiscount = globalDiscountService.getCurrentMainDiscount(category);
        Optional<GlobalDiscount> optionalDiscount = globalDiscountService.getCurrentOptionalDiscount(category);

        Double mainPercent = mainDiscount.map(GlobalDiscount::getDiscountPercent).orElse(null);
        Double optionalPercent = optionalDiscount.map(GlobalDiscount::getDiscountPercent).orElse(null);

        logger.debug("Rabaty globalne dla {}: główny={}%, opcjonalny={}%", 
                    category, mainPercent, optionalPercent);

        // Wypełnij każdy produkt
        for (Product product : products) {
            product.setGlobalMainDiscount(mainPercent);
            product.setGlobalOptionalDiscount(optionalPercent);
            product.setHasGlobalDiscount(mainPercent != null || optionalPercent != null);
        }
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
     * Wypełnij ilości produktów na podstawie inputów - TYLKO W PAMIĘCI (bez zapisu do bazy!)
     * ⚠️ WAŻNE: Tworzy KOPIE produktów, nie modyfikuje oryginalnych encji z bazy
     * ⚠️ WAŻNE: Metoda NIE jest @Transactional - nie zapisuje zmian do bazy!
     */
    public List<Product> fillProductQuantities(List<Input> inputList, ProductCategory category) {
        logger.info("fillProductQuantities START - kategoria: {} (TYLKO W PAMIĘCI - tworzę kopie)", category);
        logger.debug("Liczba inputów: {}", inputList.size());
        
        if (logger.isDebugEnabled()) {
            logger.debug("Lista inputów:");
            for (Input input : inputList) {
                logger.debug("  - name: '{}', mapperName: '{}', quantity: {}", 
                    input.getName(), input.getMapperName(), input.getQuantity());
            }
        }
        
        // Pobierz produkty z bazy (oryginalne encje - NIE modyfikujemy ich!)
        List<Product> originalProducts = productRepository.findByCategory(category);
        logger.info("Liczba produktów w kategorii {}: {}", category, originalProducts.size());
        
        // ⚠️ WAŻNE: Tworzymy KOPIE produktów zamiast modyfikować oryginalne encje
        // To zapobiega automatycznemu zapisowi zmian przez Hibernate
        List<Product> productsCopy = new ArrayList<>();
        for (Product original : originalProducts) {
            Product copy = createProductCopy(original);
            productsCopy.add(copy);
        }

        int updatedCount = 0;
        for (Product product : productsCopy) {
            for (Input input : inputList) {
                if (product.getMapperName() != null && 
                    product.getMapperName().equalsIgnoreCase(input.getMapperName())) {
                    
                    logger.debug("MATCH: {} (Input: {})", product.getMapperName(), input.getQuantity());
                    
                    // Sprawdź czy quantity nie jest null
                    if (input.getQuantity() == null) {
                        logger.debug("  Pomijam - quantity jest null dla input: {}", input.getMapperName());
                        continue;
                    }
                    
                    // 1. Oblicz ilość (na KOPII, nie na oryginale!)
                    double quantity = priceCalculationService.calculateProductQuantity(
                        input.getQuantity(), 
                        product.getQuantityConverter()
                    );
                    product.setQuantity(quantity);
                    logger.debug("  Ilość obliczona: {}", quantity);

                    // 2. Przelicz cenę zakupu jeśli nie jest ustawiona (na KOPII!)
                    if (product.getPurchasePrice() == 0.00 && product.getRetailPrice() != 0.00) {
                        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                        product.setPurchasePrice(purchasePrice);
                        logger.debug("  Cena zakupu obliczona: {}", purchasePrice);
                    }
                    
                    // 3. Ustaw cenę sprzedaży = cena katalogowa (na KOPII!)
                    // Zysk = (retailPrice - purchasePrice) × quantity
                    if (product.getRetailPrice() > 0.00) {
                        product.setSellingPrice(product.getRetailPrice());
                        logger.debug("  Cena sprzedaży = retailPrice: {} (zysk na jednostce: {})", 
                            product.getRetailPrice(), 
                            product.getRetailPrice() - product.getPurchasePrice());
                    } else if (product.getPurchasePrice() > 0.00 && product.getMarginPercent() > 0.00) {
                        // Jeśli nie ma retailPrice, ale jest marża, oblicz z marży
                        double sellingPrice = priceCalculationService.calculateRetailPrice(product);
                        product.setSellingPrice(sellingPrice);
                        logger.debug("  Cena sprzedaży obliczona z marży: {} (marża: {}%)", sellingPrice, product.getMarginPercent());
                    }
                    
                    updatedCount++;
                }
            }
        }

        logger.info("Zaktualizowano produktów: {} (TYLKO KOPIE W PAMIĘCI - oryginały w bazie nietknięte)", updatedCount);
        logger.info("fillProductQuantities KONIEC");
        
        // ⚠️ NIE ZAPISUJEMY DO BAZY! Zwracamy KOPIE produktów z przeliczonymi ilościami i cenami
        // Te kopie będą zapisane jako snapshoty w projekcie, nie w cenniku!
        return productsCopy;
    }

    /**
     * Tworzy kopię produktu (aby nie modyfikować oryginalnej encji z bazy)
     */
    private Product createProductCopy(Product original) {
        Product copy = new Product();
        
        // Skopiuj wszystkie pola
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setManufacturer(original.getManufacturer());
        copy.setGroupName(original.getGroupName());
        copy.setCategory(original.getCategory());
        copy.setMapperName(original.getMapperName());
        copy.setRetailPrice(original.getRetailPrice());
        copy.setPurchasePrice(original.getPurchasePrice());
        copy.setSellingPrice(original.getSellingPrice());
        copy.setUnit(original.getUnit());
        copy.setQuantity(original.getQuantity());
        copy.setQuantityConverter(original.getQuantityConverter());
        copy.setBasicDiscount(original.getBasicDiscount());
        copy.setPromotionDiscount(original.getPromotionDiscount());
        copy.setAdditionalDiscount(original.getAdditionalDiscount());
        copy.setSkontoDiscount(original.getSkontoDiscount());
        copy.setMarginPercent(original.getMarginPercent());
        copy.setIsMainOption(original.getIsMainOption());
        copy.setAccessoryType(original.getAccessoryType());
        
        return copy;
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
     * Oblicz marżę - TYLKO W PAMIĘCI (bez zapisu do bazy!)
     * ⚠️ WAŻNE: Nie zapisuje do bazy - zwraca KOPIE produktów z przeliczonymi cenami
     * Te produkty będą zapisane jako snapshoty w projekcie, nie w cenniku!
     * ⚠️ WAŻNE: Metoda NIE jest @Transactional - nie zapisuje zmian do bazy!
     */
    public List<Product> calculateMargin(Integer marginPercent, ProductCategory category) {
        logger.info("calculateMargin: marża={}%, kategoria={} (TYLKO W PAMIĘCI - tworzę kopie)", marginPercent, category);
        
        // Pobierz produkty z bazy (oryginalne encje - NIE modyfikujemy ich!)
        List<Product> originalProducts = productRepository.findByCategory(category);
        
        // ⚠️ WAŻNE: Tworzymy KOPIE produktów zamiast modyfikować oryginalne encje
        List<Product> products = new ArrayList<>();
        for (Product original : originalProducts) {
            Product copy = createProductCopy(original);
            products.add(copy);
        }
        
        int updatedCount = 0;

        for (Product product : products) {
            // Zapisz marginPercent w produkcie (tylko w pamięci, na KOPII!)
            product.setMarginPercent(marginPercent.doubleValue());
            
            // Oblicz nową cenę sprzedaży (purchasePrice + marża) - TYLKO W PAMIĘCI
            if (product.getPurchasePrice() > 0) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithMargin(
                    product, marginPercent
                );
                product.setSellingPrice(sellingPrice);
                updatedCount++;
                logger.debug("  {} - {}: {} → {} (TYLKO KOPIA W PAMIĘCI)", product.getName(), product.getPurchasePrice(), sellingPrice);
            }
        }

        logger.info("Przeliczono {} produktów (TYLKO KOPIE W PAMIĘCI - oryginały w bazie nietknięte)", updatedCount);
        // ⚠️ NIE ZAPISUJEMY DO BAZY! Zwracamy KOPIE produktów z przeliczonymi cenami
        return products;
    }

    /**
     * Oblicz rabat - TYLKO W PAMIĘCI (bez zapisu do bazy!)
     * ⚠️ WAŻNE: Nie zapisuje do bazy - zwraca KOPIE produktów z przeliczonymi cenami
     * Te produkty będą zapisane jako snapshoty w projekcie, nie w cenniku!
     * ⚠️ WAŻNE: Metoda NIE jest @Transactional - nie zapisuje zmian do bazy!
     */
    public List<Product> calculateDiscount(Integer discountPercent, ProductCategory category) {
        logger.info("calculateDiscount: rabat={}%, kategoria={} (TYLKO W PAMIĘCI - tworzę kopie)", discountPercent, category);
        
        // Pobierz produkty z bazy (oryginalne encje - NIE modyfikujemy ich!)
        List<Product> originalProducts = productRepository.findByCategory(category);
        
        // ⚠️ WAŻNE: Tworzymy KOPIE produktów zamiast modyfikować oryginalne encje
        List<Product> products = new ArrayList<>();
        for (Product original : originalProducts) {
            Product copy = createProductCopy(original);
            products.add(copy);
        }
        int updatedCount = 0;

        for (Product product : products) {
            // Oblicz nową cenę sprzedaży (retailPrice - rabat) - TYLKO W PAMIĘCI
            if (product.getRetailPrice() > 0) {
                double sellingPrice = priceCalculationService.calculateSellingPriceWithDiscount(
                    product, discountPercent
                );
                product.setSellingPrice(sellingPrice);
                updatedCount++;
                logger.debug("  {}: {} → {} (TYLKO W PAMIĘCI)", product.getName(), product.getRetailPrice(), sellingPrice);
            }
        }

        logger.info("Przeliczono {} produktów (TYLKO W PAMIĘCI - bez zapisu do bazy)", updatedCount);
        // ⚠️ NIE ZAPISUJEMY DO BAZY! Zwracamy produkty z przeliczonymi cenami
        return products;
    }

    /**
     * Ustaw opcję (Główna/Opcjonalna/Brak) dla CAŁEJ GRUPY produktów
     * TYLKO W PAMIĘCI (bez zapisu do bazy!)
     * ⚠️ WAŻNE: Nie zapisuje do bazy - zwraca KOPIE produktów z ustawioną opcją
     * Ta opcja będzie zapisana jako snapshoty w projekcie, nie w cenniku!
     * ⚠️ WAŻNE: Metoda NIE jest @Transactional - nie zapisuje zmian do bazy!
     * 
     * @param category Kategoria produktu (TILE, GUTTER, ACCESSORY)
     * @param manufacturer Producent (np. "CANTUS")
     * @param groupName Nazwa grupy (np. "łupek kryszał głaz NOBLESSE")
     * @param isMainOption true = Główna, false = Opcjonalna, null = Nie wybrano
     */
    public List<Product> setGroupOption(
            ProductCategory category,
            String manufacturer,
            String groupName,
            Boolean isMainOption) {
        
        logger.info("setGroupOption (TYLKO W PAMIĘCI - tworzę kopie):");
        logger.info("  Kategoria: {}", category);
        logger.info("  Producent: {}", manufacturer);
        logger.info("  Grupa: {}", groupName);
        logger.info("  isMainOption: {}", isMainOption);
        
        // Pobierz wszystkie produkty tej grupy z bazy (oryginalne encje)
        List<Product> originalProducts = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .filter(p -> groupName.equals(p.getGroupName()))
                .toList();
        
        logger.info("Znaleziono {} produktów w grupie", originalProducts.size());
        
        // ⚠️ WAŻNE: Tworzymy KOPIE produktów zamiast modyfikować oryginalne encje
        List<Product> products = new ArrayList<>();
        for (Product original : originalProducts) {
            Product copy = createProductCopy(original);
            copy.setIsMainOption(isMainOption);
            products.add(copy);
            logger.debug("  {} → isMainOption: {} (TYLKO KOPIA W PAMIĘCI)", copy.getName(), isMainOption);
        }
        
        logger.info("Ustawiono opcję dla {} produktów (TYLKO KOPIE W PAMIĘCI - oryginały w bazie nietknięte)", products.size());
        // ⚠️ NIE ZAPISUJEMY DO BAZY! Zwracamy KOPIE produktów z ustawioną opcją
        // Ta opcja będzie zapisana jako snapshoty w projekcie
        return products;
    }

    /**
     * Zapisz produkt z automatycznym przeliczaniem ceny zakupu
     * Jeśli zmieniono rabaty lub cenę katalogową, automatycznie przelicza cenę zakupu
     */
    @Transactional
    public Product saveProduct(Product product) {
        // Przelicz cenę zakupu jeśli mamy cenę katalogową i rabaty
        if (product.getRetailPrice() != null && product.getRetailPrice() > 0) {
            double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
            product.setPurchasePrice(purchasePrice);
            logger.debug("Przeliczono cenę zakupu dla produktu ID {}: {} → {}", 
                product.getId(), product.getRetailPrice(), purchasePrice);
        }
        
        return productRepository.save(product);
    }

    /**
     * Pobierz produkt po ID
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * BATCH UPDATE - aktualizuj wiele produktów naraz (PERFORMANCE!)
     */
    @Transactional
    public List<Product> updateProductsBatch(List<Product> products) {
        logger.info("📦 Batch update: {} produktów", products.size());
        
        // Walidacja wszystkich przed zapisem
        ProductValidationService.BatchValidationResult validation = 
            productValidationService.validateBatch(products);
        
        if (!validation.isAllValid()) {
            logger.error("❌ Walidacja nie powiodła się:");
            validation.getAllErrors().forEach(error -> logger.error("  - {}", error));
            throw new IllegalArgumentException(
                "Walidacja nie powiodła się: " + String.join("; ", validation.getAllErrors())
            );
        }
        
        // Loguj ostrzeżenia (jeśli są)
        if (validation.getWarningCount() > 0) {
            logger.warn("⚠️ Ostrzeżenia walidacji:");
            validation.getAllWarnings().forEach(warning -> logger.warn("  - {}", warning));
        }
        
        // ⚠️ WAŻNE: Loguj ID przed zapisem
        logger.info("📋 Produkty przed zapisem:");
        products.forEach(p -> {
            logger.info("  Produkt ID: {} | Nazwa: {} | Ma ID: {} | Kategoria: {} | Producent: {} | Grupa: {}", 
                p.getId(), 
                p.getName(), 
                p.getId() != null,
                p.getCategory(),
                p.getManufacturer(),
                p.getGroupName());
        });
        
        // Sprawdź które produkty istnieją w bazie (z ID)
        List<Long> existingIds = products.stream()
            .filter(p -> p.getId() != null)
            .map(Product::getId)
            .collect(Collectors.toList());
        
        // Pobierz istniejące produkty z bazy
        List<Product> existingProducts = new ArrayList<>();
        Set<Long> existingIdsSet;
        
        if (!existingIds.isEmpty()) {
            existingProducts = productRepository.findAllById(existingIds);
            logger.info("📊 Znaleziono {} istniejących produktów w bazie (z {} wysłanych z ID)", 
                existingProducts.size(), existingIds.size());
            
            existingProducts.forEach(ep -> {
                logger.info("  ✅ Istniejący produkt ID: {} | Nazwa: {}", ep.getId(), ep.getName());
            });
            
            // Sprawdź czy wszystkie produkty z ID istnieją w bazie
            existingIdsSet = existingProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
            
            List<Long> missingIds = existingIds.stream()
                .filter(id -> !existingIdsSet.contains(id))
                .collect(Collectors.toList());
            
            if (!missingIds.isEmpty()) {
                logger.warn("⚠️ Produkty z ID które NIE istnieją w bazie (będą utworzone jako nowe): {}", missingIds);
            }
        } else {
            existingIdsSet = new java.util.HashSet<>();
        }
        
        // ⚠️ WAŻNE: Rozdziel produkty na te do aktualizacji i do utworzenia (dla logowania)
        List<Product> productsToUpdate = new ArrayList<>();
        List<Product> productsToCreate = new ArrayList<>();
        
        for (Product product : products) {
            if (product.getId() != null && existingIdsSet.contains(product.getId())) {
                // Produkt z ID istnieje w bazie - do aktualizacji
                productsToUpdate.add(product);
            } else {
                // Produkt bez ID lub z ID które nie istnieje w bazie - do utworzenia
                productsToCreate.add(product);
            }
        }
        
        logger.info("📊 Rozdzielono produkty: {} do aktualizacji, {} do utworzenia", 
            productsToUpdate.size(), productsToCreate.size());
        
        // ⚠️ WAŻNE: Przelicz cenę zakupu dla wszystkich produktów przed zapisem
        // Jeśli zmieniono rabaty lub cenę katalogową, automatycznie przelicza cenę zakupu
        int recalculatedCount = 0;
        for (Product product : products) {
            if (product.getRetailPrice() != null && product.getRetailPrice() > 0) {
                double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                product.setPurchasePrice(purchasePrice);
                recalculatedCount++;
                logger.debug("Przeliczono cenę zakupu dla produktu ID {}: {} → {}", 
                    product.getId(), product.getRetailPrice(), purchasePrice);
            }
        }
        if (recalculatedCount > 0) {
            logger.info("💰 Przeliczono cenę zakupu dla {} produktów", recalculatedCount);
        }
        
        // Zapisz wszystkie w jednej transakcji
        // saveAll() automatycznie:
        // - Aktualizuje istniejące encje jeśli mają ID i istnieją w bazie (merge)
        // - Tworzy nowe encje jeśli nie mają ID lub nie istnieją w bazie (persist)
        List<Product> saved = productRepository.saveAll(products);
        logger.info("✅ Zapisano {} produktów", saved.size());
        
        // ⚠️ WAŻNE: Loguj które produkty zostały zaktualizowane, a które utworzone
        logger.info("📋 Produkty po zapisie:");
        Set<Long> updatedIds = productsToUpdate.stream()
            .map(Product::getId)
            .collect(Collectors.toSet());
        
        saved.forEach(p -> {
            boolean wasUpdated = updatedIds.contains(p.getId());
            String action = wasUpdated ? "✅ AKTUALIZOWANO" : "➕ UTWORZONO";
            logger.info("  {} produkt ID: {} | Nazwa: {} | Kategoria: {} | Producent: {} | Grupa: {}", 
                action, p.getId(), p.getName(), p.getCategory(), p.getManufacturer(), p.getGroupName());
        });
        
        // Utwórz snapshoty dla edytowanych kategorii
        Set<ProductCategory> changedCategories = saved.stream()
                .map(Product::getCategory)
                .distinct()
                .collect(Collectors.toSet());
        
        if (!changedCategories.isEmpty()) {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            logger.info("📸 Tworzenie snapshotów dla kategorii: {}", changedCategories);
            
            // Zapisz ID nowo utworzonych snapshotów aby wykluczyć je z usuwania
            List<Long> newlyCreatedSnapshotIds = new ArrayList<>();
            
            for (ProductCategory category : changedCategories) {
                try {
                    PriceListSnapshot newSnapshot = priceListSnapshotService.createSnapshotForDate(now, category);
                    newlyCreatedSnapshotIds.add(newSnapshot.getId());
                    logger.info("  ✅ Utworzono snapshot ID {} dla kategorii {}", newSnapshot.getId(), category);
                } catch (Exception e) {
                    logger.error("  ❌ Błąd tworzenia snapshotu dla kategorii {}: {}", category, e.getMessage(), e);
                }
            }
            
            // Wyczyść nieużywane snapshoty (wykluczając nowo utworzone)
            try {
                priceListSnapshotService.deleteUnusedSnapshots(newlyCreatedSnapshotIds);
            } catch (Exception e) {
                logger.error("❌ Błąd podczas czyszczenia nieużywanych snapshotów: {}", e.getMessage(), e);
            }
        }
        
        return saved;
    }

    /**
     * BULK DISCOUNT UPDATE - zmień rabaty dla całej grupy
     */
    @Transactional
    public List<Product> updateGroupDiscounts(
            ProductCategory category,
            String manufacturer,
            String groupName,
            Integer basicDiscount,
            Integer additionalDiscount,
            Integer promotionDiscount,
            Integer skontoDiscount) {
        
        logger.info("🎯 Bulk discount update:");
        logger.info("  Kategoria: {}", category);
        logger.info("  Producent: {}", manufacturer);
        logger.info("  Grupa: {}", groupName);
        logger.info("  Rabaty: basic={}, additional={}, promotion={}, skonto={}",
                   basicDiscount, additionalDiscount, promotionDiscount, skontoDiscount);
        
        // Pobierz wszystkie produkty grupy
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .filter(p -> groupName.equals(p.getGroupName()))
                .toList();
        
        if (products.isEmpty()) {
            logger.warn("⚠️ Nie znaleziono produktów dla grupy");
            return products;
        }
        
        logger.info("📦 Znaleziono {} produktów", products.size());
        
        // Zastosuj rabaty do wszystkich
        for (Product product : products) {
            if (basicDiscount != null) product.setBasicDiscount(basicDiscount);
            if (additionalDiscount != null) product.setAdditionalDiscount(additionalDiscount);
            if (promotionDiscount != null) product.setPromotionDiscount(promotionDiscount);
            if (skontoDiscount != null) product.setSkontoDiscount(skontoDiscount);
            
            // Przelicz cenę zakupu
            double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
            product.setPurchasePrice(purchasePrice);
            
            logger.debug("  ✓ {} - nowa cena zakupu: {}", product.getName(), purchasePrice);
        }
        
        // Zapisz wszystkie
        List<Product> saved = productRepository.saveAll(products);
        logger.info("✅ Zaktualizowano rabaty dla {} produktów", saved.size());
        
        return saved;
    }

    /**
     * Usuń wszystkie produkty danej kategorii i producenta (całkowity reset cennika)
     */
    @Transactional
    public void deleteAllByManufacturer(ProductCategory category, String manufacturer) {
        logger.warn("🗑️ Usuwanie wszystkich produktów: {} / {}", category, manufacturer);
        
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .toList();
        
        productRepository.deleteAll(products);
        logger.info("✅ Usunięto {} produktów", products.size());
    }

    /**
     * Usuń wszystkie produkty danej grupy produktowej
     */
    @Transactional
    public void deleteAllByGroup(ProductCategory category, String manufacturer, String groupName) {
        logger.warn("🗑️ Usuwanie wszystkich produktów grupy: {} / {} / {}", category, manufacturer, groupName);
        
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()) && groupName.equals(p.getGroupName()))
                .toList();
        
        productRepository.deleteAll(products);
        logger.info("✅ Usunięto {} produktów z grupy", products.size());
    }

    /**
     * BULK RENAME MANUFACTURER - zmień nazwę producenta dla wszystkich jego produktów
     */
    @Transactional
    public List<Product> renameManufacturer(
            ProductCategory category,
            String oldManufacturer,
            String newManufacturer) {
        
        logger.info("📝 Bulk rename manufacturer:");
        logger.info("  Kategoria: {}", category);
        logger.info("  Stara nazwa: '{}'", oldManufacturer);
        logger.info("  Nowa nazwa: '{}'", newManufacturer);
        
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> oldManufacturer.equals(p.getManufacturer()))
                .toList();
        
        if (products.isEmpty()) {
            logger.warn("⚠️ Nie znaleziono produktów dla producenta: {}", oldManufacturer);
            return products;
        }
        
        logger.info("📦 Znaleziono {} produktów", products.size());
        
        // Zmień nazwę producenta dla wszystkich
        for (Product product : products) {
            product.setManufacturer(newManufacturer);
            logger.debug("  ✓ {} - producent: {} → {}", 
                        product.getName(), oldManufacturer, newManufacturer);
        }
        
        List<Product> saved = productRepository.saveAll(products);
        logger.info("✅ Zmieniono nazwę producenta dla {} produktów", saved.size());
        
        return saved;
    }

    /**
     * BULK RENAME GROUP - zmień nazwę grupy dla wszystkich produktów w tej grupie
     */
    @Transactional
    public List<Product> renameGroup(
            ProductCategory category,
            String manufacturer,
            String oldGroupName,
            String newGroupName) {
        
        logger.info("📝 Bulk rename group:");
        logger.info("  Kategoria: {}", category);
        logger.info("  Producent: {}", manufacturer);
        logger.info("  Stara nazwa grupy: '{}'", oldGroupName);
        logger.info("  Nowa nazwa grupy: '{}'", newGroupName);
        
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .filter(p -> oldGroupName.equals(p.getGroupName()))
                .toList();
        
        if (products.isEmpty()) {
            logger.warn("⚠️ Nie znaleziono produktów dla grupy: {} / {}", manufacturer, oldGroupName);
            return products;
        }
        
        logger.info("📦 Znaleziono {} produktów", products.size());
        
        // Zmień nazwę grupy dla wszystkich
        for (Product product : products) {
            product.setGroupName(newGroupName);
            logger.debug("  ✓ {} - grupa: {} → {}", 
                        product.getName(), oldGroupName, newGroupName);
        }
        
        List<Product> saved = productRepository.saveAll(products);
        logger.info("✅ Zmieniono nazwę grupy dla {} produktów", saved.size());
        
        return saved;
    }
}

