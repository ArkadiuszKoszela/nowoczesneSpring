package pl.koszela.nowoczesnebud.DTO;

import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.util.List;

/**
 * DTO do sprawdzania istniejących kombinacji producent+grupa
 */
public class CheckExistingGroupsRequest {
    private ProductCategory category;
    private List<ManufacturerGroupPair> pairs;

    public CheckExistingGroupsRequest() {
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public List<ManufacturerGroupPair> getPairs() {
        return pairs;
    }

    public void setPairs(List<ManufacturerGroupPair> pairs) {
        this.pairs = pairs;
    }

    public static class ManufacturerGroupPair {
        private String manufacturer;
        private String groupName;
        private String productName; // Nazwa produktu w systemie (editableName)

        public ManufacturerGroupPair() {
        }

        public ManufacturerGroupPair(String manufacturer, String groupName, String productName) {
            this.manufacturer = manufacturer;
            this.groupName = groupName;
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

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }
    }
}

