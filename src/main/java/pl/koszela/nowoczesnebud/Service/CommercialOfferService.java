package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.TileToOffer;
import pl.koszela.nowoczesnebud.Repository.CommercialOfferRepository;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

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
        tilesService.saveGroupOfTiles(commercialOffer.getGroupOfTiles());
        return commercialOfferRepository.save(commercialOffer);
    }

    public List<TileToOffer> findCorrectTiles (List<TileToOffer> tileToOfferList){
        List<TileToOffer> allTiles = tilesService.convertToTileToOffer(tilesService.getAllTilesOrCreate());
        Set<String> manufacturerNames = tileToOfferList.stream()
                        .map(TileToOffer::getManufacturer)
                        .collect(Collectors.toSet());
        return allTiles.stream()
                        .filter(e -> manufacturerNames.contains(e.getManufacturer()))
                        .collect(Collectors.toList());
    }

    public List<CommercialOffer> getCommercialOffers() {
        return commercialOfferRepository.findAll();
    }
}
