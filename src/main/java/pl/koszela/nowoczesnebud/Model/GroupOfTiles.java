package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Data
@Entity
public class GroupOfTiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @OneToMany(targetEntity = CommercialOffer.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "commercial_offer_id")
    private Set<CommercialOffer> commercialOffers;
    @OneToMany(targetEntity = TilesInput.class, cascade = CascadeType.ALL)
    @JoinColumn (name = "tiles_input_id")
    private List<TilesInput> tilesInputs;
}
