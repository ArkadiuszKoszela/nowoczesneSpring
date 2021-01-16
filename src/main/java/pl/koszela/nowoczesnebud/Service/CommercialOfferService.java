package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Repository.CommercialOfferRepository;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class CommercialOfferService {

    private final CommercialOfferRepository commercialOfferRepository;
    private final TilesService tilesService;

    public CommercialOfferService(CommercialOfferRepository commercialOfferRepository, TilesService tilesService) {
        this.commercialOfferRepository = commercialOfferRepository;
        this.tilesService = tilesService;
    }

    @Transactional
    public CommercialOffer save (CommercialOffer commercialOffer){
//        tilesService.saveGroupOfTiles(commercialOffer.getGroupOfTiles());
        return commercialOfferRepository.save(commercialOffer);
    }

    public List<CommercialOffer> getCommercialOffers() {
        return commercialOfferRepository.findAll();
    }
}
