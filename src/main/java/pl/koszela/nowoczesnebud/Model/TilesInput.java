package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class TilesInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String mapperName;
    private double quantity;
}
