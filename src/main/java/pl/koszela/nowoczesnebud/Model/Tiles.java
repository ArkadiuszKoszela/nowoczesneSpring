package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
public class Tiles {

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    @ExcelCellName("basicDiscount")
    private int basicDiscount = 0;
    @ExcelCellName("promotionDiscount")
    private int promotionDiscount;
    @ExcelCellName("additionalDiscount")
    private int additionalDiscount;
    @ExcelCellName("skonto")
    private int skontoDiscount;
    @ExcelCellName("name")
    private String name;
    @ExcelCell(1)
    private BigDecimal unitDetalPrice;
    @ExcelCell(2)
    private BigDecimal quantityConverter;
    private double quantity = 0.0;
    private String manufacturer;
    private Double totalPriceAfterDiscount = 0.0;
    private Double totalPriceDetal = 0.0;
    private Double totalProfit = 0.0;

    private double calculatePercentage(double obtained) {
        return (100 - obtained) / 100;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getBasicDiscount() {
        return basicDiscount;
    }

    public void setBasicDiscount(int basicDiscount) {
        this.basicDiscount = basicDiscount;
    }

    public int getPromotionDiscount() {
        return promotionDiscount;
    }

    public void setPromotionDiscount(int promotionDiscount) {
        this.promotionDiscount = promotionDiscount;
    }

    public int getAdditionalDiscount() {
        return additionalDiscount;
    }

    public void setAdditionalDiscount(int additionalDiscount) {
        this.additionalDiscount = additionalDiscount;
    }

    public int getSkontoDiscount() {
        return skontoDiscount;
    }

    public void setSkontoDiscount(int skontoDiscount) {
        this.skontoDiscount = skontoDiscount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getUnitDetalPrice() {
        return unitDetalPrice;
    }

    public void setUnitDetalPrice(BigDecimal unitDetalPrice) {
        this.unitDetalPrice = unitDetalPrice;
    }

    public BigDecimal getQuantityConverter() {
        return quantityConverter;
    }

    public void setQuantityConverter(BigDecimal quantityConverter) {
        this.quantityConverter = quantityConverter;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
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

    public Double getTotalPriceAfterDiscount() {
        return totalPriceAfterDiscount;
    }

    public void setTotalPriceAfterDiscount(Double totalPriceAfterDiscount) {
        this.totalPriceAfterDiscount = totalPriceAfterDiscount;
    }

    public Double getTotalPriceDetal() {
        return totalPriceDetal;
    }

    public void setTotalPriceDetal(Double totalPriceDetal) {
        this.totalPriceDetal = totalPriceDetal;
    }

    public Double getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(Double totalProfit) {
        this.totalProfit = totalProfit;
    }
}
