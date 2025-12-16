package pl.koszela.nowoczesnebud.Service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🚀 TESTY WYDAJNOŚCIOWE DLA IMPORTU CENNIKÓW
 * 
 * Testuje import produktów z plików Excel:
 * - 600 cenników (plików Excel)
 * - W każdym cenniku 15-16 rekordów produktów
 * - Łącznie ~9000-9600 produktów
 * 
 * Sprawdza:
 * - Czas importu dużej liczby plików
 * - Wydajność zapisu produktów do bazy
 * - Poprawność importowanych danych
 */
@SpringBootTest
@ActiveProfiles("test-mysql")
@Transactional
public class ProductServiceImportPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImportPerformanceTest.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // Wyczyść produkty przed testem (opcjonalnie - można zostawić dla testów kumulatywnych)
        // productRepository.deleteAll();
    }

    /**
     * 🚀 TEST WYDAJNOŚCIOWY: Import 600 cenników × 15-16 rekordów = ~9000-9600 produktów
     * 
     * Scenariusz realistyczny:
     * - 600 plików Excel (cenników)
     * - W każdym pliku 15-16 produktów
     * - Różni producenci i grupy produktowe
     * - Różne kategorie (TILE, GUTTER, ACCESSORY)
     */
    @Test
    void testImportProducts_Performance_600PriceLists_9000Products_RealScenario() throws IOException {
        long testStartTime = System.currentTimeMillis();
        logger.info("🚀 TEST WYDAJNOŚCIOWY: Import 600 cenników × 15-16 rekordów = ~9000-9600 produktów");
        
        // Parametry testu
        int numberOfPriceLists = 600;
        int productsPerPriceListMin = 15;
        int productsPerPriceListMax = 16;
        
        // 1. Przygotuj pliki Excel (mock)
        long createFilesStartTime = System.currentTimeMillis();
        logger.info("📦 TEST: Tworzenie {} plików Excel (mock)...", numberOfPriceLists);
        
        List<MultipartFile> files = new ArrayList<>();
        List<String> customGroupNames = new ArrayList<>();
        List<String> manufacturers = new ArrayList<>();
        List<String> groupNames = new ArrayList<>();
        
        // Różne kategorie produktów (realistyczny podział)
        ProductCategory[] categories = {ProductCategory.TILE, ProductCategory.GUTTER, ProductCategory.ACCESSORY};
        String[] manufacturerNames = {"CANTUS", "BRAAS", "CREATON", "RÖBEN", "KORAMIC", "TONDACH", "GERARD"};
        
        for (int i = 0; i < numberOfPriceLists; i++) {
            // Losowa kategoria i producent
            ProductCategory category = categories[i % categories.length];
            String manufacturer = manufacturerNames[i % manufacturerNames.length];
            
            // Losowa liczba produktów w cenniku (15-16) - realistyczny zakres
            int productsInPriceList = (i % 2 == 0) ? productsPerPriceListMin : productsPerPriceListMax;
            
            // Utwórz plik Excel z produktami
            MultipartFile file = createMockExcelFile(
                "CENNIK_" + manufacturer + "_" + i + ".xlsx",
                category,
                productsInPriceList,
                i
            );
            
            files.add(file);
            customGroupNames.add("Grupa produktowa " + i);
            manufacturers.add(manufacturer);
            groupNames.add("GRUPA_" + i);
        }
        
        long createFilesEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] TEST - Utworzenie {} plików Excel: {}ms ({}s)", 
                    numberOfPriceLists, 
                    createFilesEndTime - createFilesStartTime,
                    (createFilesEndTime - createFilesStartTime) / 1000.0);
        
        // 2. Import produktów (dla każdej kategorii osobno)
        long importStartTime = System.currentTimeMillis();
        int totalImportedProducts = 0;
        
        for (ProductCategory category : categories) {
            // Filtruj pliki dla danej kategorii
            List<MultipartFile> categoryFiles = new ArrayList<>();
            List<String> categoryCustomGroupNames = new ArrayList<>();
            List<String> categoryManufacturers = new ArrayList<>();
            List<String> categoryGroupNames = new ArrayList<>();
            
            for (int i = 0; i < files.size(); i++) {
                if (categories[i % categories.length] == category) {
                    categoryFiles.add(files.get(i));
                    categoryCustomGroupNames.add(customGroupNames.get(i));
                    categoryManufacturers.add(manufacturers.get(i));
                    categoryGroupNames.add(groupNames.get(i));
                }
            }
            
            if (!categoryFiles.isEmpty()) {
                logger.info("📥 TEST: Import {} plików dla kategorii {}...", categoryFiles.size(), category);
                
                long categoryImportStartTime = System.currentTimeMillis();
                List<Product> importedProducts = productService.importProductsWithCustomNames(
                    categoryFiles,
                    categoryCustomGroupNames,
                    categoryManufacturers,
                    categoryGroupNames,
                    category
                );
                long categoryImportEndTime = System.currentTimeMillis();
                
                totalImportedProducts += importedProducts.size();
                logger.info("⏱️ [PERFORMANCE] TEST - Import kategorii {}: {} produktów w {}ms ({}s)", 
                            category,
                            importedProducts.size(),
                            categoryImportEndTime - categoryImportStartTime,
                            (categoryImportEndTime - categoryImportStartTime) / 1000.0);
            }
        }
        
        long importEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] TEST - Import wszystkich produktów: {} produktów w {}ms ({}s)", 
                    totalImportedProducts,
                    importEndTime - importStartTime,
                    (importEndTime - importStartTime) / 1000.0);
        
        // 3. Weryfikacja
        long verifyStartTime = System.currentTimeMillis();
        logger.info("✅ TEST: Weryfikacja zaimportowanych produktów...");
        
        // Sprawdź liczbę zaimportowanych produktów
        long totalProductsInDb = productRepository.count();
        logger.info("📊 TEST: Łączna liczba produktów w bazie: {}", totalProductsInDb);
        
        // Sprawdź produkty dla każdej kategorii
        for (ProductCategory category : categories) {
            long categoryCount = productRepository.findByCategory(category).size();
            logger.info("📊 TEST: Kategoria {} - {} produktów", category, categoryCount);
        }
        
        long verifyEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] TEST - Weryfikacja: {}ms", verifyEndTime - verifyStartTime);
        
        // 4. Podsumowanie
        long testEndTime = System.currentTimeMillis();
        long totalTime = testEndTime - testStartTime;
        
        logger.info("⏱️ [PERFORMANCE] TEST - CAŁKOWITY CZAS: {}ms ({}s) | " +
                    "createFiles: {}ms | import: {}ms | verify: {}ms",
                    totalTime,
                    totalTime / 1000.0,
                    createFilesEndTime - createFilesStartTime,
                    importEndTime - importStartTime,
                    verifyEndTime - verifyStartTime);
        
        // Asercje
        assertTrue(totalImportedProducts > 0, "Powinno zaimportować przynajmniej kilka produktów");
        assertTrue(totalProductsInDb >= totalImportedProducts, 
                   "Liczba produktów w bazie powinna być >= liczby zaimportowanych");
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Zaimportowano {} produktów z {} cenników", 
                    totalImportedProducts, numberOfPriceLists);
    }

    /**
     * 🚀 TEST WYDAJNOŚCIOWY: Import pojedynczego dużego cennika (1000 produktów)
     * 
     * Testuje wydajność importu jednego dużego pliku Excel z wieloma produktami
     */
    @Test
    void testImportProducts_Performance_SingleLargePriceList_1000Products() throws IOException {
        long testStartTime = System.currentTimeMillis();
        logger.info("🚀 TEST WYDAJNOŚCIOWY: Import pojedynczego dużego cennika (1000 produktów)");
        
        // Parametry testu
        int numberOfProducts = 1000;
        ProductCategory category = ProductCategory.TILE;
        String manufacturer = "CANTUS";
        String groupName = "DUŻA_GRUPA";
        
        // 1. Przygotuj plik Excel
        long createFileStartTime = System.currentTimeMillis();
        logger.info("📦 TEST: Tworzenie pliku Excel z {} produktami...", numberOfProducts);
        
        MultipartFile file = createMockExcelFile(
            "CENNIK_" + manufacturer + "_LARGE.xlsx",
            category,
            numberOfProducts,
            0
        );
        
        long createFileEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] TEST - Utworzenie pliku Excel: {}ms", 
                    createFileEndTime - createFileStartTime);
        
        // 2. Import produktów
        long importStartTime = System.currentTimeMillis();
        logger.info("📥 TEST: Import produktów...");
        
        List<MultipartFile> files = new ArrayList<>();
        files.add(file);
        
        List<String> customGroupNames = new ArrayList<>();
        customGroupNames.add("Duża grupa produktowa");
        
        List<String> manufacturers = new ArrayList<>();
        manufacturers.add(manufacturer);
        
        List<String> groupNames = new ArrayList<>();
        groupNames.add(groupName);
        
        List<Product> importedProducts = productService.importProductsWithCustomNames(
            files,
            customGroupNames,
            manufacturers,
            groupNames,
            category
        );
        
        long importEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] TEST - Import produktów: {} produktów w {}ms ({}s)", 
                    importedProducts.size(),
                    importEndTime - importStartTime,
                    (importEndTime - importStartTime) / 1000.0);
        
        // 3. Weryfikacja
        long verifyStartTime = System.currentTimeMillis();
        logger.info("✅ TEST: Weryfikacja zaimportowanych produktów...");
        
        assertEquals(numberOfProducts, importedProducts.size(), 
                     "Powinno zaimportować dokładnie " + numberOfProducts + " produktów");
        
        // Sprawdź kilka produktów
        for (int i = 0; i < Math.min(5, importedProducts.size()); i++) {
            Product product = importedProducts.get(i);
            assertNotNull(product.getId(), "Produkt powinien mieć ID");
            assertEquals(category, product.getCategory(), "Kategoria powinna być poprawna");
            assertEquals(manufacturer, product.getManufacturer(), "Producent powinien być poprawny");
            assertNotNull(product.getName(), "Nazwa produktu nie powinna być null");
        }
        
        long verifyEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] TEST - Weryfikacja: {}ms", verifyEndTime - verifyStartTime);
        
        // 4. Podsumowanie
        long testEndTime = System.currentTimeMillis();
        long totalTime = testEndTime - testStartTime;
        
        logger.info("⏱️ [PERFORMANCE] TEST - CAŁKOWITY CZAS: {}ms ({}s) | " +
                    "createFile: {}ms | import: {}ms | verify: {}ms",
                    totalTime,
                    totalTime / 1000.0,
                    createFileEndTime - createFileStartTime,
                    importEndTime - importStartTime,
                    verifyEndTime - verifyStartTime);
        
        logger.info("✅ TEST ZAKOŃCZONY POMYŚLNIE: Zaimportowano {} produktów z 1 cennika", 
                    importedProducts.size());
    }

    /**
     * Pomocnicza metoda do tworzenia mock pliku Excel z produktami
     * 
     * @param fileName Nazwa pliku
     * @param category Kategoria produktów
     * @param numberOfProducts Liczba produktów w pliku
     * @param priceListIndex Indeks cennika (dla unikalności nazw)
     * @return MultipartFile z danymi Excel
     */
    private MultipartFile createMockExcelFile(String fileName, ProductCategory category, 
                                             int numberOfProducts, int priceListIndex) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produkty");
            
            // Nagłówek (zgodny z mapowaniem w ProductImportService)
            Row headerRow = sheet.createRow(0);
            createCell(headerRow, 0, "name");
            createCell(headerRow, 1, "unitDetalP"); // retailPrice
            createCell(headerRow, 2, "cena zakupu"); // purchasePrice
            createCell(headerRow, 3, "unit");
            createCell(headerRow, 4, "quantityCo"); // quantityConverter
            createCell(headerRow, 5, "mapperName");
            createCell(headerRow, 6, "rabat"); // discount
            createCell(headerRow, 7, "basicDisc"); // basicDiscount
            createCell(headerRow, 8, "promotion"); // promotionDiscount
            
            // Wiersze z produktami
            for (int i = 0; i < numberOfProducts; i++) {
                Row row = sheet.createRow(i + 1);
                int productIndex = priceListIndex * 100 + i; // Unikalny indeks produktu
                
                createCell(row, 0, "Produkt " + productIndex);
                createCell(row, 1, 100.0 + productIndex); // retailPrice
                createCell(row, 2, 80.0 + productIndex); // purchasePrice
                createCell(row, 3, "szt");
                createCell(row, 4, 1.0); // quantityConverter
                createCell(row, 5, "Mapper_" + productIndex);
                createCell(row, 6, 10.0); // discount
                createCell(row, 7, 5); // basicDiscount
                createCell(row, 8, 3); // promotionDiscount
            }
            
            // Konwersja do byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] bytes = outputStream.toByteArray();
            
            return new MockMultipartFile(
                "file",
                fileName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
            );
        }
    }

    /**
     * Pomocnicza metoda do tworzenia komórki w Excelu
     */
    private void createCell(Row row, int columnIndex, Object value) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        }
    }
}

