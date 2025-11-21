package pl.koszela.nowoczesnebud.DTO;

import java.util.List;

public class SaveDraftChangesRequest {
    
    private String category;
    private List<DraftChangeDTO> changes;
    
    // Globalne marża/rabat dla całej kategorii
    private Double categoryMargin;
    private Double categoryDiscount;
    
    // Constructors
    public SaveDraftChangesRequest() {}
    
    public SaveDraftChangesRequest(String category, List<DraftChangeDTO> changes) {
        this.category = category;
        this.changes = changes;
    }
    
    // Getters and Setters
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<DraftChangeDTO> getChanges() {
        return changes;
    }
    
    public void setChanges(List<DraftChangeDTO> changes) {
        this.changes = changes;
    }
    
    public Double getCategoryMargin() {
        return categoryMargin;
    }
    
    public void setCategoryMargin(Double categoryMargin) {
        this.categoryMargin = categoryMargin;
    }
    
    public Double getCategoryDiscount() {
        return categoryDiscount;
    }
    
    public void setCategoryDiscount(Double categoryDiscount) {
        this.categoryDiscount = categoryDiscount;
    }
}


