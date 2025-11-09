package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;

@Repository
public interface CommercialOfferRepository extends JpaRepository<CommercialOffer, Long> {
}

