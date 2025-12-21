package pl.koszela.nowoczesnebud.DTO;

import pl.koszela.nowoczesnebud.Model.GroupOption;
import java.util.List;

/**
 * Request do batch update opcji grupy (draftIsMainOption) dla wielu produktów naraz
 * Używane gdy użytkownik zmienia wariant oferty dla całej grupy produktów
 */
public class UpdateGroupOptionBatchRequest {
    
    private String category;
    private List<Long> productIds;
    private GroupOption draftIsMainOption;
    
    public UpdateGroupOptionBatchRequest() {}
    
    public UpdateGroupOptionBatchRequest(String category, List<Long> productIds, GroupOption draftIsMainOption) {
        this.category = category;
        this.productIds = productIds;
        this.draftIsMainOption = draftIsMainOption;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<Long> getProductIds() {
        return productIds;
    }
    
    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }
    
    public GroupOption getDraftIsMainOption() {
        return draftIsMainOption;
    }
    
    public void setDraftIsMainOption(GroupOption draftIsMainOption) {
        this.draftIsMainOption = draftIsMainOption;
    }
}




