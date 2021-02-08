package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class CsvImporterImplTile implements CsvImporter<Tile> {

    private final ProductTypeService productTypeService;

    public CsvImporterImplTile(ProductTypeService productTypeService) {
        this.productTypeService = productTypeService;
    }

    @Override
    public List<Tile> readAndSaveTiles(List<MultipartFile> list) throws IOException {
        List<Tile> tilePathList = new ArrayList<>();

        Tile tile = new Tile();
        List<MultipartFile> li = new ArrayList<>(list);
        Iterator<MultipartFile> i = li.iterator();

        while (i.hasNext()) {
            MultipartFile multipartFile = i.next();
            File file = convertMultiPartToFile(multipartFile);
            String fileName = multipartFile.getOriginalFilename();
            List<ProductType> productTypeList = Poiji.fromExcel(file, ProductType.class);
            tile.setManufacturer(getManufacturer(fileName));

            for (ProductType productType : productTypeList) {
                if (productType.getDetalPrice() != 0.00d && productType.getPurchasePrice() != 0.00d) {
                    continue;
                } else if (productType.getPurchasePrice() != 0.00d) {
                    productType.setDetalPrice(productTypeService.calculateDetalPrice(productType));
                } else if (productType.getDetalPrice() != 0.00d) {
                    double purchasePrice = productTypeService.calculatePurchasePrice(productType.getDetalPrice(),
                            productType);
                    productType.setPurchasePrice(purchasePrice);
                }
            }

            ProductGroup productGroup = new ProductGroup(getTypeOfTile(fileName), productTypeList);
            tile.getProductGroupList().add(productGroup);

            i.remove();
            boolean hasAnyMore = li.stream().anyMatch(
                    e -> getManufacturer(e.getOriginalFilename()).equalsIgnoreCase(getManufacturer(fileName)));
            if (file.delete()) ;
                System.out.println("Delete file - " + file.getName());
            if (!hasAnyMore) {
                tilePathList.add(tile);
                tile = new Tile();
            }
        }
        return tilePathList;
    }

    public List<Gutter> readAndSaveGutters(List<MultipartFile> list) throws IOException {
        List<Gutter> gutterPathList = new ArrayList<>();

        Gutter gutter = new Gutter();
        List<MultipartFile> li = new ArrayList<>(list);
        Iterator<MultipartFile> i = li.iterator();

        while (i.hasNext()) {
            MultipartFile multipartFile = i.next();
            File file = convertMultiPartToFile(multipartFile);
            String fileName = multipartFile.getOriginalFilename();
            List<ProductType> productTypeList = Poiji.fromExcel(file, ProductType.class);

            for (ProductType productType : productTypeList) {
                if (productType.getDetalPrice() != 0.00d && productType.getPurchasePrice() != 0.00d) {
                    continue;
                } else if (productType.getPurchasePrice() != 0.00d) {
                    productType.setDetalPrice(productTypeService.calculateDetalPrice(productType));
                } else if (productType.getDetalPrice() != 0.00d) {
                    double purchasePrice = productTypeService.calculatePurchasePrice(productType.getDetalPrice(),
                            productType);
                    productType.setPurchasePrice(purchasePrice);
                }
            }

            gutter.setManufacturer(getManufacturer(fileName));
            ProductGroup productGroup = new ProductGroup(getTypeOfTile(fileName), productTypeList);
            gutter.getProductGroupList().add(productGroup);

            i.remove();
            boolean hasAnyMore = li.stream().anyMatch(
                    e -> getManufacturer(e.getOriginalFilename()).equalsIgnoreCase(getManufacturer(fileName)));
            if (file.delete()) ;
            System.out.println("Delete file - " + file.getName());
            if (!hasAnyMore) {
                gutterPathList.add(gutter);
                gutter = new Gutter();
            }
        }
        return gutterPathList;
    }

    List<Accessory> readAndSaveAccessories(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Accessory> accessoryPathList = new ArrayList<>();
        for (File file : files) {
            accessoryPathList = Poiji.fromExcel(file, Accessory.class);
            accessoryPathList.forEach(obj -> obj.setManufacturer(getManufacturer(file.getName())));
        }
        return accessoryPathList;
    }

    private String getManufacturer(String url) {
        String fullName = StringUtils.substringBeforeLast(url, ".");
        return StringUtils.substringBeforeLast(fullName, "-").trim();
    }

    private String getTypeOfTile(String url) {
        String fullName = StringUtils.substringBeforeLast(url, ".");
        return StringUtils.substringAfterLast(fullName, "-").trim();
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("tmp", ".xlsx");
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        System.out.println("Create file - " + convFile.getName());
        return convFile;
    }
}
