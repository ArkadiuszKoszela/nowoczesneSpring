package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.OfferTemplate;

import java.util.Optional;

@Repository
public interface OfferTemplateRepository extends JpaRepository<OfferTemplate, Long> {
    
    /**
     * Znajdź domyślny szablon
     */
    Optional<OfferTemplate> findByIsDefaultTrue();
}


















