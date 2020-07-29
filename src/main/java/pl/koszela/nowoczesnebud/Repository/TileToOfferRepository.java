package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.TileToOffer;

@Repository
public interface TileToOfferRepository extends JpaRepository<TileToOffer, Long> {
}
