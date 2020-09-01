package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.TileToOffer;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.CommercialOfferRepository;
import pl.koszela.nowoczesnebud.Repository.TileToOfferRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CommercialOfferService {

    private final UserRepository userRepository;
    private final CommercialOfferRepository commercialOfferRepository;
    private final TileToOfferRepository tileToOfferRepository;
    private final TilesService tilesService;

    public CommercialOfferService(UserRepository userRepository, CommercialOfferRepository commercialOfferRepository, TileToOfferRepository tileToOfferRepository, TilesService tilesService) {
        this.userRepository = userRepository;
        this.commercialOfferRepository = commercialOfferRepository;
        this.tileToOfferRepository = tileToOfferRepository;
        this.tilesService = tilesService;
    }

    public CommercialOffer saveUser (List<TileToOffer> tileToOffers, User user){
        List<TileToOffer> correctTiles = findCorrectTiles(tileToOffers);
        return commercialOfferRepository.save(new CommercialOffer(user, correctTiles));
    }

    private List<TileToOffer> findCorrectTiles (List<TileToOffer> tileToOfferList){
        List<TileToOffer> tileToOffers = new ArrayList<>();
        List<TileToOffer> allTiles = tilesService.convertToTileToOffer(tilesService.getAllTilesOrCreate());

        for (TileToOffer tileToOffer: tileToOfferList){
            for (TileToOffer tilesDTO: allTiles){
                if (tileToOffer.getManufacturer().equalsIgnoreCase(tilesDTO.getManufacturer())){
                    tileToOffers.add(tilesDTO);
                }
            }
        }
        return tileToOffers;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }
}
