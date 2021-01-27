package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Offer;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.OfferRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final InputService inputService;

    public OfferService(OfferRepository offerRepository, InputService inputService) {
        this.offerRepository = offerRepository;
        this.inputService = inputService;
    }

    @Transactional
    public Offer save(Offer offer, boolean forceSave) {
        List<Offer> offers = offerRepository.findAll();
        User user = offer.getUser();
        Optional<Offer> optOffer = offers.stream().filter(searchUser -> searchUser.getUser().getName().equalsIgnoreCase(
                user.getName()) && searchUser.getUser().getSurname().equalsIgnoreCase(
                user.getSurname()) && searchUser.getUser().getEmail().equalsIgnoreCase(user.getEmail())).findFirst();
        if (optOffer.isPresent()) {
            if (forceSave) {
                offer.getUser().setId(optOffer.get().getUser().getId());
                optOffer.get().setUser(offer.getUser());
                optOffer.get().setInputList(offer.getInputList());
                offerRepository.save(optOffer.get());
            }
            return null;
        }
        if (offer.getId() != null)
            return offerRepository.save(offer);
        inputService.saveAll(offer.getInputList());
        return offerRepository.save(offer);
    }

    public List<Offer> getCommercialOffers() {
        return offerRepository.findAll();
    }
}
