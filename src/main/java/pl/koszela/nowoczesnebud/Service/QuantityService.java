package pl.koszela.nowoczesnebud.Service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Mapper;
import pl.koszela.nowoczesnebud.Model.Tiles;
import pl.koszela.nowoczesnebud.Model.TilesDTO;
import pl.koszela.nowoczesnebud.Model.TilesInput;
import pl.koszela.nowoczesnebud.Repository.TilesRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class QuantityService {

    private final TilesRepository tilesRepository;

    public QuantityService(TilesRepository tilesRepository) {
        this.tilesRepository = tilesRepository;
    }

    public void filledQuantityInTiles(List<TilesInput> tilesInputList) {
        List<Tiles> allTiles = tilesRepository.findAll();
        Mapper mapper = new Mapper();
        ModelMapper modelMapper = new ModelMapper();

        for (Tiles tile : allTiles) {
            for (Map.Entry<String, String> map : mapper.getMap().entrySet()) {
                for (TilesInput tilesInput: tilesInputList) {
                    if (tile.getName().toLowerCase().equals(map.getValue().toLowerCase())) {
                        if (map.getKey().toLowerCase().equals(tilesInput.getName().toLowerCase())) {
                            TilesDTO tilesDTO = modelMapper.map(tile, TilesDTO.class);
                            BigDecimal inputQuantity = BigDecimal.valueOf(tilesInput.getQuantity());
                            BigDecimal tileQuantity = BigDecimal.valueOf(tile.getQuantityConverter());
                            double quantityFinal = inputQuantity.multiply(tileQuantity).setScale(2, RoundingMode.HALF_UP).doubleValue();
                            tile.setQuantity(quantityFinal);
                        }
                    }
                }
            }
        }
        tilesRepository.saveAll(allTiles);
    }
}
