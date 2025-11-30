package pl.koszela.nowoczesnebud.DTO;

import pl.koszela.nowoczesnebud.Model.GroupOption;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request DTO dla ustawienia opcji grupy produktów (Główna/Opcjonalna/Brak)
 */
public class GroupOptionRequest {
    
    @NotNull(message = "Kategoria jest wymagana")
    private ProductCategory category;
    
    @NotBlank(message = "Producent jest wymagany")
    private String manufacturer;
    
    @NotBlank(message = "Nazwa grupy jest wymagana")
    private String groupName;
    
    // MAIN = Główna, OPTIONAL = Opcjonalna, NONE = Nie wybrano
    private GroupOption isMainOption;

    // Constructors
    public GroupOptionRequest() {}

    public GroupOptionRequest(ProductCategory category, String manufacturer, 
                            String groupName, GroupOption isMainOption) {
        this.category = category;
        this.manufacturer = manufacturer;
        this.groupName = groupName;
        this.isMainOption = isMainOption;
    }

    // Getters and Setters
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public GroupOption getIsMainOption() { return isMainOption; }
    public void setIsMainOption(GroupOption isMainOption) { this.isMainOption = isMainOption; }

    @Override
    public String toString() {
        return "GroupOptionRequest{" +
                "category=" + category +
                ", manufacturer='" + manufacturer + '\'' +
                ", groupName='" + groupName + '\'' +
                ", isMainOption=" + isMainOption +
                '}';
    }
}






























