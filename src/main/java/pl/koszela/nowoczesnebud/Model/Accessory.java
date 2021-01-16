package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class Accessory {

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
    @ExcelCellName("type")
    private String type;
    private String manufacturer;
}
