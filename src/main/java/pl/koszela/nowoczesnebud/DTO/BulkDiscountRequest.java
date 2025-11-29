package pl.koszela.nowoczesnebud.DTO;

import pl.koszela.nowoczesnebud.Model.DiscountCalculationMethod;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Request dla bulk update rabatów
 */
public class BulkDiscountRequest {
    
    @NotNull(message = "Kategoria jest wymagana")
    private ProductCategory category;
    
    @NotNull(message = "Producent jest wymagany")
    private String manufacturer;
    
    // groupName może być null - wtedy rabaty są stosowane dla całego producenta
    private String groupName;
    
    @Min(value = 0, message = "Rabat nie może być ujemny")
    @Max(value = 100, message = "Rabat nie może przekraczać 100%")
    private Integer basicDiscount;
    
    @Min(value = 0, message = "Rabat nie może być ujemny")
    @Max(value = 100, message = "Rabat nie może przekraczać 100%")
    private Integer additionalDiscount;
    
    @Min(value = 0, message = "Rabat nie może być ujemny")
    @Max(value = 100, message = "Rabat nie może przekraczać 100%")
    private Integer promotionDiscount;
    
    @Min(value = 0, message = "Rabat nie może być ujemny")
    @Max(value = 100, message = "Rabat nie może przekraczać 100%")
    private Integer skontoDiscount;
    
    // Typ produktu (opcjonalny - jeśli null lub "ALL", rabaty są stosowane do wszystkich produktów)
    private String productType;
    
    // Metoda obliczania rabatu (wymagana)
    @NotNull(message = "Metoda obliczania rabatu jest wymagana")
    private DiscountCalculationMethod discountCalculationMethod;

    // Getters & Setters
    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
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

    public Integer getBasicDiscount() {
        return basicDiscount;
    }

    public void setBasicDiscount(Integer basicDiscount) {
        this.basicDiscount = basicDiscount;
    }

    public Integer getAdditionalDiscount() {
        return additionalDiscount;
    }

    public void setAdditionalDiscount(Integer additionalDiscount) {
        this.additionalDiscount = additionalDiscount;
    }

    public Integer getPromotionDiscount() {
        return promotionDiscount;
    }

    public void setPromotionDiscount(Integer promotionDiscount) {
        this.promotionDiscount = promotionDiscount;
    }

    public Integer getSkontoDiscount() {
        return skontoDiscount;
    }

    public void setSkontoDiscount(Integer skontoDiscount) {
        this.skontoDiscount = skontoDiscount;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public DiscountCalculationMethod getDiscountCalculationMethod() {
        return discountCalculationMethod;
    }

    public void setDiscountCalculationMethod(DiscountCalculationMethod discountCalculationMethod) {
        this.discountCalculationMethod = discountCalculationMethod;
    }
}












