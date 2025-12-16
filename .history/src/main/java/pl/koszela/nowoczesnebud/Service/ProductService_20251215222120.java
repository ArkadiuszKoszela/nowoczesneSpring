package pl.koszela.nowoczesnebud.Service;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.GroupOption;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final ProductRepository productRepository;
    private final ProductImportService productImportService;
    private final PriceCalculationService priceCalculationService;
    private final GlobalDiscountService globalDiscountService;
    private final ProductValidationService productValidationService;
    private final pl.koszela.nowoczesnebud.Repository.ProductGroupAttributesRepository productGroupAttributesRepository;
    private final DiscountCalculationService discountCalculationService;

    public ProductService(ProductRepository productRepository,
                         ProductImportService productImportService,
                         PriceCalculationService priceCalculationService,
                         GlobalDiscountService globalDiscountService,
                         ProductValidationService productValidationService,
                         pl.koszela.nowoczesnebud.Repository.ProductGroupAttributesRepository productGroupAttributesRepository,
                         DiscountCalculationService discountCalculationService) {
        this.productRepository = productRepository;
        this.productImportService = productImportService;
        this.priceCalculationService = priceCalculationService;
        this.globalDiscountService = globalDiscountService;
        this.productValidationService = productValidationService;
        this.productGroupAttributesRepository = productGroupAttributesRepository;
        this.discountCalculationService = discountCalculationService;
    }

    /**
     * IMPORT - zastępuje TilesService.getAllTileWithNames()
     * DODAJE nowe produkty bez usuwania istniejących
     */
    @Transactional
    public List<Product> importProductsWithCustomNames(
            List<MultipartFile> files,
            List<String> customNames,
            List<String> manufacturers,
            List<String> groupNames,
            ProductCategory category) throws IOException {

        // 1. Pobierz istniejące produkty tej kategorii (do sprawdzenia duplikatów)
        List<Product> existingProducts = productRepository.findByCategory(category);

        // 2. Importuj nowe produkty z plików
        List<Product> importedProducts = productImportService.importProductsWithCustomNames(
            files, customNames, manufacturers, groupNames, category);

        // 3. Sprawdź duplikaty i filtruj tylko nowe produkty
        List<Product> newProducts = new ArrayList<>();
        
        // ⚡ OPTYMALIZACJA: Użyj HashMap dla szybszego sprawdzania duplikatów O(n+m) zamiast O(n*m)
        Map<String, Product> existingProductsMap = new HashMap<>();
        for (Product existing : existingProducts) {
            if (existing.getName() != null && existing.getManufacturer() != null && existing.getGroupName() != null) {
                String key = existing.getName() + "|" + existing.getManufacturer() + "|" + existing.getGroupName();
                existingProductsMap.put(key, existing);
            }
        }
        
        for (Product importedProduct : importedProducts) {
            if (importedProduct.getName() != null && importedProduct.getManufacturer() != null && importedProduct.getGroupName() != null) {
                String key = importedProduct.getName() + "|" + importedProduct.getManufacturer() + "|" + importedProduct.getGroupName();
                boolean isDuplicate = existingProductsMap.containsKey(key);
                
                if (!isDuplicate) {
                    newProducts.add(importedProduct);
                }
            } else {
                // Produkt bez wymaganych pól - dodaj jako nowy (może być błąd w danych)
                newProducts.add(importedProduct);
            }
        }

        // 4. Zapisz tylko nowe produkty (bez duplikatów)
        if (!newProducts.isEmpty()) {
            // ⚡ OPTYMALIZACJA: Użyj JDBC batch insert dla dużej liczby produktów (znacznie szybsze niż Hibernate ORM)
            if (newProducts.size() > 100) {
                logger.info("⏱️ [PERFORMANCE] Import produktów: {} produktów - używam JDBC batch insert", newProducts.size());
                List<Product> savedProducts = batchInsertProducts(newProducts);
                return savedProducts;
            } else {
                // Dla małej liczby produktów użyj standardowego saveAll
                List<Product> savedProducts = productRepository.saveAll(newProducts);
                return savedProducts;
            }
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * ⚡ OPTYMALIZACJA: Batch insert produktów (JDBC batch insert zamiast Hibernate ORM)
     * Znacznie szybsze niż Hibernate ORM dla dużej liczby produktów (100+)
     * 
     * @param products Lista produktów do zapisania
     * @return Lista zapisanych produktów z ID z bazy
     */
    private List<Product> batchInsertProducts(List<Product> products) {
        long startTime = System.currentTimeMillis();
        int totalProducts = products.size();
        logger.info("⏱️ [PERFORMANCE] BATCH INSERT PRODUCTS - START | rekordów: {}", totalProducts);
        
        String sql = "INSERT INTO products " +
                    "(name, manufacturer, category, group_name, retail_price, purchase_price, " +
                    "selling_price, unit, quantity_converter, quantity, mapper_name, discount, " +
                    "discount_calculation_method, basic_discount, promotion_discount, " +
                    "additional_discount, skonto_discount, margin_percent, accessory_type, " +
                    "product_type, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 1000;
        int totalBatches = (int)Math.ceil((double)totalProducts / batchSize);
        
        final List<Long> insertedIds = new ArrayList<>();
        
        // ⚡ WAŻNE: Używamy Hibernate Session.doWork() - działa zarówno z H2 jak i MySQL
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalProducts);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchPrepareStart = System.currentTimeMillis();
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            Product product = products.get(i);
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, product.getName());
                            pstmt.setString(paramIndex++, product.getManufacturer());
                            pstmt.setString(paramIndex++, product.getCategory() != null ? product.getCategory().name() : null);
                            pstmt.setString(paramIndex++, product.getGroupName());
                            pstmt.setObject(paramIndex++, product.getRetailPrice());
                            pstmt.setObject(paramIndex++, product.getPurchasePrice());
                            pstmt.setObject(paramIndex++, product.getSellingPrice());
                            pstmt.setString(paramIndex++, product.getUnit());
                            pstmt.setObject(paramIndex++, product.getQuantityConverter());
                            pstmt.setObject(paramIndex++, product.getQuantity());
                            pstmt.setString(paramIndex++, product.getMapperName());
                            pstmt.setObject(paramIndex++, product.getDiscount());
                            pstmt.setString(paramIndex++, product.getDiscountCalculationMethod() != null ? product.getDiscountCalculationMethod().name() : null);
                            pstmt.setObject(paramIndex++, product.getBasicDiscount());
                            pstmt.setObject(paramIndex++, product.getPromotionDiscount());
                            pstmt.setObject(paramIndex++, product.getAdditionalDiscount());
                            pstmt.setObject(paramIndex++, product.getSkontoDiscount());
                            pstmt.setObject(paramIndex++, product.getMarginPercent());
                            pstmt.setString(paramIndex++, product.getAccessoryType());
                            pstmt.setString(paramIndex++, product.getProductType());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        long batchPrepareEnd = System.currentTimeMillis();
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} przygotowany | rekordów: {} | czas przygotowania: {}ms", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchPrepareEnd - batchPrepareStart);
                        
                        long batchSaveStart = System.currentTimeMillis();
                        pstmt.executeBatch();
                        
                        // Pobierz wygenerowane ID
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            while (rs.next()) {
                                insertedIds.add(rs.getLong(1));
                            }
                        }
                        
                        long batchSaveEnd = System.currentTimeMillis();
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} zapisany (INSERT Product) | rekordów: {} | czas zapisu: {}ms", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchSaveEnd - batchSaveStart);
                    }
                } catch (SQLException e) {
                    logger.error("❌ [PERFORMANCE] Błąd podczas batch insert Product: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch insert Product", e);
                }
            }
        });
        
        entityManager.flush();
        
        // Pobierz zapisane produkty z ID z bazy
        List<Product> savedProducts = new ArrayList<>();
        if (!insertedIds.isEmpty()) {
            savedProducts = productRepository.findAllById(insertedIds);
            // Upewnij się, że produkty są w tej samej kolejności co insertedIds
            savedProducts.sort((p1, p2) -> Long.compare(
                insertedIds.indexOf(p1.getId()),
                insertedIds.indexOf(p2.getId())
            ));
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] BATCH INSERT PRODUCTS - END | rekordów: {} | batchy: {} | czas całkowity: {}ms", 
                   totalProducts, totalBatches, duration);
        
        return savedProducts;
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
     * Sprawdź które kombinacje producent+grupa już istnieją w bazie
     * ⚠️ WAŻNE: Sprawdzamy tylko manufacturer + groupName (editableGroupName)
     * "Nazwa produktu w systemie" (editableName) jest używana tylko jako fallback dla groupName,
     * jeśli groupName[] jest puste. Więc jeśli groupName jest wypełnione, productName nie jest częścią identyfikatora.
     * 
     * @param category Kategoria produktów
     * @param manufacturerGroupPairs Lista trójek (manufacturer, groupName, productName) do sprawdzenia
     * @return Lista istniejących kombinacji (manufacturer, groupName)
     */
    public List<pl.koszela.nowoczesnebud.DTO.CheckExistingGroupsRequest.ManufacturerGroupPair> checkExistingGroups(
            ProductCategory category,
            List<pl.koszela.nowoczesnebud.DTO.CheckExistingGroupsRequest.ManufacturerGroupPair> manufacturerGroupPairs) {
        
        List<pl.koszela.nowoczesnebud.DTO.CheckExistingGroupsRequest.ManufacturerGroupPair> existing = new ArrayList<>();
        
        for (pl.koszela.nowoczesnebud.DTO.CheckExistingGroupsRequest.ManufacturerGroupPair pair : manufacturerGroupPairs) {
            // Pobierz wszystkie produkty dla danego producenta w kategorii
            List<Product> products = productRepository.findByCategoryAndManufacturer(category, pair.getManufacturer());
            
            // ⚠️ WAŻNE: W backendzie, jeśli groupName[] jest wypełnione, używa go jako finalGroupName
            // Jeśli groupName[] jest puste, używa name[] (productName) jako fallback dla finalGroupName
            // Więc sprawdzamy:
            // 1. Jeśli groupName jest wypełnione -> sprawdzamy manufacturer + groupName
            // 2. Jeśli groupName jest puste -> sprawdzamy manufacturer + productName (bo productName będzie użyte jako groupName)
            
            String groupName = pair.getGroupName() != null ? pair.getGroupName().trim() : "";
            String productName = pair.getProductName() != null ? pair.getProductName().trim() : "";
            
            // ⚠️ WAŻNE: W backendzie, jeśli groupName[] jest wypełnione i różne od name[] (productName),
            // to używa kombinacji "groupName | productName" jako finalGroupName (zobacz ProductImportService)
            // Więc sprawdzamy zgodnie z tą logiką:
            final String finalGroupNameToCheck;
            if (!groupName.isEmpty() && !productName.isEmpty() && !groupName.equals(productName)) {
                // Jeśli groupName jest wypełnione i różne od productName, backend użyje kombinacji
                finalGroupNameToCheck = groupName + " | " + productName;
            } else if (!groupName.isEmpty()) {
                // Jeśli groupName jest wypełnione (i takie samo jak productName lub productName jest puste)
                finalGroupNameToCheck = groupName;
            } else if (!productName.isEmpty()) {
                // Jeśli groupName jest puste, użyj productName jako fallback
                finalGroupNameToCheck = productName;
            } else {
                // Oba są puste - nie powinno się zdarzyć (walidacja w frontendzie)
                finalGroupNameToCheck = "";
            }
            
            // Sprawdź czy istnieje grupa z takim samym manufacturer i finalGroupName
            boolean exists = false;
            if (!finalGroupNameToCheck.isEmpty()) {
                exists = products.stream()
                        .anyMatch(p -> p.getGroupName() != null && p.getGroupName().equals(finalGroupNameToCheck));
            }
            
            if (exists) {
                existing.add(pair);
            }
        }
        
        return existing;
    }

    /**
     * Pobierz słownik sugestii atrybutów dla autouzupełniania
     * Parsuje attributes JSON ze wszystkich GRUP PRODUKTOWYCH danej kategorii
     * i zbiera unikalne klucze i wartości
     * 
     * @param category Kategoria produktu (TILE, GUTTER, ACCESSORY)
     * @return Mapa: {"kolor": ["czerwony","brązowy"], "kształt": ["płaska","karpiówka"]}
     */
    public Map<String, List<String>> getAttributeSuggestions(ProductCategory category) {
        logger.debug("Pobieranie słownika atrybutów dla kategorii: {}", category);
        
        // Mapa wynikowa: klucz atrybutu -> lista unikalnych wartości
        Map<String, Set<String>> attributeMap = new HashMap<>();
        
        // Pobierz wszystkie atrybuty GRUP produktowych dla danej kategorii
        List<pl.koszela.nowoczesnebud.Model.ProductGroupAttributes> groupAttributes = 
            productGroupAttributesRepository.findByCategory(category);
        logger.debug("Znaleziono {} grup z atrybutami w kategorii {}", groupAttributes.size(), category);
        
        // Parsuj atrybuty JSON dla każdej grupy
        for (pl.koszela.nowoczesnebud.Model.ProductGroupAttributes group : groupAttributes) {
            String attributesJson = group.getAttributes();
            
            // Pomiń grupy bez atrybutów
            if (attributesJson == null || attributesJson.trim().isEmpty()) {
                continue;
            }
            
            try {
                // Parsuj JSON do mapy
                // Przykład: {"kolor":["czerwony","brązowy"],"kształt":["płaska"]}
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, List<String>> groupAttributesMap = mapper.readValue(
                    attributesJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<String>>>() {}
                );
                
                // Dodaj wszystkie klucze i wartości do attributeMap
                for (Map.Entry<String, List<String>> entry : groupAttributesMap.entrySet()) {
                    String attributeKey = entry.getKey();
                    List<String> attributeValues = entry.getValue();
                    
                    // Dodaj wartości do zbioru (automatycznie usuwa duplikaty)
                    attributeMap.computeIfAbsent(attributeKey, k -> new java.util.HashSet<>())
                               .addAll(attributeValues);
                }
            } catch (Exception e) {
                logger.warn("Błąd parsowania atrybutów dla grupy {}/{}: {}", 
                    group.getManufacturer(), group.getGroupName(), e.getMessage());
            }
        }
        
        // Konwertuj Set<String> na List<String> i posortuj alfabetycznie
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : attributeMap.entrySet()) {
            List<String> sortedValues = entry.getValue().stream()
                .sorted()
                .collect(Collectors.toList());
            result.put(entry.getKey(), sortedValues);
        }
        
        logger.debug("Zwracam słownik z {} atrybutami", result.size());
        return result;
    }

    /**
     * Pobierz atrybuty dla konkretnej grupy produktowej
     * 
     * @param category Kategoria produktu
     * @param manufacturer Producent
     * @param groupName Nazwa grupy
     * @return JSON string z atrybutami lub null jeśli brak
     */
    public String getGroupAttributes(ProductCategory category, String manufacturer, String groupName) {
        logger.debug("Pobieranie atrybutów dla grupy: {}/{}/{}", category, manufacturer, groupName);
        
        java.util.Optional<pl.koszela.nowoczesnebud.Model.ProductGroupAttributes> result = 
            productGroupAttributesRepository.findByCategoryAndManufacturerAndGroupName(
                category,
                manufacturer,
                groupName
            );
        
        if (result.isPresent()) {
            String attributes = result.get().getAttributes();
            logger.debug("Znaleziono atrybuty dla grupy: {}", attributes);
            return attributes;
        } else {
            logger.debug("Brak atrybutów dla grupy: {}/{}/{}", category, manufacturer, groupName);
            return null;
        }
    }

    /**
     * Zapisz/zaktualizuj atrybuty dla grupy produktowej
     */
    @Transactional
    public void saveGroupAttributes(pl.koszela.nowoczesnebud.DTO.GroupAttributesRequest request) {
        logger.info("Zapisywanie atrybutów dla grupy: {}/{}/{}", 
            request.getCategory(), request.getManufacturer(), request.getGroupName());

        // Znajdź istniejący rekord lub utwórz nowy
        pl.koszela.nowoczesnebud.Model.ProductGroupAttributes groupAttributes = 
            productGroupAttributesRepository.findByCategoryAndManufacturerAndGroupName(
                request.getCategory(),
                request.getManufacturer(),
                request.getGroupName()
            ).orElse(new pl.koszela.nowoczesnebud.Model.ProductGroupAttributes());

        // Ustaw wartości
        groupAttributes.setCategory(request.getCategory());
        groupAttributes.setManufacturer(request.getManufacturer());
        groupAttributes.setGroupName(request.getGroupName());

        // Konwertuj Map<String, List<String>> do JSON String
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String attributesJson = mapper.writeValueAsString(request.getAttributes());
                groupAttributes.setAttributes(attributesJson);
                
                logger.debug("Zapisano atrybuty JSON: {}", attributesJson);
            } catch (Exception e) {
                logger.error("Błąd konwersji atrybutów do JSON: {}", e.getMessage(), e);
                throw new RuntimeException("Błąd konwersji atrybutów do JSON", e);
            }
        } else {
            // Jeśli brak atrybutów, usuń rekord
            if (groupAttributes.getId() != null) {
                logger.info("Usuwanie atrybutów dla grupy: {}/{}/{}", 
                    request.getCategory(), request.getManufacturer(), request.getGroupName());
                productGroupAttributesRepository.delete(groupAttributes);
                return;
            }
        }

        // Zapisz
        productGroupAttributesRepository.save(groupAttributes);
        logger.info("Zapisano atrybuty dla grupy: {}/{}/{}", 
            request.getCategory(), request.getManufacturer(), request.getGroupName());
    }

    /**
     * Wypełnij ilości produktów na podstawie inputów - TYLKO W PAMIĘCI (bez zapisu do bazy!)
     * ⚠️ WAŻNE: Tworzy KOPIE produktów, nie modyfikuje oryginalnych encji z bazy
     * ⚠️ WAŻNE: Metoda NIE jest @Transactional - nie zapisuje zmian do bazy!
     */
    public List<Product> fillProductQuantities(List<Input> inputList, ProductCategory category) {
        // ⏱️ PERFORMANCE LOG: Start metody "Przelicz produkty"
        long methodStartTime = System.currentTimeMillis();
        logger.info("⏱️ [Przelicz produkty] fillProductQuantities - START (kategoria: {})", category);
        logger.info("⏱️ [Przelicz produkty] Liczba inputów: {}", inputList.size());
        
        // 1. Pobierz produkty z bazy (oryginalne encje - NIE modyfikujemy ich!)
        long dbStartTime = System.currentTimeMillis();
        List<Product> originalProducts = productRepository.findByCategory(category);
        long dbEndTime = System.currentTimeMillis();
        long dbDuration = dbEndTime - dbStartTime;
        logger.info("⏱️ [Przelicz produkty] DB Query: findByCategory - {} produktów w {}ms", originalProducts.size(), dbDuration);
        
        // 2. Tworzymy KOPIE produktów zamiast modyfikować oryginalne encje (zapobiega automatycznemu zapisowi przez Hibernate)
        long copyStartTime = System.currentTimeMillis();
        List<Product> productsCopy = new ArrayList<>();
        for (Product original : originalProducts) {
            Product copy = createProductCopy(original);
            productsCopy.add(copy);
        }
        long copyEndTime = System.currentTimeMillis();
        long copyDuration = copyEndTime - copyStartTime;
        logger.info("⏱️ [Przelicz produkty] Kopiowanie produktów: {} produktów skopiowanych w {}ms", productsCopy.size(), copyDuration);

        // 3. Matchowanie produktów z inputami - OPTYMALIZACJA: HashMap zamiast pętli w pętli
        // Przed: O(n*m) = 8775 × 26 = 228,150 iteracji w 43-66ms
        // Po: O(n+m) = 8775 + 26 = 8,801 operacji w ~5-10ms (4-6x szybciej!)
        long matchingStartTime = System.currentTimeMillis();
        int updatedCount = 0;
        
        // Krok 1: Utwórz HashMap inputów (mapperName.toLowerCase() -> Input) - O(m)
        Map<String, Input> inputMap = new HashMap<>();
        for (Input input : inputList) {
            if (input.getMapperName() != null && !input.getMapperName().isEmpty()) {
                inputMap.put(input.getMapperName().toLowerCase().trim(), input);
            }
        }
        logger.info("⏱️ [Przelicz produkty] HashMap inputów: {} unikalnych mapperName", inputMap.size());
        
        // Krok 2: Iteruj przez produkty i szukaj w HashMap - O(n)
        for (Product product : productsCopy) {
            if (product.getMapperName() == null || product.getMapperName().isEmpty()) {
                continue; // Pomiń produkty bez mapperName
            }
            
            // Szukaj dopasowanego inputu w HashMap (O(1) zamiast O(m)!)
            String productMapperKey = product.getMapperName().toLowerCase().trim();
            Input matchedInput = inputMap.get(productMapperKey);
            
            if (matchedInput != null) {
                // Sprawdź czy quantity nie jest null
                if (matchedInput.getQuantity() == null) {
                    logger.warn("  ⚠️ Pomijam - quantity jest null dla input: {}", matchedInput.getMapperName());
                    continue;
                }
                
                // ⚠️ ZMIANA: Pozwalamy na quantity = 0 (użytkownik chce przeliczać nawet dla wartości 0)
                if (matchedInput.getQuantity() < 0) {
                    logger.warn("  ⚠️ Pomijam - quantity < 0 dla input: {} (quantity={})", matchedInput.getMapperName(), matchedInput.getQuantity());
                    continue;
                }
                
                // 1. Oblicz ilość (na KOPII, nie na oryginale!)
                double quantityConverter = product.getQuantityConverter() != null ? product.getQuantityConverter() : 1.0;
                if (quantityConverter <= 0) {
                    logger.warn("  ⚠️ quantityConverter <= 0 dla produktu {}: {}", product.getId(), quantityConverter);
                    quantityConverter = 1.0; // Użyj domyślnej wartości
                }
                
                double quantity = priceCalculationService.calculateProductQuantity(
                    matchedInput.getQuantity(), 
                    quantityConverter
                );
                product.setQuantity(quantity);

                // 2. Przelicz cenę zakupu jeśli nie jest ustawiona (na KOPII!)
                if (product.getPurchasePrice() == null || product.getPurchasePrice() == 0.00) {
                    if (product.getRetailPrice() != null && product.getRetailPrice() != 0.00) {
                        double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                        product.setPurchasePrice(purchasePrice);
                    }
                }
                
                // 3. Ustaw cenę sprzedaży (na KOPII!)
                if (product.getCategory() == ProductCategory.ACCESSORY) {
                    // Dla akcesoriów: domyślnie cena sprzedaży = cena zakupu
                    if (product.getPurchasePrice() != null && product.getPurchasePrice() > 0.00) {
                        product.setSellingPrice(product.getPurchasePrice());
                    } else {
                        product.setSellingPrice(null);
                    }
                } else {
                    // Dla dachówek i rynien: cena sprzedaży = cena katalogowa
                    if (product.getRetailPrice() != null && product.getRetailPrice() > 0.00) {
                        product.setSellingPrice(product.getRetailPrice());
                    } else if (product.getPurchasePrice() != null && product.getPurchasePrice() > 0.00 && product.getMarginPercent() != null && product.getMarginPercent() > 0.00) {
                        // Jeśli nie ma retailPrice, ale jest marża, oblicz z marży
                        double sellingPrice = priceCalculationService.calculateRetailPrice(product);
                        product.setSellingPrice(sellingPrice);
                    }
                }
                
                updatedCount++;
            }
        }
        
        long matchingEndTime = System.currentTimeMillis();
        long matchingDuration = matchingEndTime - matchingStartTime;
        logger.info("⏱️ [Przelicz produkty] Matchowanie (HashMap O(n+m)): {} produktów + {} inputów w {}ms ({} dopasowań)", 
                   productsCopy.size(), inputList.size(), matchingDuration, updatedCount);
        
        // ⏱️ PERFORMANCE LOG: Koniec metody
        long methodEndTime = System.currentTimeMillis();
        long totalDuration = methodEndTime - methodStartTime;
        logger.info("⏱️ [Przelicz produkty] fillProductQuantities - END: {} produktów w {}ms [DB: {}ms, Kopiowanie: {}ms, Matchowanie: {}ms]", 
                   productsCopy.size(), totalDuration, dbDuration, copyDuration, matchingDuration);
        
        // ⚠️ NIE ZAPISUJEMY DO BAZY! Zwracamy KOPIE produktów z przeliczonymi ilościami i cenami
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
        copy.setDiscount(original.getDiscount());
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
                                         Integer skontoDiscount,
                                         pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod discountCalculationMethod) {
        
        Optional<Product> optProduct = productRepository.findById(productId);
        if (!optProduct.isPresent()) {
            return null;
        }

        Product product = optProduct.get();
        
        if (discountCalculationMethod == null) {
            throw new IllegalArgumentException("Metoda obliczania rabatu jest wymagana");
        }
        
        // Oblicz końcowy rabat używając wybranej metody
        double finalDiscount = discountCalculationService.calculateDiscount(
            discountCalculationMethod,
            basicDiscount,
            additionalDiscount,
            promotionDiscount,
            skontoDiscount
        );
        
        // Zapisz składowe rabaty
        if (basicDiscount != null) product.setBasicDiscount(basicDiscount);
        if (additionalDiscount != null) product.setAdditionalDiscount(additionalDiscount);
        if (promotionDiscount != null) product.setPromotionDiscount(promotionDiscount);
        if (skontoDiscount != null) product.setSkontoDiscount(skontoDiscount);
        
        // Zapisz metodę obliczania i końcowy rabat
        product.setDiscountCalculationMethod(discountCalculationMethod);
        product.setDiscount(finalDiscount);

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
            // ⚠️ WAŻNE: Dla akcesoriów nie stosujemy rabatów - pomijamy je
            if (product.getCategory() == ProductCategory.ACCESSORY) {
                // Akcesoria nie mają rabatów - pomijamy
                logger.debug("  {} (AKCESORIA): pomijam - akcesoria nie mają rabatów", product.getName());
                continue;
            }
            
            // Dla dachówek i rynien: rabat od retailPrice
            if (product.getRetailPrice() != null && product.getRetailPrice() > 0) {
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
     * @param isMainOption MAIN = Główna, OPTIONAL = Opcjonalna, NONE = Nie wybrano
     */
    public List<Product> setGroupOption(
            ProductCategory category,
            String manufacturer,
            String groupName,
            GroupOption isMainOption) {
        
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
        
        // ⚠️ WAŻNE: Przelicz cenę zakupu TYLKO jeśli użytkownik zmienił cenę katalogową lub rabaty
        // Jeśli użytkownik ręcznie zmienił cenę zakupu, użyj wartości z frontendu
        int recalculatedCount = 0;
        int preservedCount = 0;
        for (Product product : products) {
            if (product.getRetailPrice() != null && product.getRetailPrice() > 0) {
                // Sprawdź czy produkt istnieje w bazie (ma ID i jest w existingProducts)
                Product existingProduct = null;
                if (product.getId() != null && existingIdsSet.contains(product.getId())) {
                    existingProduct = existingProducts.stream()
                        .filter(ep -> ep.getId().equals(product.getId()))
                        .findFirst()
                        .orElse(null);
                }
                
                // Oblicz nową cenę zakupu na podstawie aktualnych wartości
                double calculatedPurchasePrice = priceCalculationService.calculatePurchasePrice(product);
                
                // Jeśli produkt istnieje w bazie, sprawdź czy użytkownik ręcznie zmienił cenę zakupu
                if (existingProduct != null) {
                    double newPurchasePrice = product.getPurchasePrice() != null ? product.getPurchasePrice() : 0.0;
                    
                    // Jeśli nowa cena zakupu różni się od obliczonej (więcej niż 0.01), 
                    // oznacza to że użytkownik ręcznie zmienił cenę zakupu - zachowaj ją
                    if (Math.abs(newPurchasePrice - calculatedPurchasePrice) > 0.01) {
                        // Użytkownik ręcznie zmienił cenę zakupu - zachowaj wartość z frontendu
                        preservedCount++;
                        logger.debug("Zachowano ręcznie zmienioną cenę zakupu dla produktu ID {}: {} (obliczona: {})", 
                            product.getId(), newPurchasePrice, calculatedPurchasePrice);
                    } else {
                        // Cena zakupu jest zgodna z obliczoną - użyj obliczonej wartości
                        product.setPurchasePrice(calculatedPurchasePrice);
                        recalculatedCount++;
                        logger.debug("Przeliczono cenę zakupu dla produktu ID {}: {} → {}", 
                            product.getId(), product.getRetailPrice(), calculatedPurchasePrice);
                    }
                } else {
                    // Nowy produkt (nie istnieje w bazie) - zawsze przelicz cenę zakupu
                    product.setPurchasePrice(calculatedPurchasePrice);
                    recalculatedCount++;
                    logger.debug("Przeliczono cenę zakupu dla nowego produktu: {} → {}", 
                        product.getRetailPrice(), calculatedPurchasePrice);
                }
            }
        }
        if (recalculatedCount > 0) {
            logger.info("💰 Przeliczono cenę zakupu dla {} produktów", recalculatedCount);
        }
        if (preservedCount > 0) {
            logger.info("💾 Zachowano ręcznie zmienioną cenę zakupu dla {} produktów", preservedCount);
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
        
        // TODO: Snapshoty zostały usunięte - projekty będą teraz przechowywać zapisane ceny w ProjectProduct
        
        return saved;
    }

    /**
     * BULK DISCOUNT UPDATE - zmień rabaty dla całej grupy
     * Oblicza końcowy rabat na podstawie wybranej metody i zapisuje do pola "discount"
     */
    @Transactional
    public List<Product> updateGroupDiscounts(
            ProductCategory category,
            String manufacturer,
            String groupName,
            Integer basicDiscount,
            Integer additionalDiscount,
            Integer promotionDiscount,
            Integer skontoDiscount,
            String productType,
            pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod discountCalculationMethod) {
        
        logger.info("🎯 Bulk discount update:");
        logger.info("  Kategoria: {}", category);
        logger.info("  Producent: {}", manufacturer);
        logger.info("  Grupa: {}", groupName != null ? groupName : "WSZYSTKIE (cały producent)");
        logger.info("  Typ produktu: {}", productType != null && !"ALL".equals(productType) ? productType : "WSZYSTKIE");
        logger.info("  Rabaty: basic={}, additional={}, promotion={}, skonto={}",
                   basicDiscount, additionalDiscount, promotionDiscount, skontoDiscount);
        logger.info("  Metoda obliczania: {}", discountCalculationMethod);
        
        if (discountCalculationMethod == null) {
            throw new IllegalArgumentException("Metoda obliczania rabatu jest wymagana");
        }
        
        // Oblicz końcowy rabat używając wybranej metody
        double finalDiscount = discountCalculationService.calculateDiscount(
            discountCalculationMethod,
            basicDiscount,
            additionalDiscount,
            promotionDiscount,
            skontoDiscount
        );
        logger.info("  → Końcowy rabat: {}%", finalDiscount);
        
        // Pobierz wszystkie produkty - jeśli groupName jest null, to dla całego producenta
        List<Product> products = productRepository.findByCategory(category).stream()
                .filter(p -> manufacturer.equals(p.getManufacturer()))
                .filter(p -> groupName == null || groupName.equals(p.getGroupName()))
                .filter(p -> productType == null || "ALL".equals(productType) || productType.equals(p.getProductType())) // Filtruj po typie produktu ("ALL" = wszystkie typy)
                .toList();
        
        if (products.isEmpty()) {
            String typeInfo = (productType != null && !"ALL".equals(productType)) ? " typu " + productType : "";
            logger.warn("⚠️ Nie znaleziono produktów dla {} / {}{}", 
                       manufacturer, 
                       groupName != null ? groupName : "całego producenta",
                       typeInfo);
            return products;
        }
        
        String typeInfo = (productType != null && !"ALL".equals(productType)) ? " typu " + productType : "";
        logger.info("📦 Znaleziono {} produktów{}", products.size(), typeInfo);
        
        // ⚡ OPTYMALIZACJA: Użyj JDBC batch UPDATE dla dużej liczby produktów (znacznie szybsze niż Hibernate ORM)
        if (products.size() > 50) {
            logger.info("⏱️ [PERFORMANCE] Bulk update group discounts: {} produktów - używam JDBC batch UPDATE", products.size());
            
            // Przelicz ceny zakupu dla wszystkich produktów przed batch update
            for (Product product : products) {
                // Zapisz składowe rabaty
                if (basicDiscount != null) product.setBasicDiscount(basicDiscount);
                if (additionalDiscount != null) product.setAdditionalDiscount(additionalDiscount);
                if (promotionDiscount != null) product.setPromotionDiscount(promotionDiscount);
                if (skontoDiscount != null) product.setSkontoDiscount(skontoDiscount);
                
                // Zapisz metodę obliczania i końcowy rabat
                product.setDiscountCalculationMethod(discountCalculationMethod);
                product.setDiscount(finalDiscount);
                
                // Przelicz cenę zakupu
                double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                product.setPurchasePrice(purchasePrice);
                
                logger.debug("  ✓ {} - rabat: {}%, metoda: {}, nowa cena zakupu: {}", 
                            product.getName(), finalDiscount, discountCalculationMethod, purchasePrice);
            }
            
            batchUpdateGroupDiscounts(products, basicDiscount, additionalDiscount, promotionDiscount, 
                                     skontoDiscount, discountCalculationMethod, finalDiscount);
            
            // Pobierz zaktualizowane produkty z bazy
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<Product> saved = productRepository.findAllById(productIds);
            logger.info("✅ Zaktualizowano rabaty dla {} produktów (JDBC batch UPDATE)", saved.size());
            return saved;
        } else {
            // Dla małej liczby produktów użyj standardowego saveAll
            for (Product product : products) {
                // Zapisz składowe rabaty
                if (basicDiscount != null) product.setBasicDiscount(basicDiscount);
                if (additionalDiscount != null) product.setAdditionalDiscount(additionalDiscount);
                if (promotionDiscount != null) product.setPromotionDiscount(promotionDiscount);
                if (skontoDiscount != null) product.setSkontoDiscount(skontoDiscount);
                
                // Zapisz metodę obliczania i końcowy rabat
                product.setDiscountCalculationMethod(discountCalculationMethod);
                product.setDiscount(finalDiscount);
                
                // Przelicz cenę zakupu
                double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                product.setPurchasePrice(purchasePrice);
                
                logger.debug("  ✓ {} - rabat: {}%, metoda: {}, nowa cena zakupu: {}", 
                            product.getName(), finalDiscount, discountCalculationMethod, purchasePrice);
            }
            
            List<Product> saved = productRepository.saveAll(products);
            logger.info("✅ Zaktualizowano rabaty dla {} produktów", saved.size());
            return saved;
        }
    }

    /**
     * Usuń wszystkie produkty danej kategorii (dla testów E2E)
     */
    @Transactional
    public void deleteAllByCategory(ProductCategory category) {
        logger.warn("🗑️ Usuwanie WSZYSTKICH produktów kategorii: {}", category);
        
        List<Product> products = productRepository.findByCategory(category);
        productRepository.deleteAll(products);
        
        logger.info("✅ Usunięto {} produktów kategorii {}", products.size(), category);
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
     * Usuń pojedynczy produkt po ID
     */
    @Transactional
    public void deleteProductById(Long id) {
        logger.info("🗑️ Usuwanie produktu ID: {}", id);
        
        if (!productRepository.existsById(id)) {
            logger.warn("⚠️ Produkt ID {} nie istnieje w bazie", id);
            throw new IllegalArgumentException("Produkt o ID " + id + " nie istnieje");
        }
        
        productRepository.deleteById(id);
        logger.info("✅ Produkt ID {} został usunięty z bazy", id);
    }

    /**
     * Usuń wiele produktów jednocześnie po ID (batch delete)
     * @param productIds Lista ID produktów do usunięcia
     * @return Map z wynikami: deletedCount, notFoundCount, deletedIds, notFoundIds
     */
    @Transactional
    public Map<String, Object> deleteProductsByIds(List<Long> productIds) {
        logger.info("🗑️ Batch delete: usuwanie {} produktów", productIds.size());
        
        Map<String, Object> result = new HashMap<>();
        List<Long> deletedIds = new ArrayList<>();
        List<Long> notFoundIds = new ArrayList<>();
        
        for (Long id : productIds) {
            if (productRepository.existsById(id)) {
                productRepository.deleteById(id);
                deletedIds.add(id);
                logger.debug("✅ Usunięto produkt ID: {}", id);
            } else {
                notFoundIds.add(id);
                logger.debug("⚠️ Produkt ID {} nie istnieje", id);
            }
        }
        
        result.put("deletedCount", deletedIds.size());
        result.put("notFoundCount", notFoundIds.size());
        result.put("deletedIds", deletedIds);
        result.put("notFoundIds", notFoundIds);
        
        logger.info("✅ Batch delete zakończony: usunięto {}, nie znaleziono {}", 
                   deletedIds.size(), notFoundIds.size());
        
        return result;
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
        
        // ⚡ OPTYMALIZACJA: Użyj JDBC batch UPDATE dla dużej liczby produktów (znacznie szybsze niż Hibernate ORM)
        if (products.size() > 50) {
            logger.info("⏱️ [PERFORMANCE] Bulk rename manufacturer: {} produktów - używam JDBC batch UPDATE", products.size());
            batchUpdateManufacturer(products, newManufacturer);
            // Pobierz zaktualizowane produkty z bazy
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<Product> saved = productRepository.findAllById(productIds);
            logger.info("✅ Zmieniono nazwę producenta dla {} produktów (JDBC batch UPDATE)", saved.size());
            return saved;
        } else {
            // Dla małej liczby produktów użyj standardowego saveAll
            for (Product product : products) {
                product.setManufacturer(newManufacturer);
                logger.debug("  ✓ {} - producent: {} → {}", 
                            product.getName(), oldManufacturer, newManufacturer);
            }
            List<Product> saved = productRepository.saveAll(products);
            logger.info("✅ Zmieniono nazwę producenta dla {} produktów", saved.size());
            return saved;
        }
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
        
        // ⚡ OPTYMALIZACJA: Użyj JDBC batch UPDATE dla dużej liczby produktów (znacznie szybsze niż Hibernate ORM)
        if (products.size() > 50) {
            logger.info("⏱️ [PERFORMANCE] Bulk rename group: {} produktów - używam JDBC batch UPDATE", products.size());
            batchUpdateGroupName(products, newGroupName);
            // Pobierz zaktualizowane produkty z bazy
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<Product> saved = productRepository.findAllById(productIds);
            logger.info("✅ Zmieniono nazwę grupy dla {} produktów (JDBC batch UPDATE)", saved.size());
            return saved;
        } else {
            // Dla małej liczby produktów użyj standardowego saveAll
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

    /**
     * ⚡ OPTYMALIZACJA: Batch UPDATE manufacturer (JDBC batch update zamiast Hibernate ORM)
     * Znacznie szybsze niż Hibernate ORM dla dużej liczby produktów (50+)
     * 
     * @param products Lista produktów do aktualizacji (muszą mieć ID)
     * @param newManufacturer Nowa nazwa producenta
     */
    private void batchUpdateManufacturer(List<Product> products, String newManufacturer) {
        long startTime = System.currentTimeMillis();
        int totalProducts = products.size();
        logger.info("⏱️ [PERFORMANCE] BATCH UPDATE MANUFACTURER - START | rekordów: {}", totalProducts);
        
        String sql = "UPDATE products SET manufacturer = ?, updated_at = ? WHERE id = ?";
        
        int batchSize = 1000;
        int totalBatches = (int) Math.ceil((double) totalProducts / batchSize);
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalProducts);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchPrepareStart = System.currentTimeMillis();
                        
                        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            Product product = products.get(i);
                            
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, newManufacturer);
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setLong(paramIndex++, product.getId());
                            
                            pstmt.addBatch();
                        }
                        
                        long batchPrepareEnd = System.currentTimeMillis();
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} przygotowany | rekordów: {} | czas przygotowania: {}ms",
                                   batchIndex + 1, totalBatches, recordsInBatch, batchPrepareEnd - batchPrepareStart);
                        
                        long batchUpdateStart = System.currentTimeMillis();
                        pstmt.executeBatch();
                        long batchUpdateEnd = System.currentTimeMillis();
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} zaktualizowany (UPDATE manufacturer) | rekordów: {} | czas aktualizacji: {}ms",
                                   batchIndex + 1, totalBatches, recordsInBatch, batchUpdateEnd - batchUpdateStart);
                    }
                } catch (SQLException e) {
                    logger.error("❌ [PERFORMANCE] Błąd podczas batch update manufacturer: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch update manufacturer", e);
                }
            }
        });
        
        entityManager.flush();
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] BATCH UPDATE MANUFACTURER - END | rekordów: {} | batchy: {} | czas całkowity: {}ms",
                   totalProducts, totalBatches, duration);
    }

    /**
     * ⚡ OPTYMALIZACJA: Batch UPDATE group_name (JDBC batch update zamiast Hibernate ORM)
     * Znacznie szybsze niż Hibernate ORM dla dużej liczby produktów (50+)
     * 
     * @param products Lista produktów do aktualizacji (muszą mieć ID)
     * @param newGroupName Nowa nazwa grupy
     */
    private void batchUpdateGroupName(List<Product> products, String newGroupName) {
        long startTime = System.currentTimeMillis();
        int totalProducts = products.size();
        logger.info("⏱️ [PERFORMANCE] BATCH UPDATE GROUP_NAME - START | rekordów: {}", totalProducts);
        
        String sql = "UPDATE products SET group_name = ?, updated_at = ? WHERE id = ?";
        
        int batchSize = 1000;
        int totalBatches = (int) Math.ceil((double) totalProducts / batchSize);
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalProducts);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchPrepareStart = System.currentTimeMillis();
                        
                        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            Product product = products.get(i);
                            
                            int paramIndex = 1;
                            pstmt.setString(paramIndex++, newGroupName);
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setLong(paramIndex++, product.getId());
                            
                            pstmt.addBatch();
                        }
                        
                        long batchPrepareEnd = System.currentTimeMillis();
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} przygotowany | rekordów: {} | czas przygotowania: {}ms",
                                   batchIndex + 1, totalBatches, recordsInBatch, batchPrepareEnd - batchPrepareStart);
                        
                        long batchUpdateStart = System.currentTimeMillis();
                        pstmt.executeBatch();
                        long batchUpdateEnd = System.currentTimeMillis();
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} zaktualizowany (UPDATE group_name) | rekordów: {} | czas aktualizacji: {}ms",
                                   batchIndex + 1, totalBatches, recordsInBatch, batchUpdateEnd - batchUpdateStart);
                    }
                } catch (SQLException e) {
                    logger.error("❌ [PERFORMANCE] Błąd podczas batch update group_name: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch update group_name", e);
                }
            }
        });
        
        entityManager.flush();
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] BATCH UPDATE GROUP_NAME - END | rekordów: {} | batchy: {} | czas całkowity: {}ms",
                   totalProducts, totalBatches, duration);
    }
}

