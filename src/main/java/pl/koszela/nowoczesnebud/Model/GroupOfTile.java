package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
public class GroupOfTile {

    public GroupOfTile(String typeOfTileName, List<TypeOfTile> typeOfTileList) {
        this.typeOfTileName = typeOfTileName;
        this.totalPriceAfterDiscount = 0.0d;
        this.totalPriceDetal = 0.0d;
        this.totalProfit = 0.0d;
        this.typeOfTileList = typeOfTileList;
    }

    public GroupOfTile() {
    }

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    private String typeOfTileName;
    @Column(name = "option_name")
    private Boolean option;
    private Double totalPriceAfterDiscount = 0.0;
    private Double totalPriceDetal = 0.0;
    private Double totalProfit = 0.0;

    @OneToMany(targetEntity = TypeOfTile.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "group_of_tile_id")
    List<TypeOfTile> typeOfTileList;

}
