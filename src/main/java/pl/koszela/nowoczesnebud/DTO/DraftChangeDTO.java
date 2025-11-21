package pl.koszela.nowoczesnebud.DTO;

public class DraftChangeDTO {
    
    private Long productId;
    private String category;
    private Double draftRetailPrice;
    private Double draftPurchasePrice;
    private Double draftSellingPrice;
    private Double draftQuantity;
    private Double draftMarginPercent;
    private Double draftDiscountPercent;
    private String priceChangeSource;
    
    // Constructors
    public DraftChangeDTO() {}
    
    public DraftChangeDTO(Long productId, String category) {
        this.productId = productId;
        this.category = category;
    }
    
    // Getters and Setters
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Double getDraftRetailPrice() {
        return draftRetailPrice;
    }
    
    public void setDraftRetailPrice(Double draftRetailPrice) {
        this.draftRetailPrice = draftRetailPrice;
    }
    
    public Double getDraftPurchasePrice() {
        return draftPurchasePrice;
    }
    
    public void setDraftPurchasePrice(Double draftPurchasePrice) {
        this.draftPurchasePrice = draftPurchasePrice;
    }
    
    public Double getDraftSellingPrice() {
        return draftSellingPrice;
    }
    
    public void setDraftSellingPrice(Double draftSellingPrice) {
        this.draftSellingPrice = draftSellingPrice;
    }
    
    public Double getDraftQuantity() {
        return draftQuantity;
    }
    
    public void setDraftQuantity(Double draftQuantity) {
        this.draftQuantity = draftQuantity;
    }
    
    public Double getDraftMarginPercent() {
        return draftMarginPercent;
    }
    
    public void setDraftMarginPercent(Double draftMarginPercent) {
        this.draftMarginPercent = draftMarginPercent;
    }
    
    public Double getDraftDiscountPercent() {
        return draftDiscountPercent;
    }
    
    public void setDraftDiscountPercent(Double draftDiscountPercent) {
        this.draftDiscountPercent = draftDiscountPercent;
    }
    
    public String getPriceChangeSource() {
        return priceChangeSource;
    }
    
    public void setPriceChangeSource(String priceChangeSource) {
        this.priceChangeSource = priceChangeSource;
    }
}


