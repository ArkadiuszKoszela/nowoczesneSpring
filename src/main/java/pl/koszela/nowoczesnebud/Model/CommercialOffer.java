package pl.koszela.nowoczesnebud.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "COMMERCIAL_OFFER")
public class CommercialOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @OneToOne(targetEntity = User.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;
    @OneToMany(targetEntity = TileToOffer.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "tile_to_offer_id")
    private List<TileToOffer> tileToOffer;

    public CommercialOffer() {
        super();
    }

    public CommercialOffer(User user, List<TileToOffer> tileToOffer) {
        this.user = user;
        this.tileToOffer = tileToOffer;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<TileToOffer> getTileToOffer() {
        return tileToOffer;
    }

    public void setTileToOffer(List<TileToOffer> tileToOffer) {
        this.tileToOffer = tileToOffer;
    }
}
