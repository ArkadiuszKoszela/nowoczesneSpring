package pl.koszela.nowoczesnebud.DTO;

import pl.koszela.nowoczesnebud.Model.Product;

import java.util.List;

/**
 * DTO z informacjami o pojedynczym przetworzonym pliku podczas importu
 */
public class ImportFileResult {
    private String fileName;
    private String productName;
    private String manufacturer;
    private String groupName;
    private int productsCount;
    private boolean success;
    private String errorMessage;
    private List<Product> products;

    public ImportFileResult() {
    }

    public ImportFileResult(String fileName, String productName, String manufacturer, String groupName, 
                           int productsCount, boolean success) {
        this.fileName = fileName;
        this.productName = productName;
        this.manufacturer = manufacturer;
        this.groupName = groupName;
        this.productsCount = productsCount;
        this.success = success;
    }

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getProductsCount() {
        return productsCount;
    }

    public void setProductsCount(int productsCount) {
        this.productsCount = productsCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}

