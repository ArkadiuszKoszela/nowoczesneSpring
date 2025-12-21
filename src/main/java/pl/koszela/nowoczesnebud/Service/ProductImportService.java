package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Import z Excel - DOKŁADNIE TA SAMA LOGIKA co CsvImporterImplTile
 * Używa Poiji + parsowanie nazw plików
 */
@Service
public class ProductImportService {

    private static final Logger logger = LoggerFactory.getLogger(ProductImportService.class);
    
    private final PriceCalculationService priceCalculationService;
    private final DiscountCalculationService discountCalculationService;

    public ProductImportService(PriceCalculationService priceCalculationService,
                               DiscountCalculationService discountCalculationService) {
        this.priceCalculationService = priceCalculationService;
        this.discountCalculationService = discountCalculationService;
    }

    /**
     * Import z nazwami użytkownika, producentami i grupami (główna metoda)
     * Jeśli producent/grupa nie są podane z frontendu, wyciąga z nazwy pliku jako fallback
     */
    public List<Product> importProductsWithCustomNames(
            List<MultipartFile> files, 
            List<String> customGroupNames,
            List<String> manufacturers,
            List<String> groupNames,
            ProductCategory category) throws IOException {

        if (files.size() != customGroupNames.size()) {
            throw new IllegalArgumentException("Liczba plików musi być równa liczbie nazw");
        }

        List<Product> allProducts = new ArrayList<>();

        long totalImportStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < files.size(); i++) {
            long fileStartTime = System.currentTimeMillis();
            MultipartFile multipartFile = files.get(i);
            String customGroupName = customGroupNames.get(i);
            String customManufacturer = (manufacturers != null && i < manufacturers.size()) ? manufacturers.get(i) : null;
            String customGroupNameFromParam = (groupNames != null && i < groupNames.size()) ? groupNames.get(i) : null;
            String fileName = multipartFile.getOriginalFilename();

            // 1. Konwersja MultipartFile -> File
            long convertStartTime = System.currentTimeMillis();
            File file = convertMultiPartToFile(multipartFile);
            long convertTime = System.currentTimeMillis() - convertStartTime;
            if (convertTime > 50) {
                logger.info("⏱️ [PERFORMANCE] Konwersja MultipartFile -> File: {}ms (plik: {})", convertTime, fileName);
            }

            // 2. Mapowanie user-friendly nazw kolumn na nazwy techniczne
            long mapStartTime = System.currentTimeMillis();
            File mappedFile = mapColumnNamesToTechnical(file, category);
            long mapTime = System.currentTimeMillis() - mapStartTime;
            if (mapTime > 50) {
                logger.info("⏱️ [PERFORMANCE] Mapowanie kolumn: {}ms (plik: {})", mapTime, fileName);
            }
            
            // 3. Parsowanie Excel (Poiji)
            long parseStartTime = System.currentTimeMillis();
            List<Product> productsFromFile = Poiji.fromExcel(mappedFile, Product.class);
            long parseTime = System.currentTimeMillis() - parseStartTime;
            if (parseTime > 100) {
                logger.info("⏱️ [PERFORMANCE] Parsowanie Excel (Poiji): {}ms | {} produktów (plik: {})", 
                           parseTime, productsFromFile.size(), fileName);
            }
            
            // 4. Ręczne uzupełnienie rabatów jeśli Poiji ich nie odczytało (problem z typami Integer)
            long fillStartTime = System.currentTimeMillis();
            fillDiscountsFromExcel(mappedFile, productsFromFile);
            long fillTime = System.currentTimeMillis() - fillStartTime;
            if (fillTime > 50) {
                logger.info("⏱️ [PERFORMANCE] Uzupełnienie rabatów: {}ms (plik: {})", fillTime, fileName);
            }
            
            long fileTotalTime = System.currentTimeMillis() - fileStartTime;
            if (fileTotalTime > 200) {
                logger.info("⏱️ [PERFORMANCE] Całkowity czas przetworzenia pliku: {}ms (plik: {})", fileTotalTime, fileName);
            }
            
            // Usuń tymczasowy plik z zmapowanymi nazwami
            if (mappedFile != file && mappedFile.exists()) {
                mappedFile.delete();
            }

            // ⚠️ WAŻNE: Frontend zawsze wysyła wartości (editableManufacturer i editableGroupName)
            // Frontend dba o walidację - wartości nie mogą być puste
            // Używamy BEZPOŚREDNIO wartości z frontendu (z sugestii), bez fallbacku do parsowania z nazwy pliku
            String manufacturer;
            if (customManufacturer != null && !customManufacturer.trim().isEmpty()) {
                // Frontend przesłał wartość - użyj jej BEZPOŚREDNIO (z sugestii)
                manufacturer = customManufacturer.trim();
            } else {
                // Frontend nie przesłał wartości - to nie powinno się zdarzyć (walidacja w frontendzie)
                // Fallback tylko dla bezpieczeństwa
                logger.warn("⚠️ Frontend nie przesłał producenta dla pliku: {} - używam fallback z nazwy pliku", fileName);
                manufacturer = getManufacturer(fileName);
            }
            
            // ⚠️ WAŻNE: Frontend zawsze wysyła wartości (editableGroupName)
            // Frontend dba o walidację - wartości nie mogą być puste
            // Używamy BEZPOŚREDNIO wartości z frontendu (z sugestii), bez fallbacku do parsowania z nazwy pliku
            // ⚠️ NOWA LOGIKA: Jeśli groupName[] jest wypełnione, używamy go jako finalGroupName
            // Jeśli groupName[] jest puste, używamy name[] (customGroupName) jako fallback dla finalGroupName
            // ⚠️ WAŻNE: Aby "Nazwa produktu w systemie" była częścią identyfikatora, jeśli użytkownik zmieni
            // tylko "Nazwa produktu w systemie" (a groupName pozostaje takie samo), to utworzy nowy cennik.
            // Więc jeśli groupName jest wypełnione, ale różne od customGroupName, to używamy kombinacji
            // manufacturer + groupName + customGroupName jako identyfikatora grupy.
            String finalGroupName;
            if (customGroupNameFromParam != null && !customGroupNameFromParam.trim().isEmpty()) {
                // Frontend przesłał wartość w groupName[] - użyj jej BEZPOŚREDNIO (z sugestii)
                // ⚠️ WAŻNE: groupName[] jest już poprawnie wyciągnięte z nazwy pliku przez frontend,
                // więc używamy go bezpośrednio bez dodatkowego parsowania
                // ⚠️ ZMIANA: NIE tworzymy kombinacji z name[], bo groupName[] jest już poprawne
                // Kombinacja była potrzebna tylko gdy użytkownik RZECZYWIŚCIE zmienił "Nazwa produktu w systemie"
                // ale w przypadku importu z pliku, name[] zawiera całą nazwę pliku (z producentem),
                // więc nie powinniśmy tworzyć kombinacji
                finalGroupName = customGroupNameFromParam.trim();
                logger.debug("🔍 Używam groupName[] bezpośrednio: '{}' (bez kombinacji z name[])", finalGroupName);
            } else if (customGroupName != null && !customGroupName.trim().isEmpty()) {
                // Frontend przesłał wartość w name[] - wyciągnij z niej tylko część grupy (bez producenta)
                finalGroupName = extractGroupNameFromCustomName(customGroupName.trim(), manufacturer);
                logger.debug("🔍 groupName[] puste, używam wyciągniętego z name[]: '{}' → '{}'", 
                            customGroupName, finalGroupName);
            } else {
                // Frontend nie przesłał wartości - to nie powinno się zdarzyć (walidacja w frontendzie)
                // Fallback tylko dla bezpieczeństwa
                logger.warn("⚠️ Frontend nie przesłał grupy produktowej dla pliku: {} - używam fallback z nazwy pliku", fileName);
                finalGroupName = extractGroupNameFromFileName(fileName);
            }
            
            // 5. Przetwarzanie produktów (ustawianie manufacturer, groupName, kalkulacje cen)
            // Mapowanie produktów do grup dla ustawienia displayOrder
            Map<String, List<Product>> productsByGroup = new HashMap<>();
            for (Product product : productsFromFile) {
                product.setManufacturer(manufacturer);
                product.setGroupName(finalGroupName);
                product.setCategory(category);
                
                // Grupuj produkty po manufacturer + groupName (dla ustawienia displayOrder w obrębie grupy)
                String groupKey = manufacturer + "|" + finalGroupName;
                productsByGroup.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(product);
                
                // ⭐ AUTOMATYCZNE MAPOWANIE NAZWY → mapperName
                // Jeśli Excel nie ma kolumny mapperName, generujemy z nazwy produktu
                if (product.getMapperName() == null || product.getMapperName().trim().isEmpty()) {
                    String mapperName = generateMapperNameFromProductName(product.getName());
                    product.setMapperName(mapperName);
                }
                
                // Normalizuj puste stringi na null dla productType i accessoryType
                if (product.getProductType() != null && product.getProductType().trim().isEmpty()) {
                    product.setProductType(null);
                }
                if (product.getAccessoryType() != null && product.getAccessoryType().trim().isEmpty()) {
                    product.setAccessoryType(null);
                }
                
                // ⚠️ WAŻNE: Logika domyślna dla productType jest przeniesiona do fillDiscountsFromExcel
                // aby była uruchamiana PO odczytaniu productType z Excel (jeśli Excel ma kolumnę "Typ produktu")

                // DOKŁADNIE TA SAMA LOGIKA KALKULACJI co w CsvImporterImplTile
                if (product.getRetailPrice() != 0.00 && product.getPurchasePrice() != 0.00) {
                    // Mamy obie ceny - nic nie rób
                } else if (product.getPurchasePrice() != 0.00) {
                    // Mamy cenę zakupu, oblicz cenę katalogową z marży
                    double retailPrice = priceCalculationService.calculateRetailPrice(product);
                    product.setRetailPrice(retailPrice);
                } else if (product.getRetailPrice() != 0.00) {
                    // Mamy cenę katalogową, oblicz cenę zakupu z rabatów
                    double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                    product.setPurchasePrice(purchasePrice);
                }
                
                // Ustaw cenę sprzedaży
                // Dla dachówek i rynien: cena sprzedaży = cena katalogowa (retailPrice)
                // Dla akcesoriów: cena sprzedaży = cena zakupu (purchasePrice) - domyślnie
                // Zysk = (sellingPrice - purchasePrice) × quantity
                if (product.getCategory() == ProductCategory.ACCESSORY) {
                    // Dla akcesoriów: domyślnie cena sprzedaży = cena zakupu
                    if (product.getPurchasePrice() != null && product.getPurchasePrice() > 0.00) {
                        product.setSellingPrice(product.getPurchasePrice());
                    }
                } else {
                    // Dla dachówek i rynien: cena sprzedaży = cena katalogowa
                    if (product.getRetailPrice() != null && product.getRetailPrice() > 0.00) {
                        product.setSellingPrice(product.getRetailPrice());
                    }
                }
            }
            
            // 6. Ustaw displayOrder dla produktów zgodnie z kolejnością wierszy w Excelu
            // ⚠️ WAŻNE: displayOrder powinno być ustawione zgodnie z kolejnością wierszy w Excelu (0, 1, 2, ...)
            // Jeśli Excel ma kolumnę "Lp", Poiji już ustawiła wartość (dzięki @ExcelCellName("Lp"))
            // Jeśli nie ma lub wartość jest null, ustawiamy na podstawie indeksu w productsFromFile (kolejność w Excelu)
            for (int excelRowIndex = 0; excelRowIndex < productsFromFile.size(); excelRowIndex++) {
                Product product = productsFromFile.get(excelRowIndex);
                
                // Jeśli produkt nie ma displayOrder (null) lub ma 0, ustaw na podstawie kolejności w Excelu
                if (product.getDisplayOrder() == null) {
                    // Excel nie ma kolumny "Lp" lub wartość jest pusta - ustaw zgodnie z kolejnością wierszy
                    product.setDisplayOrder(excelRowIndex);
                    logger.debug("🔢 Ustawiono displayOrder dla produktu '{}' na {} (kolejność w Excelu)", 
                                product.getName(), excelRowIndex);
                } else {
                    // Excel ma kolumnę "Lp" - normalizuj wartość (zaczynając od 0)
                    // Jeśli wartość jest już znormalizowana (>= 0), zostaw bez zmian
                    // Jeśli wartość jest ujemna lub bardzo duża, znormalizuj
                    int displayOrder = product.getDisplayOrder();
                    if (displayOrder < 0) {
                        // Wartość ujemna - znormalizuj do 0, 1, 2, ...
                        product.setDisplayOrder(excelRowIndex);
                        logger.debug("🔢 Znormalizowano displayOrder dla produktu '{}' z {} na {} (wartość ujemna)", 
                                    product.getName(), displayOrder, excelRowIndex);
                    }
                    // Jeśli displayOrder >= 0, zostaw bez zmian (Excel już ma poprawną wartość)
                }
            }
            
            // 7. Normalizuj displayOrder w obrębie każdej grupy (zaczynając od 0 dla każdej grupy)
            // ⚠️ WAŻNE: Produkty w różnych grupach mogą mieć takie same displayOrder (np. obie grupy zaczynają od 0)
            // Normalizujemy displayOrder w obrębie każdej grupy osobno, zachowując kolejność z Excela
            for (Map.Entry<String, List<Product>> entry : productsByGroup.entrySet()) {
                List<Product> groupProducts = entry.getValue();
                
                // Sortuj produkty po kolejności w Excelu (indeks w productsFromFile)
                // To zachowa oryginalną kolejność wierszy z Excela
                groupProducts.sort((p1, p2) -> {
                    int index1 = productsFromFile.indexOf(p1);
                    int index2 = productsFromFile.indexOf(p2);
                    return Integer.compare(index1, index2);
                });
                
                // Znormalizuj displayOrder w obrębie grupy (0, 1, 2, ...) zgodnie z kolejnością z Excela
                for (int j = 0; j < groupProducts.size(); j++) {
                    groupProducts.get(j).setDisplayOrder(j);
                }
                
                logger.debug("🔢 Znormalizowano displayOrder dla grupy '{}': {} produktów (0, 1, 2, ...) zgodnie z kolejnością z Excela", 
                            entry.getKey(), groupProducts.size());
            }
            
            allProducts.addAll(productsFromFile);

            file.delete();
        }
        
