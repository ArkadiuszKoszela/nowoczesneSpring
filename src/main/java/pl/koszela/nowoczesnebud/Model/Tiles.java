package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;

import javax.persistence.*;

@Entity
public class Tiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ExcelCellName("basicDiscount")
    private Integer basicDiscount;
    @ExcelCellName("promotionDiscount")
    private Integer promotionDiscount;
    @ExcelCellName("additionalDiscount")
    private Integer additionalDiscount;
    @ExcelCellName("skonto")
    private Integer skontoDiscount;
    @ExcelCellName("name")
    private String name;
    @ExcelCell(1)
    private Double unitDetalPrice;
    @ExcelCell(2)
    private Double quantityConverter;
    private Double quantity = 0.0;
    private String manufacturer;
    private TotalValues totalValues;

    public Tiles() {
    }

    private double calculatePercentage(double obtained) {
        return (100 - obtained) / 100;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBasicDiscount() {
        return basicDiscount;
    }

    public void setBasicDiscount(Integer basicDiscount) {
        this.basicDiscount = basicDiscount;
    }

    public Integer getPromotionDiscount() {
        return promotionDiscount;
    }

    public void setPromotionDiscount(Integer promotionDiscount) {
        this.promotionDiscount = promotionDiscount;
    }

    public Integer getAdditionalDiscount() {
        return additionalDiscount;
    }

    public void setAdditionalDiscount(Integer additionalDiscount) {
        this.additionalDiscount = additionalDiscount;
    }

    public Integer getSkontoDiscount() {
        return skontoDiscount;
    }

    public void setSkontoDiscount(Integer skontoDiscount) {
        this.skontoDiscount = skontoDiscount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getUnitDetalPrice() {
        return unitDetalPrice;
    }

    public void setUnitDetalPrice(Double unitDetalPrice) {
        this.unitDetalPrice = unitDetalPrice;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public String toString() {
        return "Tiles{" +
                "name=" + name +
                ", unitDetalPrice=" + unitDetalPrice +
                ", quantityConverter=" + quantityConverter +
                ", basicDiscount=" + basicDiscount +
                ", additionalDiscount=" + additionalDiscount +
                ", promotionDiscount=" + promotionDiscount +
                ", skontoDiscount=" + skontoDiscount +
                '}';
    }

    public Double getQuantityConverter() {
        return quantityConverter;
    }

    public void setQuantityConverter(Double quantityConverter) {
        this.quantityConverter = quantityConverter;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
}
