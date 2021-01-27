package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Gutter {

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    private String manufacturer;

    @ExcelCellName("basicDiscount")
    private int basicDiscount = 0;
    @ExcelCellName("promotionDiscount")
    private int promotionDiscount;
    @ExcelCellName("additionalDiscount")
    private int additionalDiscount;
    @ExcelCellName("skonto")
    private int skontoDiscount;

    @OneToMany(targetEntity = ProductGroup.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "gutter_id")
    @JsonIgnore
    List<ProductGroup> productGroupList = new ArrayList<>();
}
