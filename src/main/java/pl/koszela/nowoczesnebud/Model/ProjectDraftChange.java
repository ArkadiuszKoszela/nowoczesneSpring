package pl.koszela.nowoczesnebud.Model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_draft_changes_ws", indexes = {
    @Index(name = "idx_draft_change_ws_project_category", columnList = "project_id,category"),
    @Index(name = "idx_draft_change_ws_product_id", columnList = "product_id")
})
public class ProjectDraftChange {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "category", nullable = false)
    private String category; // TILE, GUTTER, ACCESSORY
    
    // Draft ceny (tymczasowe, niezapisane)
    @Column(name = "draft_retail_price")
    private Double draftRetailPrice;
    
    @Column(name = "draft_purchase_price")
    private Double draftPurchasePrice;
    
    @Column(name = "draft_selling_price")
    private Double draftSellingPrice;
    
    @Column(name = "draft_quantity")
    private Double draftQuantity;
    
    @Column(name = "draft_selected")
    private Boolean draftSelected; // Dla akcesoriów - czy produkt jest zaznaczony (checkbox)
    
    // Draft marża i rabat (tymczasowe)
    @Column(name = "draft_margin_percent")
    private Double draftMarginPercent;
    
    @Column(name = "draft_discount_percent")
    private Double draftDiscountPercent;
    
    @Column(name = "price_change_source")
    private String priceChangeSource; // AUTO, MARGIN, DISCOUNT, MANUAL
    
    // Opcja dla grupy produktowej (draft)
    // ⚠️ WAŻNE: manufacturer i groupName są pobierane z Product przez productId
    @Convert(converter = GroupOptionConverter.class)
    @Column(name = "draft_is_main_option", length = 20)
    private GroupOption draftIsMainOption; // MAIN, OPTIONAL, lub NONE (domyślnie)
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ProjectDraftChange() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
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
    
    public GroupOption getDraftIsMainOption() {
        return draftIsMainOption;
    }
    
    public void setDraftIsMainOption(GroupOption draftIsMainOption) {
        this.draftIsMainOption = draftIsMainOption;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

