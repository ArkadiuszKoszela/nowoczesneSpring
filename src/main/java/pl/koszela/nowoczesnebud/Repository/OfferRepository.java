package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.Offer;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    Offer findByUserIdEquals (long user);

}
