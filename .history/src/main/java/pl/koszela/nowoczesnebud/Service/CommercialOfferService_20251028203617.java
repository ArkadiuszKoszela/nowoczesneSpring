package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.Customer;
import pl.koszela.nowoczesnebud.Repository.CommercialOfferRepository;
import pl.koszela.nowoczesnebud.Repository.CustomerRepository;

import java.util.List;
import java.util.Optional;

/**
 * Serwis do zarządzania ofertami komercyjnymi
 * ZASTĘPUJE: OfferService
 */
@Service
public class CommercialOfferService {

    private final CommercialOfferRepository offerRepository;
    private final CustomerRepository customerRepository;

    public CommercialOfferService(CommercialOfferRepository offerRepository,
                                 CustomerRepository customerRepository) {
        this.offerRepository = offerRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Pobierz wszystkie oferty
     */
    public List<CommercialOffer> getAllOffers() {
        return offerRepository.findAll();
    }

    /**
     * Pobierz ofertę po ID
     */
    public Optional<CommercialOffer> getOfferById(Long id) {
        return offerRepository.findById(id);
    }

    /**
     * Zapisz ofertę
     * DOKŁADNIE TA SAMA LOGIKA co w OfferService.save()
     */
    @Transactional
    public CommercialOffer saveOffer(CommercialOffer offer, boolean forceSave) {
        Customer customer = offer.getCustomer();
        
        // Sprawdź czy klient już istnieje
        Optional<Customer> existingCustomer = customerRepository.findByNameAndSurnameAndEmail(
            customer.getName(),
            customer.getSurname(),
            customer.getEmail()
        );

        if (existingCustomer.isPresent()) {
            // Klient istnieje
            if (forceSave) {
                // Aktualizuj istniejącego klienta
                customer.setId(existingCustomer.get().getId());
                offer.setCustomer(customer);
                return offerRepository.save(offer);
            } else {
                // Nie zapisuj - klient już istnieje
                return null;
            }
        }

        // Nowy klient - zapisz ofertę
        return offerRepository.save(offer);
    }

    /**
     * Usuń ofertę
     */
    @Transactional
    public void deleteOffer(Long id) {
        offerRepository.deleteById(id);
    }
}

