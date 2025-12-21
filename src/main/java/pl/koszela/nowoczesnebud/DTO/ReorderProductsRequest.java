package pl.koszela.nowoczesnebud.DTO;

import pl.koszela.nowoczesnebud.Model.ProductCategory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO dla zmiany kolejności produktów w grupie
 * Używany w endpoint /api/products/reorder
 */
public class ReorderProductsRequest {
    
    @NotEmpty(message = "Lista produktów nie może być pusta")
    private List<Long> productIds;
    
    @NotNull(message = "Kategoria jest wymagana")
    private ProductCategory category;
    
    @NotBlank(message = "Producent jest wymagany")
    private String manufacturer;
    
    @NotBlank(message = "Nazwa grupy jest wymagana")
    private String groupName;

    // Constructors
    public ReorderProductsRequest() {}

    public ReorderProductsRequest(List<Long> productIds, ProductCategory category, 
                                 String manufacturer, String groupName) {
        this.productIds = productIds;
        this.category = category;
        this.manufacturer = manufacturer;
        this.groupName = groupName;
    }

    // Getters and Setters
    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    @Override
    public String toString() {
        return "ReorderProductsRequest{" +
                "productIds=" + productIds +
                ", category=" + category +
                ", manufacturer='" + manufacturer + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }
}


