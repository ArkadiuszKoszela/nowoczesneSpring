package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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

    public ProductImportService(PriceCalculationService priceCalculationService) {
        this.priceCalculationService = priceCalculationService;
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

            // DOKŁADNIE TAK SAMO jak CsvImporterImplTile - używamy Poiji
            List<Product> productsFromFile = Poiji.fromExcel(file, Product.class);

            String manufacturer = getManufacturer(fileName);

            for (Product product : productsFromFile) {
                product.setManufacturer(manufacturer);
                product.setGroupName(customGroupName);
                product.setCategory(category);
                
                // ⭐ AUTOMATYCZNE MAPOWANIE NAZWY → mapperName
                // Jeśli Excel nie ma kolumny mapperName, generujemy z nazwy produktu
                if (product.getMapperName() == null || product.getMapperName().trim().isEmpty()) {
                    String mapperName = generateMapperNameFromProductName(product.getName());
                    product.setMapperName(mapperName);
                    System.out.println("🔹 Auto-mapowanie: '" + product.getName() + "' → mapperName: '" + mapperName + "'");
                }

                // DOKŁADNIE TA SAMA LOGIKA KALKULACJI co w CsvImporterImplTile
                if (product.getRetailPrice() != 0.00 && product.getPurchasePrice() != 0.00) {
                    continue;
                } else if (product.getPurchasePrice() != 0.00) {
                    double retailPrice = priceCalculationService.calculateRetailPrice(product);
                    product.setRetailPrice(retailPrice);
                } else if (product.getRetailPrice() != 0.00) {
                    double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                    product.setPurchasePrice(purchasePrice);
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
     * "CANTUS łupek ang-NUANE.xlsx" -> "CANTUS łupek ang"
     */
    private String getManufacturer(String fileName) {
        String nameWithoutExtension = StringUtils.substringBeforeLast(fileName, ".");
        return StringUtils.substringBeforeLast(nameWithoutExtension, "-").trim();
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
}

