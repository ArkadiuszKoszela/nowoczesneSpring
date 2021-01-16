package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.GroupOfTileRepository;
import pl.koszela.nowoczesnebud.Repository.GuttersRepository;
import pl.koszela.nowoczesnebud.Repository.TileRepository;
import pl.koszela.nowoczesnebud.Repository.TypeOfTileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuantityService {

    private final TypeOfTileRepository typeOfTileRepository;
    private final GroupOfTileRepository groupOfTileRepository;
    private final TileRepository tileRepository;
    private final GuttersRepository guttersRepository;

    public QuantityService(TypeOfTileRepository typeOfTileRepository, GroupOfTileRepository groupOfTileRepository, TileRepository tileRepository, GuttersRepository guttersRepository) {
        this.typeOfTileRepository = typeOfTileRepository;
        this.groupOfTileRepository = groupOfTileRepository;
        this.tileRepository = tileRepository;
        this.guttersRepository = guttersRepository;
    }

    public List<Tile> filledQuantityInTiles(List<TilesInput> tilesInputList) {
        List<TypeOfTile> typeOfTileList = typeOfTileRepository.findAll();
        List<TypeOfTile> typeOfTilesToUpdate = new ArrayList<>();
        for (TypeOfTile typeOfTile : typeOfTileList) {
                for (TilesInput tilesInput : tilesInputList) {
                    double quantityToSet = tilesInput.getQuantity();
                    String mapperValue = tilesInput.getMapperName();
                    String mapper = typeOfTile.getMapperName();
                    if (mapperValue.equalsIgnoreCase(mapper))
                        calculateQuantity (typeOfTile, quantityToSet);
                    typeOfTilesToUpdate.add (typeOfTile);
            }
        }
        typeOfTileRepository.saveAll(typeOfTilesToUpdate);

        List<Tile> allTiles =  tileRepository.findAll();

        for (Tile tile : allTiles) {
            for (GroupOfTile groupOfTile : tile.getGroupOfTileList()) {
                double sumPrice = groupOfTile.getTypeOfTileList().stream().mapToDouble(TypeOfTile::getPrice).sum();
                groupOfTile.setTotalPriceDetal(BigDecimal.valueOf(sumPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
                groupOfTile.setTotalPriceAfterDiscount(calculateTotalPriceAfterDiscount(sumPrice, tile));
                groupOfTile.setTotalProfit(calculateTotalProfit(groupOfTile));
            }
        }

        return tileRepository.saveAll(allTiles);
    }

    public List<Gutter> filledQuantityInGutters(List<TilesInput> tilesInputList) {
        List<TypeOfTile> typeOfTileList = typeOfTileRepository.findAll();
        List<TypeOfTile> typeOfTilesToUpdate = new ArrayList<>();
        for (TypeOfTile typeOfTile : typeOfTileList) {
            for (TilesInput tilesInput : tilesInputList) {
                double quantityToSet = tilesInput.getQuantity();
                String mapperValue = tilesInput.getMapperName();
                String mapper = typeOfTile.getMapperName();
                if (mapperValue.equalsIgnoreCase(mapper))
                    calculateQuantity (typeOfTile, quantityToSet);
                typeOfTilesToUpdate.add (typeOfTile);
            }
        }
        typeOfTileRepository.saveAll(typeOfTilesToUpdate);

        List<Gutter> allGuters =  guttersRepository.findAll();

        for (Gutter gutter : allGuters) {
            for (GroupOfTile groupOfTile : gutter.getGroupOfTileList()) {
                double sumPrice = groupOfTile.getTypeOfTileList().stream().mapToDouble(TypeOfTile::getPrice).sum();
                groupOfTile.setTotalPriceDetal(BigDecimal.valueOf(sumPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
                groupOfTile.setTotalPriceAfterDiscount(calculateTotalPriceAfterDiscount(sumPrice, gutter));
                groupOfTile.setTotalProfit(calculateTotalProfit(groupOfTile));
            }
        }

        return guttersRepository.saveAll(allGuters);
    }

    public List<Tile> setQuantity (TypeOfTile typeOfTileToUpdate) {
        Optional<TypeOfTile> typeOfTile = typeOfTileRepository.findById(typeOfTileToUpdate.getId());
        if (!typeOfTile.isPresent())
            return new ArrayList<>();
        typeOfTile.get().setQuantity(typeOfTileToUpdate.getQuantity());
        BigDecimal price = BigDecimal.valueOf(typeOfTile.get().getQuantity() * typeOfTile.get().getUnitDetalPrice().doubleValue());
        typeOfTile.get().setPrice(price.setScale(2 ,RoundingMode.HALF_UP).doubleValue());

        typeOfTileRepository.save(typeOfTile.get());
        long idGroupOfTile = typeOfTileRepository.findIdGroupOfType(typeOfTileToUpdate.getId());
        Optional<GroupOfTile> groupOfTile = groupOfTileRepository.findById(idGroupOfTile);
        if (!groupOfTile.isPresent()) {
            return new ArrayList<>();
        }
        long tileId = groupOfTileRepository.findIdTile(groupOfTile.get().getId());
        Optional<Tile> tile = tileRepository.findById(tileId);

        double sumPrice = groupOfTile.get().getTypeOfTileList().stream().mapToDouble(TypeOfTile::getPrice).sum();
        groupOfTile.get().setTotalPriceDetal(BigDecimal.valueOf(sumPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
        groupOfTile.get().setTotalPriceAfterDiscount(calculateTotalPriceAfterDiscount(sumPrice, tile.get()));
        groupOfTile.get().setTotalProfit(calculateTotalProfit(groupOfTile.get()));

        for (GroupOfTile groupOfTile1 :tile.get().getGroupOfTileList()) {
            if (groupOfTile1.getId() == groupOfTile.get().getId()) {
                groupOfTile1 = groupOfTile.get();
            }
        }

        tileRepository.save(tile.get());
        return tileRepository.findAll();
    }

    private double calculateTotalPriceAfterDiscount(double sumPrice, Tile tile) {
        return BigDecimal.valueOf(sumPrice
                * convertPercents(tile.getBasicDiscount())
                * convertPercents(tile.getAdditionalDiscount())
                * convertPercents(tile.getPromotionDiscount())
                * convertPercents(tile.getSkontoDiscount())).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateTotalPriceAfterDiscount(double sumPrice, Gutter gutter) {
        return BigDecimal.valueOf(sumPrice
                * convertPercents(gutter.getBasicDiscount())
                * convertPercents(gutter.getAdditionalDiscount())
                * convertPercents(gutter.getPromotionDiscount())
                * convertPercents(gutter.getSkontoDiscount())).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateTotalProfit(GroupOfTile tile) {
        return BigDecimal.valueOf(tile.getTotalPriceDetal()
                - tile.getTotalPriceAfterDiscount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double convertPercents(double value) {
        return (100 - value) / 100;
    }

    private void calculateQuantity(TypeOfTile typeOfTile, double valueToSet) {
        double quantity = BigDecimal.valueOf(valueToSet)
                .multiply(typeOfTile.getQuantityConverter()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        typeOfTile.setQuantity(quantity);
        double price = BigDecimal.valueOf(quantity).multiply(typeOfTile.getUnitDetalPrice()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        typeOfTile.setPrice(price);
    }
}

