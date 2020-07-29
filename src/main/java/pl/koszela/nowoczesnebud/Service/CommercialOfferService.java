package pl.koszela.nowoczesnebud.Service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.TileToOffer;
import pl.koszela.nowoczesnebud.Model.TilesDTO;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.CommercialOfferRepository;
import pl.koszela.nowoczesnebud.Repository.TileToOfferRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

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
        CommercialOffer commercialOffer = new CommercialOffer(user, correctTiles);
        userRepository.save(user);
        tileToOfferRepository.saveAll(correctTiles);
        return commercialOfferRepository.save(commercialOffer);
    }

    private List<TileToOffer> findCorrectTiles (List<TileToOffer> tileToOfferList){
        ModelMapper modelMapper = new ModelMapper();
        List<TileToOffer> tileToOffers = new ArrayList<>();
        List<TilesDTO> allTiles = tilesService.getAllTiles();
        for (TileToOffer tileToOffer: tileToOfferList){
            for (TilesDTO tilesDTO: allTiles){
                if (tileToOffer.getManufacturer().equalsIgnoreCase(tilesDTO.getManufacturer())){
                    TileToOffer tileToOffer1 = modelMapper.map(tilesDTO, TileToOffer.class);
                    tileToOffers.add(tileToOffer1);
                }
            }
        }
        return tileToOffers;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }
}
