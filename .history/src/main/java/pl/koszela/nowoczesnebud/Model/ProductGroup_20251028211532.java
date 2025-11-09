package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Grupa produktów
 * Używana przez TilesService, GuttersService (stary system)
 */
@Entity
public class ProductGroup {

    public ProductGroup(String typeName, List<ProductType> productTypeList) {
        this.typeName = typeName;
        this.totalPriceAfterDiscount = 0.0d;
        this.totalPriceDetal = 0.0d;
        this.totalProfit = 0.0d;
        this.productTypeList = productTypeList;
    }

    public ProductGroup() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String typeName;
    
    @Column(name = "option_name")
    private Boolean option;
    
    private Double totalPriceAfterDiscount = 0.0;
    private Double totalPriceDetal = 0.0;
    private Double totalProfit = 0.0;
    private Double totalSellingPrice = 0.0;

    @OneToMany(targetEntity = ProductType.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "product_group_id")
    @JsonIgnore
    private List<ProductType> productTypeList = new ArrayList<>();

    public void setId(long id) {
        this.id = id;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setOption(Boolean option) {
        this.option = option;
    }

    public void setTotalPriceAfterDiscount(Double totalPriceAfterDiscount) {
        this.totalPriceAfterDiscount = totalPriceAfterDiscount;
    }

    public void setTotalPriceDetal(Double totalPriceDetal) {
        this.totalPriceDetal = totalPriceDetal;
    }

    public void setTotalProfit(Double totalProfit) {
        this.totalProfit = totalProfit;
    }

    public void setProductTypeList(List<ProductType> productTypeList) {
        this.productTypeList = productTypeList;
    }

    public long getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }

    public Boolean getOption() {
        return option;
    }

    public Double getTotalPriceAfterDiscount() {
        return totalPriceAfterDiscount;
    }

    public Double getTotalPriceDetal() {
        return totalPriceDetal;
    }

    public Double getTotalProfit() {
        return totalProfit;
    }

    public List<ProductType> getProductTypeList() {
        return productTypeList;
    }

    public Double getTotalSellingPrice() {
        return totalSellingPrice;
    }

    public void setTotalSellingPrice(Double totalSellingPrice) {
        this.totalSellingPrice = totalSellingPrice;
    }
}

