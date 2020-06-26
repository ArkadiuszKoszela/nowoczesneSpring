package pl.koszela.nowoczesnebud.Model;

import com.opencsv.bean.CsvBindByName;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Tiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @CsvBindByName(column = "basicDiscount", required = true)
    private Integer basicDiscount;
    @CsvBindByName(column = "promotionDiscount", required = true)
    private Integer promotionDiscount;
    @CsvBindByName(column = "additionalDiscount", required = true)
    private Integer additionalDiscount;
    @CsvBindByName(column = "skontoDiscount", required = true)
    private Integer skontoDiscount;
    @CsvBindByName(column = "name")
    private String name;
    @CsvBindByName(column = "unitDetalPrice", required = true)
    private Double unitDetalPrice;
    private String manufacturer;

    public Tiles() {
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
//        StringBuilder builder = new StringBuilder();
//        builder.append("Tiles{name=").append(name).append(", unitDetalPrice=")
//                .append(unitDetalPrice).append(", basicDiscount=").append(basicDiscount)
//                .append(", additionalDiscount=").append(additionalDiscount)
//                .append(", promotionDiscount=").append(promotionDiscount)
//                .append(", skontoDiscount=").append(skontoDiscount).append("}");
//
//        return builder.toString();
        return "Tiles{" +
                "name=" + name +
                ", unitDetalPrice=" + unitDetalPrice +
                ", basicDiscount=" + basicDiscount +
                ", additionalDiscount=" + additionalDiscount +
                ", promotionDiscount=" + promotionDiscount +
                ", skontoDiscount=" + skontoDiscount +
                '}';
    }
}
