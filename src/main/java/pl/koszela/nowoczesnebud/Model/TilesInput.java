package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class TilesInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private double quantity;
    @ManyToOne
    private GroupOfTiles groupOfTiles;
}
