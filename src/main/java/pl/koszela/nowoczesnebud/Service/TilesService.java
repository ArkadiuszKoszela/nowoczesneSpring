package pl.koszela.nowoczesnebud.Service;

import org.apache.commons.collections.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.TileToOffer;
import pl.koszela.nowoczesnebud.Model.Tiles;
import pl.koszela.nowoczesnebud.Model.TilesDTO;
import pl.koszela.nowoczesnebud.Repository.TilesRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TilesService {

    private final TilesRepository tilesRepository;
    private final ServiceCsv serviceCsv;

    public TilesService(TilesRepository tilesRepository, ServiceCsv serviceCsv) {
        this.tilesRepository = Objects.requireNonNull(tilesRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
    }

    public List<TilesDTO> getAllTilesOrCreate() {
        if (CollectionUtils.isEmpty(tilesRepository.findAll())) {
            List<Tiles> tiles = serviceCsv.readAndSaveTiles("src/main/resources/assets/cenniki");
            tilesRepository.saveAll(tiles);
            return convertTilesToDTO(tiles);
        }
        return convertTilesToDTO(tilesRepository.findAll());
    }

    private List<TilesDTO> convertTilesToDTO(List<Tiles> getAll){
        return getAll.stream()
                .map(tiles -> new ModelMapper().map(tiles, TilesDTO.class))
                .collect(Collectors.toList());
    }

    public List<TileToOffer> convertToTileToOffer (List<TilesDTO> tilesDTOList){
        return tilesDTOList.stream().map(e -> new ModelMapper().map(e, TileToOffer.class)).collect(Collectors.toList());
    }

    public List<Tiles> clearQuantity() {
        List<Tiles> tilesRepositoryAll = tilesRepository.findAll();
        tilesRepositoryAll.forEach(tile -> {
            tile.setQuantity(0.0);
            tile.setTotalProfit(0.0);
            tile.setTotalPriceAfterDiscount(0.0);
            tile.setTotalPriceDetal(0.0);
        });
        return tilesRepository.saveAll(tilesRepositoryAll);
    }
}
