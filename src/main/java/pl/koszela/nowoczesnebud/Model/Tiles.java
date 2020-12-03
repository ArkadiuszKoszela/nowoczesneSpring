package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
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
}
