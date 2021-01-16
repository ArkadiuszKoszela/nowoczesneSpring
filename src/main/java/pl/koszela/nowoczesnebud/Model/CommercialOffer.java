package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "COMMERCIAL_OFFER")
public class CommercialOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(targetEntity = User.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;
    @OneToMany(targetEntity = TileToOffer.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "tile_to_offer_id")
    private List<TileToOffer> tileToOffer;

}
