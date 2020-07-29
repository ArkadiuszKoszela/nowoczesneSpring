package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;

import java.util.List;

@Repository
public interface CommercialOfferRepository extends JpaRepository<CommercialOffer, Long> {
    CommercialOffer findByUserIdEquals (long user);
}
