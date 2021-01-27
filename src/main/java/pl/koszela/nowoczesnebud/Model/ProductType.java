package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;

@Entity
public class ProductType {

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    @ExcelCellName("name")
    private String name;
    @ExcelCell(1)
    private BigDecimal unitDetalPrice;
    @ExcelCell(2)
    private BigDecimal quantityConverter;
    @ExcelCellName("mapperName")
    private String mapperName;
    private String unit;
    private double price;
    private double quantity = 0.0;


    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUnitDetalPrice(BigDecimal unitDetalPrice) {
        this.unitDetalPrice = unitDetalPrice;
    }

    public void setQuantityConverter(BigDecimal quantityConverter) {
        this.quantityConverter = quantityConverter;
    }

    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitDetalPrice() {
        return unitDetalPrice;
    }

    public BigDecimal getQuantityConverter() {
        return quantityConverter;
    }

    public String getMapperName() {
        return mapperName;
    }

    public String getUnit() {
        return unit;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }
}
