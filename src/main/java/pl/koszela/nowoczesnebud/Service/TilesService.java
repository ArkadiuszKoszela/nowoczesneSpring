package pl.koszela.nowoczesnebud.Service;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.DTO;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Model.Tile;
import pl.koszela.nowoczesnebud.Repository.ProductGroupRepository;
import pl.koszela.nowoczesnebud.Repository.ProductTypeRepository;
import pl.koszela.nowoczesnebud.Repository.TileRepository;

import java.io.IOException;
import java.util.*;

@Service
public class TilesService {

    private final TileRepository tileRepository;
    private final ServiceCsv serviceCsv;
    private final QuantityService quantityService;
    private final ProductGroupRepository productGroupRepository;
    private final ProductTypeRepository productTypeRepository;

    public TilesService(TileRepository tileRepository, ServiceCsv serviceCsv, @Lazy QuantityService quantityService,
                        ProductGroupRepository productGroupRepository,
                        ProductTypeRepository productTypeRepository) {
        this.tileRepository = Objects.requireNonNull(tileRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
        this.quantityService = quantityService;
        this.productGroupRepository = productGroupRepository;
        this.productTypeRepository = productTypeRepository;
    }

    @Transactional
    public List<Tile> getAllTilesOrCreate() {
        List<Tile> allTiles = tileRepository.findAll();
        if (CollectionUtils.isEmpty(allTiles)) {
            List<Tile> tiles = serviceCsv.readAndSaveTiles("src/main/resources/assets/cenniki");
            for (Tile tile : tiles) {
                for (ProductGroup productGroup : tile.getProductGroupList()) {
                    List<ProductType> productTypeList =
                            productTypeRepository.saveAll(productGroup.getProductTypeList());
                    productGroup.setProductTypeList(productTypeList);
                }
                List<ProductGroup> productGroupList = productGroupRepository.saveAll(tile.getProductGroupList());
                tile.setProductGroupList(productGroupList);
            }
            tileRepository.saveAll(tiles);
            return convertTilesToDTO(tiles);
        }
        return convertTilesToDTO(allTiles);
    }

    public List<ProductType> saveProductTypes(List<ProductType> productTypeList) {
        return productTypeRepository.saveAll(productTypeList);
    }

    public ProductGroup saveProductGroup(ProductGroup productGroupList) {
        return productGroupRepository.save(productGroupList);
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

    public void saveTile(Tile tile) {
        tileRepository.save(tile);
    }

    public Tile findTileByProductGroupId(long id) {
        return tileRepository.findTileByProductGroupId(id);
    }

    public List<Tile> getAllTile(List<MultipartFile> list) throws IOException {
        List<Tile> tiles = serviceCsv.readAndSaveTiles(list);
        if (tiles.size() == 0)
            return tiles;
        deleteAll ();
        for (Tile tile : tiles) {
            for (ProductGroup productGroup : tile.getProductGroupList()) {
                List<ProductType> productTypeList =
                        productTypeRepository.saveAll(productGroup.getProductTypeList());
                productGroup.setProductTypeList(productTypeList);
            }
            List<ProductGroup> productGroupList = productGroupRepository.saveAll(tile.getProductGroupList());
            tile.setProductGroupList(productGroupList);
        }
        return tileRepository.saveAll(tiles);
    }

    private void deleteAll () {
        List<Tile> tileList = tileRepository.findAll();
        tileList.forEach(e -> e.getProductGroupList().forEach(ProductGroup::getProductTypeList));
        tileRepository.deleteAll(tileList);
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
        return getAllTiles();
    }

    public DTO editTypeOfTile(DTO dto) {
        long tileId = productGroupRepository.findIdTile(dto.getProductGroup().getId());
        Optional<Tile> tile = findById(tileId);
        if (!tile.isPresent())
            return dto;
        return quantityService.setQuantity(dto, tile.get());
    }

    public ProductGroup setOption(ProductGroup updateProductGroup) {
        List<ProductType> productTypeList = productTypeRepository.findProductsTypes(updateProductGroup.getId());
        updateProductGroup.setProductTypeList(productTypeList);
        return productGroupRepository.save(updateProductGroup);
    }

    public List<ProductGroup> getProductGroups(long id) {
        return productGroupRepository.findProductsGroupForTile(id);
    }

    public List<ProductType> getProductTypes(long id) {
        return productTypeRepository.findProductsTypes(id);
    }
}