        long totalImportTime = System.currentTimeMillis() - totalImportStartTime;
        logger.info("⏱️ [PERFORMANCE] Import z Excel - CAŁKOWITY CZAS: {}ms ({}s) | {} plików | {} produktów", 
                   totalImportTime, totalImportTime / 1000.0, files.size(), allProducts.size());

        return allProducts;
    }

    /**
     * Wyciąga producenta z nazwy pliku
     * Producent = TYLKO pierwsze słowo
     * 
     * Przykłady:
     * "CANTUS łupek ang-NUANE.xlsx" -> "CANTUS"
     * "BRAAS czerwona-FINESSE.xlsx" -> "BRAAS"
     * "CREATON-NUANE.xlsx" -> "CREATON"
     */
    private String getManufacturer(String fileName) {
        String nameWithoutExtension = StringUtils.substringBeforeLast(fileName, ".");
        
        // Producent = pierwsze słowo (do pierwszej spacji lub myślnika)
        String manufacturer = nameWithoutExtension.split("[\\s-]")[0].trim();
        
        return manufacturer;
    }
    
    /**
     * Wyciąga grupę produktową z nazwy pliku
     * Grupa = wszystko poza producentem (łącznie z częścią przed i po myślniku)
     * 
     * Przykłady:
     * "CANTUS łupek ang-NUANE.xlsx" -> "łupek ang NUANE"
     * "CANTUS czerwona glaz-FINESSE.xlsx" -> "czerwona glaz FINESSE"
     * "CANTUS-NOBLESSE.xlsx" -> "NOBLESSE"
     */
    private String extractGroupNameFromFileName(String fileName) {
        String nameWithoutExtension = StringUtils.substringBeforeLast(fileName, ".");
        
        // Usuń producenta (pierwsze słowo)
        String[] parts = nameWithoutExtension.split("[\\s-]", 2);
        if (parts.length < 2) {
            return nameWithoutExtension; // Jeśli nie ma spacji/myślnika, zwróć całość
        }
        
        // Wszystko poza pierwszym słowem = grupa produktowa
        // Zamień myślniki na spacje dla czytelności
        String groupName = parts[1].replace("-", " ").trim();
        
        return groupName;
    }
    
    /**
     * Wyciąga tylko nazwę grupy z "Nazwa produktu w systemie" (customGroupName/name[]).
     * Jeśli customGroupName zawiera producenta (np. "BORHOLM-czerwień naturalna"),
     * to wyciąga tylko część grupy (np. "czerwień naturalna").
     * 
     * Przykłady:
     * "BORHOLM-czerwień naturalna" + manufacturer="BORHOLM" -> "czerwień naturalna"
     * "czerwień naturalna" + manufacturer="BORHOLM" -> "czerwień naturalna" (już bez producenta)
     * "CANTUS łupek ang-NUANE" + manufacturer="CANTUS" -> "łupek ang NUANE"
     */
    private String extractGroupNameFromCustomName(String customGroupName, String manufacturer) {
        if (customGroupName == null || customGroupName.trim().isEmpty()) {
            return customGroupName;
        }
        
        String trimmed = customGroupName.trim();
        
        // Jeśli customGroupName zaczyna się od producenta (z spacją lub myślnikiem),
        // to wyciągnij tylko część po producencie
        if (manufacturer != null && !manufacturer.trim().isEmpty()) {
            String manufacturerTrimmed = manufacturer.trim();
            
            // Sprawdź czy customGroupName zaczyna się od producenta
            if (trimmed.startsWith(manufacturerTrimmed)) {
                // Usuń producenta z początku (z spacją lub myślnikiem)
                String withoutManufacturer = trimmed.substring(manufacturerTrimmed.length()).trim();
                
                // Jeśli po producencie jest separator (spacja lub myślnik), usuń go
                if (withoutManufacturer.startsWith("-") || withoutManufacturer.startsWith(" ")) {
                    withoutManufacturer = withoutManufacturer.substring(1).trim();
                }
                
                // Zamień myślniki na spacje dla czytelności
                String result = withoutManufacturer.replace("-", " ").trim();
                logger.debug("🔍 extractGroupNameFromCustomName: '{}' + manufacturer='{}' → '{}'", 
                            customGroupName, manufacturer, result);
                return result;
            }
        }
        
        // Jeśli nie zaczyna się od producenta, użyj tej samej logiki co extractGroupNameFromFileName
        // (wyciągnij wszystko po pierwszym słowie)
        String[] parts = trimmed.split("[\\s-]", 2);
        if (parts.length < 2) {
            logger.debug("🔍 extractGroupNameFromCustomName: '{}' → '{}' (brak separatora)", 
                        customGroupName, trimmed);
            return trimmed; // Jeśli nie ma spacji/myślnika, zwróć całość
        }
        
        // Wszystko poza pierwszym słowem = grupa produktowa
        String groupName = parts[1].replace("-", " ").trim();
        logger.debug("🔍 extractGroupNameFromCustomName: '{}' → '{}' (split po pierwszym słowie)", 
                    customGroupName, groupName);
        return groupName;
    }

    /**
     * Konwersja MultipartFile -> File
     */
    private File convertMultiPartToFile(MultipartFile multipartFile) throws IOException {
        File tempFile = File.createTempFile("product-import-", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }
        return tempFile;
    }
    
    /**
     * Generuje mapperName z nazwy produktu
     * Dokładnie tak samo jak w starym systemie CsvImporterImplTile
     * 
     * Przykłady:
     * "Dachówka podstawowa" → "Powierzchnia polaci"
     * "Dachówka krawędziowa lewa" → "dlugosc krawedzi lewych"
     * "Gąsior początkowy" → "gasiar podstawowy"
     */
    private String generateMapperNameFromProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return null;
        }
        
        String name = productName.toLowerCase().trim();
        
        // DACHÓWKI - wszystkie podstawowe dachówki = "Powierzchnia polaci"
        if (name.contains("dachówka podstawowa") || 
            name.contains("dachowka podstawowa") ||
            name.contains("dachówka zwykła") ||
            name.equals("dachówka") ||
            name.equals("dachowka")) {
            return "Powierzchnia polaci";
        }
        
        // DACHÓWKI KRAWĘDZIOWE
        if (name.contains("dachówka krawędziowa lewa") || name.contains("dachowka krawędziowa lewa")) {
            return "dlugosc krawedzi lewych";
        }
        if (name.contains("dachówka krawędziowa prawa") || name.contains("dachowka krawędziowa prawa")) {
            return "dlugosc krawedzi prawych";
        }
        
        // DACHÓWKI DWUFALOWE
        if (name.contains("dachówka dwufalowa") || name.contains("dachowka dwufalowa") ||
            name.contains("dachówka krawędziowa dwufalowa") || name.contains("dachowka krawędziowa dwufalowa")) {
            return "dachowka dwufalowa";
        }
        
        // DACHÓWKI WENTYLACYJNE
        if (name.contains("dachówka wentylacyjna") || name.contains("dachowka wentylacyjna")) {
            return "dachowka wentylacyjna";
        }
        
        // GĄSIORY
        if (name.contains("gąsior początkowy") || name.contains("gasior początkowy") ||
            name.contains("gąsior podstawowy") || name.contains("gasior podstawowy")) {
            return "gasiar podstawowy";
        }
        if (name.contains("gąsior końcowy") || name.contains("gasior końcowy")) {
            return "gasior koncowy";
        }
        if (name.contains("gąsior zaokrąglony") || name.contains("gasior zaokraglony")) {
            return "gasior zaokraglony";
        }
        if (name.contains("gąsior z podwójną mufą") || name.contains("gasior z podwójna mufa")) {
            return "gasior z podwojna mufa";
        }
        
        // KOMIN/WENTYLACJA
        if (name.contains("kominewk") || name.contains("kominek wentylacyjny")) {
            return "komplet kominka wentylacyjnego";
        }
        if (name.contains("obwód komina")) {
            return "obwod komina";
        }
        
        // TRÓJNIK/CZWÓRNIK
        if (name.contains("trójnik") || name.contains("trojnik")) {
            return "trojnik";
        }
        if (name.contains("czwórnik") || name.contains("czwornik")) {
            return "czwornik";
        }
        
        // OKNO
        if (name.contains("okno połaciowe") || name.contains("okno polaciowe")) {
            return "okno polaciowe";
        }
        
        // AKCESORIA - okapy, kalenie, kosze
        if (name.contains("kratka okapu") || name.contains("grzebień okapu") || 
            name.contains("grzebien okapu") || name.contains("okapu")) {
            return "dlugosc okapu";
        }
        if (name.contains("wspornik łaty") || name.contains("wspornik laty") || 
            name.contains("taśma kalenicy") || name.contains("tasma kalenicy")) {
            return "dlugosc kalenic";
        }
        if (name.contains("klin")) {
            return "dlugosc koszy";
        }
        if (name.contains("folia")) {
            return "Powierzchnia polaci";
        }
        
        // RYNNY
        if (name.contains("rynna 3") || name.contains("rynna 3mb")) {
            return "rynna 3mb";
        }
        if (name.contains("rynna 4") || name.contains("rynna 4mb")) {
            return "rynna 4mb";
        }
        if (name.contains("narożnik wewnętrzny") || name.contains("naroznik wewntrzny")) {
            return "narożnik wewntrzny";
        }
        if (name.contains("narożnik zewnętrzny") || name.contains("naroznik zewnetrzny")) {
            return "narożnik zewnętrzny";
        }
        if (name.contains("złączka rynny") || name.contains("zlaczka rynny")) {
            return "złączka rynny";
        }
        if (name.contains("denko")) {
            return "denko";
        }
        if (name.contains("lej spustowy")) {
            return "lej spustowy";
        }
        
        // DOMYŚLNIE - zwróć null (nie mapujemy)
        return null;
    }

    /**
     * Mapuje user-friendly nazwy kolumn na techniczne nazwy dla Poiji
     * Obsługuje zarówno polskie nazwy (z eksportu) jak i stare nazwy (z importu)
     * 
     * Mapowanie różni się w zależności od kategorii:
     * 
     * Dla AKCESORIÓW (ACCESSORY):
     * - "Nazwa" / "name" → "name"
     * - "Cena katalogowa" / "unitDetalPrice" / "unitDetalP" → "unitDetalP"
     * - "Jednostka" / "unit" → "unit"
     * - "Rabat podstawowy" / "basicDiscount" / "basicDisc" → "basicDisc"
     * - "Rabat dodatkowy" / "additionalDiscount" / "additional" → "additional"
     * - "Rabat promocyjny" / "promotionDiscount" / "promotion" → "promotion"
     * - "Skonto" / "skonto" → "skonto"
     * - "Typ" / "type" / "accessoryType" → "type"
     * 
     * Dla DACHÓWEK I RYNNEN (TILE, GUTTER):
     * - "Nazwa" / "name" → "name"
     * - "Cena katalogowa" / "unitDetalP" / "detalPrice" → "unitDetalP"
     * - "Przelicznik" / "quantityCo" / "quantityConverter" → "quantityCo"
     * - "Rabat podstawowy" / "basicDisc" / "basicDiscount" → "basicDisc"
     * - "Rabat dodatkowy" / "additional" / "additionalDiscount" → "additional"
     * - "Rabat promocyjny" / "promotion" / "promotionDiscount" → "promotion"
     * - "Skonto" / "skonto" → "skonto"
     */
    private File mapColumnNamesToTechnical(File excelFile, ProductCategory category) throws IOException {
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook(new java.io.FileInputStream(excelFile));
            Sheet sheet = workbook.getSheetAt(0);
            
            // Pobierz pierwszy wiersz (nagłówki)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                workbook.close();
                return excelFile; // Brak nagłówków - zwróć oryginalny plik
            }
            
            // Mapowanie nazw kolumn - różne dla różnych kategorii
            Map<String, String> columnMapping = new HashMap<>();
            
            // Wspólne dla wszystkich kategorii
            columnMapping.put("Nazwa", "name");
            columnMapping.put("name", "name");
            
            columnMapping.put("Cena katalogowa", "unitDetalP");
            columnMapping.put("unitDetalP", "unitDetalP");
            columnMapping.put("unitDetalPrice", "unitDetalP");
            columnMapping.put("detalPrice", "unitDetalP");
            
            columnMapping.put("Rabat podstawowy", "basicDisc");
            columnMapping.put("basicDisc", "basicDisc");
            columnMapping.put("basicDiscount", "basicDisc");
            
            columnMapping.put("Rabat dodatkowy", "additional");
            columnMapping.put("additional", "additional");
            columnMapping.put("additionalDiscount", "additional");
            
            columnMapping.put("Rabat promocyjny", "promotion");
            columnMapping.put("promotion", "promotion");
            columnMapping.put("promotionDiscount", "promotion");
            
            columnMapping.put("Skonto", "skonto");
            columnMapping.put("skonto", "skonto");
            columnMapping.put("skontoDiscount", "skonto");
            
            // Sposób obliczania rabatu
            columnMapping.put("Sposób obliczania rabatu", "discountCalculationMethod");
            columnMapping.put("discountCalculationMethod", "discountCalculationMethod");
            columnMapping.put("Discount Calculation Method", "discountCalculationMethod");
            
            // Różne dla różnych kategorii
            if (category == ProductCategory.ACCESSORY) {
                // AKCESORIA: Jednostka zamiast Przelicznik, plus Typ
                columnMapping.put("Jednostka", "unit");
                columnMapping.put("unit", "unit");
                
                columnMapping.put("Typ", "type");
                columnMapping.put("type", "type");
                columnMapping.put("accessoryType", "type");
            } else {
                // DACHÓWKI I RYNNY: Przelicznik zamiast Jednostka, plus Typ produktu
                columnMapping.put("Przelicznik", "quantityCo");
                columnMapping.put("quantityCo", "quantityCo");
                columnMapping.put("quantityConverter", "quantityCo");
                
                columnMapping.put("Typ produktu", "productType");
                columnMapping.put("productType", "productType");
            }
            
            // Zmień nazwy kolumn w nagłówku
            boolean changed = false;
            List<String> originalHeaders = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String originalName = cell.getStringCellValue().trim();
                    originalHeaders.add(originalName);
                    String technicalName = columnMapping.get(originalName);
                    
                    if (technicalName != null && !technicalName.equals(originalName)) {
                        cell.setCellValue(technicalName);
                        changed = true;
                    }
                }
            }
            
            // Jeśli zmieniono nazwy, zapisz do tymczasowego pliku
            if (changed) {
                File tempFile = File.createTempFile("product-import-mapped-", ".xlsx");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    workbook.write(fos);
                }
                workbook.close();
                
                return tempFile;
            }
            
            // Jeśli nie zmieniono, zwróć oryginalny plik
            workbook.close();
            return excelFile;
        } catch (Exception e) {
            if (workbook != null) {
                workbook.close();
            }
            logger.error("Błąd podczas mapowania nazw kolumn: {}", e.getMessage(), e);
            return excelFile; // W razie błędu zwróć oryginalny plik
        }
    }

    /**
     * Ręcznie odczytuje wartości rabatów z Excel jeśli Poiji ich nie odczytało
     * Problem: Poiji może mieć problem z konwersją Double -> Integer dla rabatów
     */
    private void fillDiscountsFromExcel(File excelFile, List<Product> products) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(new java.io.FileInputStream(excelFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            if (headerRow == null || products.isEmpty()) {
                return;
            }
            
            // Znajdź indeksy kolumn z rabatami, metodą obliczania i typem produktu
            int basicDiscIndex = -1;
            int additionalIndex = -1;
            int promotionIndex = -1;
            int skontoIndex = -1;
            int discountCalculationMethodIndex = -1;
            int productTypeIndex = -1;
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String headerName = cell.getStringCellValue().trim();
                    if (headerName.equals("basicDisc")) {
                        basicDiscIndex = i;
                    } else if (headerName.equals("additional")) {
                        additionalIndex = i;
                    } else if (headerName.equals("promotion")) {
                        promotionIndex = i;
                    } else if (headerName.equals("skonto")) {
                        skontoIndex = i;
                    } else if (headerName.equals("discountCalculationMethod")) {
                        discountCalculationMethodIndex = i;
                    } else if (headerName.equals("productType") || headerName.equals("Typ produktu")) {
                        // Sprawdź zarówno "productType" (po mapowaniu) jak i "Typ produktu" (oryginalna nazwa)
                        productTypeIndex = i;
                    }
                }
            }
            
            // Uzupełnij rabaty dla każdego produktu
            for (int rowIndex = 0; rowIndex < products.size() && rowIndex + 1 < sheet.getLastRowNum() + 1; rowIndex++) {
                Product product = products.get(rowIndex);
                Row dataRow = sheet.getRow(rowIndex + 1); // +1 bo pierwszy wiersz to nagłówek
                
                if (dataRow == null) {
                    continue;
                }
                
                // Odczytaj basicDisc - zawsze odczytaj z Excel (Poiji może mieć problem z Integer)
                if (basicDiscIndex >= 0) {
                    Cell cell = dataRow.getCell(basicDiscIndex);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        double value = 0.0;
                        if (cell.getCellType() == CellType.NUMERIC) {
                            value = cell.getNumericCellValue();
                        } else if (cell.getCellType() == CellType.STRING) {
                            try {
                                value = Double.parseDouble(cell.getStringCellValue().trim());
                            } catch (NumberFormatException e) {
                                value = 0.0;
                            }
                        }
                        int intValue = (int) Math.round(value);
                        product.setBasicDiscount(intValue);
                    }
                }
                
                // Odczytaj additional - zawsze odczytaj z Excel
                if (additionalIndex >= 0) {
                    Cell cell = dataRow.getCell(additionalIndex);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        double value = 0.0;
                        if (cell.getCellType() == CellType.NUMERIC) {
                            value = cell.getNumericCellValue();
                        } else if (cell.getCellType() == CellType.STRING) {
                            try {
                                value = Double.parseDouble(cell.getStringCellValue().trim());
                            } catch (NumberFormatException e) {
                                value = 0.0;
                            }
                        }
                        int intValue = (int) Math.round(value);
                        product.setAdditionalDiscount(intValue);
                    }
                }
                
                // Odczytaj promotion - zawsze odczytaj z Excel
                if (promotionIndex >= 0) {
                    Cell cell = dataRow.getCell(promotionIndex);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        double value = 0.0;
                        if (cell.getCellType() == CellType.NUMERIC) {
                            value = cell.getNumericCellValue();
                        } else if (cell.getCellType() == CellType.STRING) {
                            try {
                                value = Double.parseDouble(cell.getStringCellValue().trim());
                            } catch (NumberFormatException e) {
                                value = 0.0;
                            }
                        }
                        int intValue = (int) Math.round(value);
                        product.setPromotionDiscount(intValue);
                    }
                }
                
                // Odczytaj skonto - zawsze odczytaj z Excel
                if (skontoIndex >= 0) {
                    Cell cell = dataRow.getCell(skontoIndex);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        double value = 0.0;
                        if (cell.getCellType() == CellType.NUMERIC) {
                            value = cell.getNumericCellValue();
                        } else if (cell.getCellType() == CellType.STRING) {
                            try {
                                value = Double.parseDouble(cell.getStringCellValue().trim());
                            } catch (NumberFormatException e) {
                                value = 0.0;
                            }
                        }
                        int intValue = (int) Math.round(value);
                        product.setSkontoDiscount(intValue);
                    }
                }
                
                // Odczytaj metodę obliczania rabatu
                DiscountCalculationMethod method = null;
                if (discountCalculationMethodIndex >= 0) {
                    Cell cell = dataRow.getCell(discountCalculationMethodIndex);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String methodValue = "";
                        if (cell.getCellType() == CellType.STRING) {
                            methodValue = cell.getStringCellValue().trim();
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            methodValue = String.valueOf((int) cell.getNumericCellValue());
                        }
                        
                        if (!methodValue.isEmpty()) {
                            try {
                                method = DiscountCalculationMethod.valueOf(methodValue.toUpperCase());
                                product.setDiscountCalculationMethod(method);
                            } catch (IllegalArgumentException e) {
                                logger.warn("Nieprawidłowa metoda obliczania rabatu dla produktu '{}': {}", product.getName(), methodValue);
                            }
                        }
                    }
                }
                
                // Oblicz końcowy rabat na podstawie metody i 4 rabatów
                if (method != null) {
                    double finalDiscount = discountCalculationService.calculateDiscount(
                        method,
                        product.getBasicDiscount(),
                        product.getAdditionalDiscount(),
                        product.getPromotionDiscount(),
                        product.getSkontoDiscount()
                    );
                    product.setDiscount(finalDiscount);
                } else {
                    // Jeśli brak metody, ustaw discount na 0
                    product.setDiscount(0.0);
                }
                
                // Odczytaj productType - zawsze odczytaj z Excel (dla pewności)
                if (productTypeIndex >= 0) {
                    Cell cell = dataRow.getCell(productTypeIndex);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String productTypeValue = "";
                        if (cell.getCellType() == CellType.STRING) {
                            productTypeValue = cell.getStringCellValue().trim();
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            productTypeValue = String.valueOf((int) cell.getNumericCellValue());
                        }
                        // Ustaw null jeśli wartość jest pusta (dla zgodności z bazą danych)
                        if (productTypeValue.isEmpty()) {
                            product.setProductType(null);
                        } else {
                            product.setProductType(productTypeValue);
                        }
                    } else {
                        // Komórka jest pusta - ustaw null (zachowaj null, nie ustawiaj domyślnej wartości)
                        product.setProductType(null);
                    }
                } else {
                    // ⚠️ WAŻNE: Excel NIE MA kolumny "Typ produktu" - ustaw domyślny productType
                    // Tylko w tym przypadku ustawiamy domyślną wartość
                    if (product.getProductType() == null || product.getProductType().trim().isEmpty()) {
                        String productName = product.getName() != null ? product.getName().trim() : "";
                        
                        if ("Dachówka podstawowa".equals(productName)) {
                            // Jeśli nazwa to dokładnie "Dachówka podstawowa", ustaw productType na "Dachówka podstawowa"
                            product.setProductType("Dachówka podstawowa");
                            logger.debug("🔧 Ustawiono domyślny productType 'Dachówka podstawowa' dla produktu '{}' (Excel nie ma kolumny 'Typ produktu')", productName);
                        } else {
                            // W przeciwnym razie ustaw domyślnie "Akcesoria ceramiczne"
                            product.setProductType("Akcesoria ceramiczne");
                            logger.debug("🔧 Ustawiono domyślny productType 'Akcesoria ceramiczne' dla produktu '{}' (Excel nie ma kolumny 'Typ produktu')", productName);
                        }
                    }
                }
            }
        }
    }
}

