package pl.koszela.nowoczesnebud.DTO;

public class DraftChangeDTO {
    
    private Long productId;
    private String category;
    private Double draftRetailPrice;
    private Double draftPurchasePrice;
    private Double draftSellingPrice;
    private Double draftQuantity;
    private Boolean draftSelected; // Dla akcesoriów - czy produkt jest zaznaczony (checkbox)
    private Double draftMarginPercent;
    private Double draftDiscountPercent;
    private String priceChangeSource;
    
    // Opcja dla grupy produktowej (draft)
    // ⚠️ WAŻNE: manufacturer i groupName są pobierane z Product przez productId
    private Boolean draftIsMainOption; // Czy grupa jest "Główna" (true) czy "Opcjonalna" (false) lub null
    
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
    
    public Boolean getDraftSelected() {
        return draftSelected;
    }
    
    public void setDraftSelected(Boolean draftSelected) {
        this.draftSelected = draftSelected;
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
    
    public Boolean getDraftIsMainOption() {
        return draftIsMainOption;
    }
    
    public void setDraftIsMainOption(Boolean draftIsMainOption) {
        this.draftIsMainOption = draftIsMainOption;
    }
}


