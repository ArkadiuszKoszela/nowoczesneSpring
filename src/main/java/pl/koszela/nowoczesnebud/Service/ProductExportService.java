package pl.koszela.nowoczesnebud.Service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Eksport produktów do Excel - DOKŁADNIE TAKI SAM FORMAT jak import
 * Format pliku: "Manufacturer-GroupName.xlsx"
 * Struktura Excel zgodna z @ExcelCellName w Product.java
 */
@Service
public class ProductExportService {

    private static final Logger logger = LoggerFactory.getLogger(ProductExportService.class);

    /**
     * Eksportuj produkty do ZIP z plikami Excel
     * Każdy plik Excel = jedna grupa produktów (Manufacturer-GroupName.xlsx)
     * 
     * @param products Lista produktów do eksportu
     * @return byte[] - plik ZIP z plikami Excel
     */
    public byte[] exportToExcelZip(List<Product> products) throws IOException {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Brak produktów do eksportu");
        }

        // Określ kategorię z pierwszego produktu (wszystkie produkty powinny być tej samej kategorii)
        ProductCategory category = products.get(0).getCategory();
        if (category == null) {
            throw new IllegalArgumentException("Produkty muszą mieć przypisaną kategorię");
        }

        logger.info("📊 Eksportowanie {} produktów kategorii {}", products.size(), category);

        // Grupuj produkty po producencie i grupie
        Map<String, List<Product>> groupedProducts = groupProductsByManufacturerAndGroup(products);

        logger.info("📁 Utworzono {} grup produktów do eksportu", groupedProducts.size());

        // Utwórz ZIP w pamięci
        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
        
        int filesAdded = 0;
        try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            // Ustaw kodowanie UTF-8 dla nazw plików (obsługa polskich znaków)
            zipOut.setComment("Eksport produktów - " + category.name());
            
            // Dla każdej grupy utwórz plik Excel
            for (Map.Entry<String, List<Product>> entry : groupedProducts.entrySet()) {
                String fileName = entry.getKey() + ".xlsx";
                // Usuń nieprawidłowe znaki z nazwy pliku (Windows nie lubi niektórych znaków)
                fileName = sanitizeFileName(fileName);
                List<Product> groupProducts = entry.getValue();
                
                logger.info("📄 Tworzenie pliku Excel: {} ({} produktów)", fileName, groupProducts.size());
                
                // Utwórz plik Excel dla tej grupy (z kategorią)
                byte[] excelFile = createExcelFile(groupProducts, category);
                logger.info("📊 Plik Excel utworzony: {} - {} bajtów", fileName, excelFile.length);
                
                // Dodaj do ZIP
                ZipEntry zipEntry = new ZipEntry(fileName);
                // ⚠️ NIE ustawiamy setSize() - może powodować problemy z kompresją
                zipOut.putNextEntry(zipEntry);
                zipOut.write(excelFile);
                zipOut.closeEntry();
                // ⚠️ NIE wywołujemy flush() tutaj - może powodować problemy
                filesAdded++;
                
                // Sprawdź rozmiar ZIP po każdym dodaniu
                long zipSizeAfter = zipOutputStream.size();
                logger.info("✅ Dodano do ZIP: {} ({} bajtów Excel) | Rozmiar ZIP po dodaniu: {} bajtów", 
                    fileName, excelFile.length, zipSizeAfter);
            }
            
