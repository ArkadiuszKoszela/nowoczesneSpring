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
}

