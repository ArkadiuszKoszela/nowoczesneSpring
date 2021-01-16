package pl.koszela.nowoczesnebud.Service;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.TileRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TilesService {

    private final TileRepository tileRepository;
    private final ServiceCsv serviceCsv;
    private final QuantityService quantityService;

    public TilesService(TileRepository tileRepository, ServiceCsv serviceCsv, QuantityService quantityService) {
        this.tileRepository = Objects.requireNonNull(tileRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
        this.quantityService = quantityService;
    }

    public List<Tile> getAllTilesOrCreate() {
        List<Tile> allTiles = tileRepository.findAll();
        if (CollectionUtils.isEmpty(allTiles)) {
            List<Tile> tiles = serviceCsv.readAndSaveTiles("src/main/resources/assets/cenniki");
            tileRepository.saveAll(tiles);
            return convertTilesToDTO(tiles);
        }
        return convertTilesToDTO(allTiles);
    }

    public List<Tile> getAllTile(List<MultipartFile> list) throws IOException {
        List<Tile> tiles = serviceCsv.readAndSaveTiles(list);
        if (tiles.size() == 0)
            return tiles;
        tileRepository.deleteAll();
        tileRepository.saveAll(tiles);
        return tiles;
    }

    public List<Tile> convertTilesToDTO(List<Tile> getAll) {
        return getAll;
    }

    public List<Tile> clearQuantity() {
        List<Tile> tileRepositoryAll = tileRepository.findAll();
        return tileRepository.saveAll(tileRepositoryAll);
    }

    public List<String> getTilesManufacturers() {
        return tileRepository.findManufacturers();
    }

    public List<Tile> getDiscounts() {
        return tileRepository.findDiscounts();
    }

    public List<Tile> saveDiscounts(Tile tileToSave) {
        Optional<Tile> tile = tileRepository.findById(tileToSave.getId());
        if (!tile.isPresent())
            return new ArrayList<>();
        tile.get().setBasicDiscount(tileToSave.getBasicDiscount());
        tile.get().setAdditionalDiscount(tileToSave.getAdditionalDiscount());
        tile.get().setPromotionDiscount(tileToSave.getPromotionDiscount());
        tile.get().setSkontoDiscount(tileToSave.getSkontoDiscount());
        tileRepository.save(tile.get());
        return getDiscounts();
    }

    public List<Tile> editTypeOfTile(TypeOfTile typeOfTileToUpdate) {
        return quantityService.setQuantity(typeOfTileToUpdate);
    }
}
