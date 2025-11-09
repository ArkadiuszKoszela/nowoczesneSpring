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
 * Serwis do importu produktów z plików Excel
 * Używa biblioteki Poiji - DOKŁADNIE TA SAMA LOGIKA co CsvImporterImplTile
 */
@Service
public class ProductImportService {

    private final PriceCalculationService priceCalculationService;

    public ProductImportService(PriceCalculationService priceCalculationService) {
        this.priceCalculationService = priceCalculationService;
    }

    /**
     * Import produktów z listy plików Excel
     * Nazwa pliku koduje: "Manufacturer-GroupName.xlsx"
     * Np: "CANTUS łupek ang-NUANE.xlsx" -> manufacturer: "CANTUS łupek ang", groupName: "NUANE"
     */
    public List<Product> importProducts(List<MultipartFile> files, ProductCategory category) throws IOException {
        List<Product> allProducts = new ArrayList<>();
        Map<String, List<Product>> productsByManufacturer = new HashMap<>();

        for (MultipartFile multipartFile : files) {
            File file = convertMultiPartToFile(multipartFile);
            String fileName = multipartFile.getOriginalFilename();

            // Parse Excel używając Poiji (ta sama biblioteka co wcześniej)
            List<Product> productsFromFile = Poiji.fromExcel(file, Product.class);

            String manufacturer = getManufacturer(fileName);
            String groupName = getGroupName(fileName);

            // Ustaw metadane i wykonaj kalkulacje
            for (Product product : productsFromFile) {
                product.setManufacturer(manufacturer);
                product.setGroupName(groupName);
                product.setCategory(category);

                // Kalkulacja cen - DOKŁADNIE TA SAMA LOGIKA
                if (product.getRetailPrice() != 0.00 && product.getPurchasePrice() != 0.00) {
                    // Oba pola wypełnione - nie rób nic
                    continue;
                } else if (product.getPurchasePrice() != 0.00) {
                    // Mamy purchasePrice -> oblicz retailPrice
                    double retailPrice = priceCalculationService.calculateRetailPrice(product);
                    product.setRetailPrice(retailPrice);
                } else if (product.getRetailPrice() != 0.00) {
                    // Mamy retailPrice -> oblicz purchasePrice
                    double purchasePrice = priceCalculationService.calculatePurchasePrice(product);
                    product.setPurchasePrice(purchasePrice);
                }
            }

            allProducts.addAll(productsFromFile);

            // Grupuj po manufacturer
            productsByManufacturer
                .computeIfAbsent(manufacturer, k -> new ArrayList<>())
                .addAll(productsFromFile);

            // Usuń tymczasowy plik
            if (file.delete()) {
                System.out.println("Deleted temp file - " + file.getName());
            }
        }

        return allProducts;
    }

    /**
     * Import produktów z niestandardowymi nazwami grup
     * Zamiast parsować nazwę z pliku, użytkownik podaje nazwy ręcznie
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

            List<Product> productsFromFile = Poiji.fromExcel(file, Product.class);

            String manufacturer = getManufacturer(fileName);

            for (Product product : productsFromFile) {
                product.setManufacturer(manufacturer);
                product.setGroupName(customGroupName); // Użyj niestandardowej nazwy
                product.setCategory(category);

                // Kalkulacja cen
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
     * Import akcesoriów z katalogu (dla kompatybilności)
     */
    public List<Product> importAccessoriesFromDirectory(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Product> accessories = new ArrayList<>();

        if (files == null) {
            return accessories;
        }

        for (File file : files) {
            List<Product> productsFromFile = Poiji.fromExcel(file, Product.class);
            String manufacturer = getManufacturer(file.getName());

            for (Product product : productsFromFile) {
                product.setManufacturer(manufacturer);
                product.setCategory(ProductCategory.ACCESSORY);
            }

            accessories.addAll(productsFromFile);
        }

        return accessories;
    }

    /**
     * Wyciąga nazwę producenta z nazwy pliku
     * Np: "CANTUS łupek ang-NUANE.xlsx" -> "CANTUS łupek ang"
     */
    private String getManufacturer(String fileName) {
        String nameWithoutExtension = StringUtils.substringBeforeLast(fileName, ".");
        return StringUtils.substringBeforeLast(nameWithoutExtension, "-").trim();
    }

    /**
     * Wyciąga nazwę grupy z nazwy pliku
     * Np: "CANTUS łupek ang-NUANE.xlsx" -> "NUANE"
     */
    private String getGroupName(String fileName) {
        String nameWithoutExtension = StringUtils.substringBeforeLast(fileName, ".");
        return StringUtils.substringAfterLast(nameWithoutExtension, "-").trim();
    }

    /**
     * Konwersja MultipartFile na File (tymczasowy)
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