            // ⚠️ NIE wywołujemy finish() - close() w try-with-resources zrobi to automatycznie
            logger.info("📦 ZipOutputStream - wszystkie wpisy dodane, zamykanie strumienia...");
        }

        byte[] zipBytes = zipOutputStream.toByteArray();
        logger.info("✅ ZIP utworzony: {} plików Excel, {} bajtów", filesAdded, zipBytes.length);
        
        if (filesAdded == 0) {
            logger.warn("⚠️ UWAGA: ZIP jest pusty - brak plików Excel! Sprawdź czy produkty mają ustawione manufacturer i groupName");
        } else if (zipBytes.length == 0) {
            logger.error("❌ BŁĄD: ZIP ma rozmiar 0 bajtów mimo {} dodanych plików!", filesAdded);
        } else {
            logger.info("✅ ZIP gotowy do pobrania: {} plików, {} bajtów", filesAdded, zipBytes.length);
        }
        
        return zipBytes;
    }

    /**
     * Grupuj produkty po producencie i grupie
     * Klucz: "Manufacturer-GroupName"
     * Produkty bez manufacturer lub groupName są eksportowane z domyślnymi wartościami "BRAK_MANUFACTURER" i "BRAK_GROUP"
     */
    private Map<String, List<Product>> groupProductsByManufacturerAndGroup(List<Product> products) {
        int totalProducts = products.size();
        AtomicInteger productsWithMissingFields = new AtomicInteger(0);
        
        Map<String, List<Product>> grouped = products.stream()
            .collect(Collectors.groupingBy(product -> {
                String manufacturer = product.getManufacturer();
                String groupName = product.getGroupName();
                
                // Sprawdź czy pola są puste lub null
                boolean hasManufacturer = manufacturer != null && !manufacturer.trim().isEmpty();
                boolean hasGroupName = groupName != null && !groupName.trim().isEmpty();
                
                if (!hasManufacturer || !hasGroupName) {
                    productsWithMissingFields.incrementAndGet();
                    logger.warn("⚠️ Produkt bez wymaganych pól: ID={}, name={}, manufacturer={}, groupName={}", 
                        product.getId(), product.getName(), manufacturer, groupName);
                }
                
                // Użyj domyślnych wartości jeśli brak
                String finalManufacturer = hasManufacturer ? manufacturer.trim() : "BRAK_MANUFACTURER";
                String finalGroupName = hasGroupName ? groupName.trim() : "BRAK_GROUP";
                
                // ⚠️ WAŻNE: Format musi być DOKŁADNIE taki sam jak importowane pliki!
                // Importowane pliki mają format: "Manufacturer-GroupName.xlsx" (z myślnikiem)
                // Przykład: "CANTUS-czarna ang NUANE.xlsx", "BORHOLM-miedziana ang.xlsx"
                // 
                // Import używa getManufacturer() który dzieli: split("[\\s-]")[0] - pierwsza część przed spacją/myślnikiem
                // Import używa extractGroupNameFromFileName() który dzieli: split("[\\s-]", 2)[1] - wszystko po pierwszej spacji/myślniku
                // 
                // ⚠️ WAŻNE: Manufacturer nie może zawierać myślnika (bo to jest separator), więc zamień myślniki w manufacturer na podkreślenia
                // Spacje w manufacturer też zamień na podkreślenia (dla spójności)
                String sanitizedManufacturer = finalManufacturer.replace(" ", "_").replace("-", "_");
                
                // Format: "Manufacturer-GroupName" (z myślnikiem) - DOKŁADNIE taki sam jak importowane pliki
                return sanitizedManufacturer + "-" + finalGroupName;
            }));
        
        int missingFieldsCount = productsWithMissingFields.get();
        if (missingFieldsCount > 0) {
            logger.warn("⚠️ UWAGA: {} z {} produktów ma brakujące pola manufacturer lub groupName (używam domyślnych wartości)", 
                missingFieldsCount, totalProducts);
        }
        
        logger.info("📊 Grupowanie: {} produktów pogrupowanych w {} grup", totalProducts, grouped.size());
        
        return grouped;
    }

    /**
     * Utwórz plik Excel dla grupy produktów
     * Struktura zgodna z @ExcelCellName w Product.java
     * 
     * @param products Lista produktów
     * @param category Kategoria produktów (TILE, GUTTER, ACCESSORY)
     */
    private byte[] createExcelFile(List<Product> products, ProductCategory category) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produkty");
            
            // Styl nagłówka
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Styl komórek
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            
            // Styl dla liczb (z przecinkiem)
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(cellStyle);
            DataFormat numberFormat = workbook.createDataFormat();
            numberStyle.setDataFormat(numberFormat.getFormat("#,##0.00"));
            
            // Styl dla liczb całkowitych (bez przecinka) - dla displayOrder
            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.cloneStyleFrom(cellStyle);
            integerStyle.setDataFormat(numberFormat.getFormat("0")); // Format integer bez przecinka
            
            // Utwórz nagłówki - różne dla różnych kategorii
            Row headerRow = sheet.createRow(0);
            String[] headers;
            
            if (category == ProductCategory.ACCESSORY) {
                // AKCESORIA: Lp, name, unitDetalPrice, unit, quantityConverter, basicDiscount, additionalDiscount, promotionDiscount, skonto, discountCalculationMethod, type
                headers = new String[]{
                    "Lp",                              // displayOrder (liczba porządkowa)
                    "Nazwa",                           // name
                    "Cena katalogowa",                 // unitDetalPrice
                    "Jednostka",                       // unit
                    "Przelicznik",                     // quantityConverter
                    "Rabat podstawowy",                // basicDiscount
                    "Rabat dodatkowy",                 // additionalDiscount
                    "Rabat promocyjny",                // promotionDiscount
                    "Skonto",                          // skonto
                    "Sposób obliczania rabatu",        // discountCalculationMethod
                    "Typ"                              // type
                };
            } else {
                // DACHÓWKI I RYNNY: Lp, name, unitDetalP, unit, quantityCo, basicDisc, additional, promotion, skonto, discountCalculationMethod, productType
                headers = new String[]{
                    "Lp",                              // displayOrder (liczba porządkowa)
                    "Nazwa",                           // name
                    "Cena katalogowa",                 // unitDetalP
                    "Jednostka",                       // unit
                    "Przelicznik",                     // quantityCo
                    "Rabat podstawowy",                // basicDisc
                    "Rabat dodatkowy",                 // additional
                    "Rabat promocyjny",                // promotion
                    "Skonto",                          // skonto
                    "Sposób obliczania rabatu",        // discountCalculationMethod
                    "Typ produktu"                     // productType
                };
            }
            
            // Utwórz nagłówki
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Utwórz wiersze z produktami
            // WAŻNE: Kolejność kolumn musi być identyczna jak w nagłówkach!
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                int colIndex = 0;
                
                // Lp (kolumna 0) - displayOrder + 1 (dla użytkownika: 1, 2, 3, ... zamiast 0, 1, 2, ...)
                // ⚠️ WAŻNE: Używamy integerStyle (format "0") zamiast numberStyle (format "#,##0.00")
                // aby liczba porządkowa była zawsze wyświetlana jako integer bez przecinka
                Integer displayOrder = product.getDisplayOrder() != null ? product.getDisplayOrder() : 0;
                createNumericCell(row, colIndex++, (double)(displayOrder + 1), integerStyle);
                
                // name (kolumna 1)
                createCell(row, colIndex++, product.getName(), cellStyle);
                
                // Cena katalogowa (kolumna 2)
                createNumericCell(row, colIndex++, product.getRetailPrice(), numberStyle);
                
                // Jednostka (kolumna 3) - dla wszystkich kategorii
                createCell(row, colIndex++, product.getUnit(), cellStyle);
                
                // Przelicznik (kolumna 4) - dla wszystkich kategorii
                createNumericCell(row, colIndex++, product.getQuantityConverter(), numberStyle);
                
                // Rabaty (kolumny 5-7)
                createNumericCell(row, colIndex++, product.getBasicDiscount() != null ? product.getBasicDiscount().doubleValue() : 0.0, numberStyle);
                createNumericCell(row, colIndex++, product.getAdditionalDiscount() != null ? product.getAdditionalDiscount().doubleValue() : 0.0, numberStyle);
                createNumericCell(row, colIndex++, product.getPromotionDiscount() != null ? product.getPromotionDiscount().doubleValue() : 0.0, numberStyle);
                
                // skonto (kolumna 7)
                createNumericCell(row, colIndex++, product.getSkontoDiscount() != null ? product.getSkontoDiscount().doubleValue() : 0.0, numberStyle);
                
                // Sposób obliczania rabatu (kolumna 8)
                String methodValue = product.getDiscountCalculationMethod() != null 
                    ? product.getDiscountCalculationMethod().name() 
                    : "";
                createCell(row, colIndex++, methodValue, cellStyle);
                
                if (category == ProductCategory.ACCESSORY) {
                    // Typ (kolumna 9) - tylko dla akcesoriów
                    createCell(row, colIndex++, product.getAccessoryType(), cellStyle);
                } else {
                    // Typ produktu (kolumna 9) - dla dachówek i rynien
                    createCell(row, colIndex++, product.getProductType(), cellStyle);
                }
            }
            
            // Auto-size kolumny
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Zapisz do byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Utwórz komórkę tekstową
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Utwórz komórkę numeryczną
     */
    private void createNumericCell(Row row, int column, Double value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    /**
     * Usuń nieprawidłowe znaki z nazwy pliku
     * Windows nie pozwala na: < > : " / \ | ? *
     * ⚠️ WAŻNE: NIE zamieniaj myślników "-" - są one częścią formatu "Manufacturer-GroupName.xlsx"
     * ⚠️ WAŻNE: NIE zamieniaj spacji - mogą być w groupName (np. "czarna ang NUANE")
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed.xlsx";
        }
        
        // Zamień tylko nieprawidłowe znaki Windows na podkreślenia
        // NIE zamieniaj myślników "-" ani spacji " " - są one częścią formatu nazwy pliku
        String sanitized = fileName
            .replace("<", "_")
            .replace(">", "_")
            .replace(":", "_")
            .replace("\"", "_")
            .replace("/", "_")
            .replace("\\", "_")
            .replace("|", "_")
            .replace("?", "_")
            .replace("*", "_");
        
        // Usuń wielokrotne podkreślenia (ale nie myślniki ani spacje)
        while (sanitized.contains("__")) {
            sanitized = sanitized.replace("__", "_");
        }
        
        // Usuń podkreślenia na początku i końcu (ale nie myślniki ani spacje)
        sanitized = sanitized.trim();
        while (sanitized.startsWith("_")) {
            sanitized = sanitized.substring(1);
        }
        while (sanitized.endsWith("_")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        
        // Jeśli nazwa jest pusta, użyj domyślnej
        if (sanitized.isEmpty() || sanitized.equals(".xlsx")) {
            sanitized = "unnamed.xlsx";
        }
        
        return sanitized;
    }
}

