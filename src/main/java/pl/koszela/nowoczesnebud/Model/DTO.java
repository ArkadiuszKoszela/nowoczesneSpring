package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

@Data
public class DTO {
    private ProductType productType;
    private ProductGroup productGroup;

    public DTO(ProductType productType, ProductGroup productGroup) {
        this.productType = productType;
        this.productGroup = productGroup;
    }

    public DTO() {
    }
}
