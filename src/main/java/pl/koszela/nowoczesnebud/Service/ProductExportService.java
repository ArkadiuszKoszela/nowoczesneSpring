package pl.koszela.nowoczesnebud.Service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
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

        // Grupuj produkty po producencie i grupie
        Map<String, List<Product>> groupedProducts = groupProductsByManufacturerAndGroup(products);

        // Utwórz ZIP w pamięci
        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
        
        try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            // Dla każdej grupy utwórz plik Excel
            for (Map.Entry<String, List<Product>> entry : groupedProducts.entrySet()) {
                String fileName = entry.getKey() + ".xlsx";
                List<Product> groupProducts = entry.getValue();
                
                // Utwórz plik Excel dla tej grupy (z kategorią)
                byte[] excelFile = createExcelFile(groupProducts, category);
                
                // Dodaj do ZIP
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipEntry.setSize(excelFile.length);
                zipOut.putNextEntry(zipEntry);
                zipOut.write(excelFile);
                zipOut.closeEntry();
            }
        }

        byte[] zipBytes = zipOutputStream.toByteArray();
        return zipBytes;
    }

    /**
     * Grupuj produkty po producencie i grupie
     * Klucz: "Manufacturer-GroupName"
     */
    private Map<String, List<Product>> groupProductsByManufacturerAndGroup(List<Product> products) {
        return products.stream()
            .filter(p -> p.getManufacturer() != null && p.getGroupName() != null)
            .collect(Collectors.groupingBy(product -> {
                String manufacturer = product.getManufacturer().trim();
                String groupName = product.getGroupName().trim();
                // Format: "Manufacturer-GroupName" (tak jak w imporcie)
                return manufacturer + "-" + groupName;
            }));
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
            
            // Styl dla liczb
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(cellStyle);
            DataFormat numberFormat = workbook.createDataFormat();
            numberStyle.setDataFormat(numberFormat.getFormat("#,##0.00"));
            
            // Utwórz nagłówki - różne dla różnych kategorii
            Row headerRow = sheet.createRow(0);
            String[] headers;
            
            if (category == ProductCategory.ACCESSORY) {
                // AKCESORIA: name, unitDetalPrice, unit, basicDiscount, additionalDiscount, promotionDiscount, skonto, discountCalculationMethod, type
                headers = new String[]{
                    "Nazwa",                           // name
                    "Cena katalogowa",                 // unitDetalPrice
                    "Jednostka",                       // unit
                    "Rabat podstawowy",                // basicDiscount
                    "Rabat dodatkowy",                 // additionalDiscount
                    "Rabat promocyjny",                // promotionDiscount
                    "Skonto",                          // skonto
                    "Sposób obliczania rabatu",        // discountCalculationMethod
                    "Typ"                              // type
                };
            } else {
                // DACHÓWKI I RYNNY: name, unitDetalP, quantityCo, basicDisc, additional, promotion, skonto, discountCalculationMethod, productType
                headers = new String[]{
                    "Nazwa",                           // name
                    "Cena katalogowa",                 // unitDetalP
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
                
                // name (kolumna 0)
                createCell(row, colIndex++, product.getName(), cellStyle);
                
                // Cena katalogowa (kolumna 1)
                createNumericCell(row, colIndex++, product.getRetailPrice(), numberStyle);
                
                if (category == ProductCategory.ACCESSORY) {
                    // AKCESORIA: unit (kolumna 2)
                    createCell(row, colIndex++, product.getUnit(), cellStyle);
                } else {
                    // DACHÓWKI I RYNNY: quantityCo (kolumna 2)
                    createNumericCell(row, colIndex++, product.getQuantityConverter(), numberStyle);
                }
                
                // Rabaty (kolumny 3-5)
                createNumericCell(row, colIndex++, product.getBasicDiscount() != null ? product.getBasicDiscount().doubleValue() : 0.0, numberStyle);
                createNumericCell(row, colIndex++, product.getAdditionalDiscount() != null ? product.getAdditionalDiscount().doubleValue() : 0.0, numberStyle);
                createNumericCell(row, colIndex++, product.getPromotionDiscount() != null ? product.getPromotionDiscount().doubleValue() : 0.0, numberStyle);
                
                // skonto (kolumna 6)
                createNumericCell(row, colIndex++, product.getSkontoDiscount() != null ? product.getSkontoDiscount().doubleValue() : 0.0, numberStyle);
                
                // Sposób obliczania rabatu (kolumna 7)
                String methodValue = product.getDiscountCalculationMethod() != null 
                    ? product.getDiscountCalculationMethod().name() 
                    : "";
                createCell(row, colIndex++, methodValue, cellStyle);
                
                if (category == ProductCategory.ACCESSORY) {
                    // Typ (kolumna 8) - tylko dla akcesoriów
                    createCell(row, colIndex++, product.getAccessoryType(), cellStyle);
                } else {
                    // Typ produktu (kolumna 8) - dla dachówek i rynien
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
}

