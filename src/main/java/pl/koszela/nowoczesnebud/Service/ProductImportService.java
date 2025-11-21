package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    private final PriceCalculationService priceCalculationService;
    private final DiscountCalculationService discountCalculationService;

    public ProductImportService(PriceCalculationService priceCalculationService,
                               DiscountCalculationService discountCalculationService) {
        this.priceCalculationService = priceCalculationService;
        this.discountCalculationService = discountCalculationService;
    }

    /**
     * Import z nazwami użytkownika (główna metoda)
     * Format pliku: "Manufacturer-GroupName.xlsx" lub własne nazwy
     */
    public List<Product> importProductsWithCustomNames(
            List<MultipartFile> files, 
            List<String> customGroupNames, 
            ProductCategory category) throws IOException {

        if (files.size() != customGroupNames.size()) {
            throw new IllegalArgumentException("Liczba plików musi być równa liczbie nazw");
        }

        List<Product> allProducts = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile multipartFile = files.get(i);
            String customGroupName = customGroupNames.get(i);

            File file = convertMultiPartToFile(multipartFile);
            String fileName = multipartFile.getOriginalFilename();

            // Mapowanie user-friendly nazw kolumn na nazwy techniczne
            // Obsługujemy zarówno polskie nazwy (z eksportu) jak i stare nazwy (z importu)
            File mappedFile = mapColumnNamesToTechnical(file, category);
            
            // DOKŁADNIE TAK SAMO jak CsvImporterImplTile - używamy Poiji
            // Poiji automatycznie mapuje kolumny na podstawie @ExcelCellName w Product.java
            // Product.java używa technicznych nazw kolumn (unitDetalP, quantityCo, basicDisc, etc.)
            List<Product> productsFromFile = Poiji.fromExcel(mappedFile, Product.class);
            
            // Ręczne uzupełnienie rabatów jeśli Poiji ich nie odczytało (problem z typami Integer)
            fillDiscountsFromExcel(mappedFile, productsFromFile);
            
            // Usuń tymczasowy plik z zmapowanymi nazwami
            if (mappedFile != file && mappedFile.exists()) {
                mappedFile.delete();
            }

            String manufacturer = getManufacturer(fileName);
            
            // Jeśli użytkownik nie podał nazwy grupy (lub jest pusta), wyciągnij z nazwy pliku
            String finalGroupName = customGroupName;
            if (finalGroupName == null || finalGroupName.trim().isEmpty()) {
                finalGroupName = extractGroupNameFromFileName(fileName);
            }
            
            System.out.println("🔹 Import pliku: " + fileName);
            System.out.println("   → Producent: " + manufacturer);
            System.out.println("   → Grupa: " + finalGroupName);

            for (Product product : productsFromFile) {
                product.setManufacturer(manufacturer);
                product.setGroupName(finalGroupName);
                product.setCategory(category);
                
                // DEBUG: Sprawdź wartości rabatów po zaimportowaniu
                System.out.println("🔍 DEBUG produkt: " + product.getName());
                System.out.println("   → basicDiscount: " + product.getBasicDiscount());
                System.out.println("   → additionalDiscount: " + product.getAdditionalDiscount());
                System.out.println("   → promotionDiscount: " + product.getPromotionDiscount());
                System.out.println("   → skontoDiscount: " + product.getSkontoDiscount());
                
                // ⭐ AUTOMATYCZNE MAPOWANIE NAZWY → mapperName
                // Jeśli Excel nie ma kolumny mapperName, generujemy z nazwy produktu
                if (product.getMapperName() == null || product.getMapperName().trim().isEmpty()) {
                    String mapperName = generateMapperNameFromProductName(product.getName());
                    product.setMapperName(mapperName);
                    System.out.println("🔹 Auto-mapowanie: '" + product.getName() + "' → mapperName: '" + mapperName + "'");
                }

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
                
                // Ustaw cenę sprzedaży = cena katalogowa (to co płaci klient!)
                // Zysk = (retailPrice - purchasePrice) × quantity
                if (product.getRetailPrice() > 0.00) {
                    product.setSellingPrice(product.getRetailPrice());
                    System.out.println("🔹 Cena sprzedaży = " + product.getRetailPrice() + " (zysk/szt: " + (product.getRetailPrice() - product.getPurchasePrice()) + ")");
                }
            }

            allProducts.addAll(productsFromFile);

            if (file.delete()) {
                System.out.println("Deleted temp file - " + file.getName());
            }
        }

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
        
        System.out.println("📦 Plik: '" + fileName + "' → Producent: '" + manufacturer + "'");
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
        
        System.out.println("📦 Plik: '" + fileName + "' → Grupa: '" + groupName + "'");
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
        System.out.println("Created temp file - " + tempFile.getName());
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
                // DACHÓWKI I RYNNY: Przelicznik zamiast Jednostka, bez Typ
                columnMapping.put("Przelicznik", "quantityCo");
                columnMapping.put("quantityCo", "quantityCo");
                columnMapping.put("quantityConverter", "quantityCo");
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
                        System.out.println("🔹 Mapowanie kolumny: '" + originalName + "' → '" + technicalName + "'");
                    } else if (technicalName == null) {
                        System.out.println("⚠️ Nieznana kolumna: '" + originalName + "' (indeks: " + i + ")");
                    }
                }
            }
            
            System.out.println("📋 Oryginalne nagłówki: " + originalHeaders);
            
            // Jeśli zmieniono nazwy, zapisz do tymczasowego pliku
            if (changed) {
                File tempFile = File.createTempFile("product-import-mapped-", ".xlsx");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    workbook.write(fos);
                }
                workbook.close();
                System.out.println("✅ Utworzono plik z zmapowanymi nazwami kolumn: " + tempFile.getName());
                
                // Weryfikacja - sprawdź nagłówki w nowym pliku
                try (Workbook verifyWorkbook = new XSSFWorkbook(new java.io.FileInputStream(tempFile))) {
                    Sheet verifySheet = verifyWorkbook.getSheetAt(0);
                    Row verifyHeaderRow = verifySheet.getRow(0);
                    List<String> mappedHeaders = new ArrayList<>();
                    for (int i = 0; i < verifyHeaderRow.getLastCellNum(); i++) {
                        Cell cell = verifyHeaderRow.getCell(i);
                        if (cell != null) {
                            mappedHeaders.add(cell.getStringCellValue());
                        }
                    }
                    System.out.println("✅ Zmapowane nagłówki: " + mappedHeaders);
                }
                
                return tempFile;
            }
            
            // Jeśli nie zmieniono, zwróć oryginalny plik
            workbook.close();
            return excelFile;
        } catch (Exception e) {
            if (workbook != null) {
                workbook.close();
            }
            System.err.println("❌ Błąd podczas mapowania nazw kolumn: " + e.getMessage());
            e.printStackTrace();
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
            
            // Znajdź indeksy kolumn z rabatami i metodą obliczania
            int basicDiscIndex = -1;
            int additionalIndex = -1;
            int promotionIndex = -1;
            int skontoIndex = -1;
            int discountCalculationMethodIndex = -1;
            
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
                    }
                }
            }
            
            System.out.println("🔍 Indeksy kolumn rabatów: basicDisc=" + basicDiscIndex + 
                             ", additional=" + additionalIndex + 
                             ", promotion=" + promotionIndex + 
                             ", skonto=" + skontoIndex +
                             ", discountCalculationMethod=" + discountCalculationMethodIndex);
            
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
                        if (intValue > 0) {
                            System.out.println("🔹 Odczytano basicDisc dla '" + product.getName() + "': " + intValue);
                        }
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
                        if (intValue > 0) {
                            System.out.println("🔹 Odczytano additional dla '" + product.getName() + "': " + intValue);
                        }
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
                        if (intValue > 0) {
                            System.out.println("🔹 Odczytano promotion dla '" + product.getName() + "': " + intValue);
                        }
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
                        if (intValue > 0) {
                            System.out.println("🔹 Odczytano skonto dla '" + product.getName() + "': " + intValue);
                        }
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
                                System.out.println("🔹 Odczytano metodę obliczania dla '" + product.getName() + "': " + method);
                            } catch (IllegalArgumentException e) {
                                System.out.println("⚠️ Nieprawidłowa metoda obliczania dla '" + product.getName() + "': " + methodValue);
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
                    System.out.println("🔹 Obliczono końcowy rabat dla '" + product.getName() + "': " + finalDiscount + "% (metoda: " + method + ")");
                } else {
                    // Jeśli brak metody, ustaw discount na 0 (lub można rzucić wyjątek)
                    System.out.println("⚠️ Brak metody obliczania dla '" + product.getName() + "' - ustawiono rabat na 0");
                    product.setDiscount(0.0);
                }
            }
        }
    }
}

