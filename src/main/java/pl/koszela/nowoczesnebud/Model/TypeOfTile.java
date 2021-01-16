package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
public class TypeOfTile {

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
    private double price;
    private double quantity = 0.0;
}
