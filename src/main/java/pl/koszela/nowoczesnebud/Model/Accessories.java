package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Accessories {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ExcelCellName("name")
    private String name;
    @ExcelCell(1)
    private Double unitDetalPrice;
    @ExcelCellName("unit")
    private String unit;
    @ExcelCellName("basicDiscount")
    private double basicDiscount;
    @ExcelCellName("additionalDiscount")
    private double additionalDiscount;
    @ExcelCellName("promotionDiscount")
    private double promotionDiscount;
    @ExcelCellName("skonto")
    private double skonto;
    private String manufacturer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getBasicDiscount() {
        return basicDiscount;
    }

    public void setBasicDiscount(double basicDiscount) {
        this.basicDiscount = basicDiscount;
    }

    public double getAdditionalDiscount() {
        return additionalDiscount;
    }

    public void setAdditionalDiscount(double additionalDiscount) {
        this.additionalDiscount = additionalDiscount;
    }

    public double getPromotionDiscount() {
        return promotionDiscount;
    }

    public void setPromotionDiscount(double promotionDiscount) {
        this.promotionDiscount = promotionDiscount;
    }

    public double getSkonto() {
        return skonto;
    }

    public void setSkonto(double skonto) {
        this.skonto = skonto;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
}
