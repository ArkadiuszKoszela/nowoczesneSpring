package pl.koszela.nowoczesnebud.DTO;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Request DTO dla aktualizacji rabatów produktu
 */
public class DiscountUpdateRequest {
    
    @Min(value = 0, message = "Rabat podstawowy nie może być mniejszy niż 0")
    @Max(value = 100, message = "Rabat podstawowy nie może być większy niż 100")
    private Integer basicDiscount;
    
    @Min(value = 0, message = "Rabat promocyjny nie może być mniejszy niż 0")
    @Max(value = 100, message = "Rabat promocyjny nie może być większy niż 100")
    private Integer promotionDiscount;
    
    @Min(value = 0, message = "Rabat dodatkowy nie może być mniejszy niż 0")
    @Max(value = 100, message = "Rabat dodatkowy nie może być większy niż 100")
    private Integer additionalDiscount;
    
    @Min(value = 0, message = "Rabat skonto nie może być mniejszy niż 0")
    @Max(value = 100, message = "Rabat skonto nie może być większy niż 100")
    private Integer skontoDiscount;

    // Constructors
    public DiscountUpdateRequest() {}

    public DiscountUpdateRequest(Integer basicDiscount, Integer promotionDiscount, 
                                Integer additionalDiscount, Integer skontoDiscount) {
        this.basicDiscount = basicDiscount;
        this.promotionDiscount = promotionDiscount;
        this.additionalDiscount = additionalDiscount;
        this.skontoDiscount = skontoDiscount;
    }

    // Getters and Setters
    public Integer getBasicDiscount() { return basicDiscount; }
    public void setBasicDiscount(Integer basicDiscount) { this.basicDiscount = basicDiscount; }

    public Integer getPromotionDiscount() { return promotionDiscount; }
    public void setPromotionDiscount(Integer promotionDiscount) { this.promotionDiscount = promotionDiscount; }

    public Integer getAdditionalDiscount() { return additionalDiscount; }
    public void setAdditionalDiscount(Integer additionalDiscount) { this.additionalDiscount = additionalDiscount; }

    public Integer getSkontoDiscount() { return skontoDiscount; }
    public void setSkontoDiscount(Integer skontoDiscount) { this.skontoDiscount = skontoDiscount; }
}















