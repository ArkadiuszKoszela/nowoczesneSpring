package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Mapper;
import pl.koszela.nowoczesnebud.Model.Tiles;
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

        for (Tiles tile : allTiles) {
            for (Map.Entry<String, String> map : mapper.getMap().entrySet()) {
                for (TilesInput tilesInput : tilesInputList) {
                    if (tile.getName().toLowerCase().equalsIgnoreCase(map.getValue().toLowerCase()) && map.getKey().toLowerCase().equalsIgnoreCase(tilesInput.getName().toLowerCase())) {
                        tile.setQuantity(calculateQuantity(tilesInput, tile));
                        tile.setTotalPriceDetal(calculateTotalPriceDetal(tile));
                        tile.setTotalPriceAfterDiscount(calculateTotalPriceAfterDiscount(tile));
                        tile.setTotalProfit(calculateTotalProfit(tile));
                    }
                }
            }
        }
        tilesRepository.saveAll(allTiles);
    }

    private double calculateTotalPriceDetal(Tiles tile) {
        return BigDecimal.valueOf(tile.getQuantity())
                .multiply(tile.getUnitDetalPrice()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateTotalPriceAfterDiscount(Tiles tile) {
        return BigDecimal.valueOf(tile.getTotalPriceDetal()
                * convertPercents(tile.getBasicDiscount())
                * convertPercents(tile.getAdditionalDiscount())
                * convertPercents(tile.getPromotionDiscount())
                * convertPercents(tile.getSkontoDiscount())).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateTotalProfit(Tiles tile) {
        return BigDecimal.valueOf(tile.getTotalPriceDetal()
                - tile.getTotalPriceAfterDiscount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double convertPercents(double value) {
        return (100 - value) / 100;
    }

    private double calculateQuantity(TilesInput tilesInput, Tiles tile) {
        return BigDecimal.valueOf(tilesInput.getQuantity())
                .multiply(tile.getQuantityConverter()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

