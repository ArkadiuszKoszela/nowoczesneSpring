package pl.koszela.nowoczesnebud.Service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.DTO;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Model.Tile;
import pl.koszela.nowoczesnebud.Repository.TileRepository;

import java.io.IOException;
import java.util.*;

@Service
public class TilesService {

    private final TileRepository tileRepository;
    private final CsvImporterImplTile csvImporterImplTile;
    private final QuantityService quantityService;
    private final ProductGroupService productGroupService;
    private final ProductTypeService productTypeService;

    public TilesService(TileRepository tileRepository, CsvImporterImplTile csvImporterImplTile,
                        @Lazy QuantityService quantityService,
                        ProductGroupService productGroupService,
                        ProductTypeService productTypeService) {
        this.tileRepository = Objects.requireNonNull(tileRepository);
        this.csvImporterImplTile = Objects.requireNonNull(csvImporterImplTile);
        this.quantityService = quantityService;
        this.productGroupService = productGroupService;
        this.productTypeService = productTypeService;
    }

    @Transactional
    public List<Tile> getAllTilesOrCreate() {
        return tileRepository.findAll();
    }

    public List<Tile> getAllTiles() {
        return tileRepository.findAll();
    }

    public List<Tile> saveAllTiles(List<Tile> tileList) {
        return tileRepository.saveAll(tileList);
    }

    public Optional<Tile> findById(long tileId) {
        return tileRepository.findById(tileId);
    }

    public List<Tile> getAllTile(List<MultipartFile> list) throws IOException {
        List<Tile> tiles = csvImporterImplTile.readAndSaveTiles(list);
        if (tiles.size() == 0)
            return tiles;
        deleteAll ();
        for (Tile tile : tiles) {
            for (ProductGroup productGroup : tile.getProductGroupList()) {
                List<ProductType> productTypeList =
                        productTypeService.saveAll(productGroup.getProductTypeList());
                productGroup.setProductTypeList(productTypeList);
            }
            List<ProductGroup> productGroupList = productGroupService.saveAll(tile.getProductGroupList());
            tile.setProductGroupList(productGroupList);
        }
        List<Tile> tileList = tileRepository.saveAll(tiles);
        return tileList;
    }

    private void deleteAll () {
        List<Tile> tileList = tileRepository.findAll();
        tileList.forEach(e -> e.getProductGroupList().forEach(ProductGroup::getProductTypeList));
        tileRepository.deleteAll(tileList);
    }

    public List<Tile> convertTilesToDTO(List<Tile> getAll) {
        return getAll;
    }

    public List<Tile> getDiscounts() {
        return tileRepository.findDiscounts();
    }

    public DTO editTypeOfTile(DTO dto) {
        long tileId = productGroupService.findIdTile(dto.getProductGroup().getId());
        Optional<Tile> tile = findById(tileId);
        if (!tile.isPresent())
            return dto;
        return quantityService.setQuantity(dto);
    }

    public List<ProductGroup> getProductGroupsForTile() {
        List<Tile> tiles = getAllTiles ();
        List<ProductGroup> productGroupList = new ArrayList<>();
        for (Tile tile: tiles) {
            productGroupList.addAll(productGroupService.getProductGroupsForTile(tile.getId()));
        }
        return productGroupList;
    }
}
