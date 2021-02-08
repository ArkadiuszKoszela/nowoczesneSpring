package pl.koszela.nowoczesnebud.Service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.GuttersRepository;
import pl.koszela.nowoczesnebud.Repository.ProductGroupRepository;
import pl.koszela.nowoczesnebud.Repository.ProductTypeRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GuttersService {

    private final GuttersRepository guttersRepository;
    private final CsvImporterImplTile csvImporterImplTile;
    private final QuantityService quantityService;
    private final ProductGroupService productGroupService;
    private final ProductTypeService productTypeService;

    public GuttersService(GuttersRepository guttersRepository, CsvImporterImplTile csvImporterImplTile,
                          @Lazy QuantityService quantityService,
                          ProductGroupService productGroupService,
                          ProductTypeService productTypeService) {
        this.guttersRepository = Objects.requireNonNull(guttersRepository);
        this.csvImporterImplTile = Objects.requireNonNull(csvImporterImplTile);
        this.quantityService = quantityService;
        this.productGroupService = productGroupService;
        this.productTypeService = productTypeService;
    }

    public List<Gutter> getAllGutters() {
        return guttersRepository.findAll();
    }

    public List<Gutter> importGutters(List<MultipartFile> list) throws IOException {
        List<Gutter> gutters = csvImporterImplTile.readAndSaveGutters(list);
        if (gutters.size() == 0)
            return gutters;
        deleteAll ();
        for (Gutter gutter : gutters) {
            for (ProductGroup productGroup : gutter.getProductGroupList()) {
                List<ProductType> productTypeList =
                        productTypeService.saveAll(productGroup.getProductTypeList());
                productGroup.setProductTypeList(productTypeList);
            }
            List<ProductGroup> productGroupList = productGroupService.saveAll(gutter.getProductGroupList());
            gutter.setProductGroupList(productGroupList);
        }
        return saveAll(gutters);
    }

    private void deleteAll () {
        List<Gutter> gutterList = getAllGutters();
        gutterList.forEach(e -> e.getProductGroupList().forEach(ProductGroup::getProductTypeList));
        guttersRepository.deleteAll(gutterList);
    }

    public List<Gutter> saveAll (List<Gutter> gutterList) {
        return guttersRepository.saveAll(gutterList);
    }

    public List<Gutter> getDiscounts() {
        return guttersRepository.findDiscounts();
    }

    public DTO editType(DTO dto) {
        return quantityService.setQuantity(dto);
    }

    public List<ProductGroup> getProductGroupsForGutter() {
        List<Gutter> gutters = getAllGutters ();
        List<ProductGroup> productGroupList = new ArrayList<>();
        for (Gutter gutter: gutters) {
            productGroupList.addAll(productGroupService.getProductGroupsForGutter(gutter.getId()));
        }
        return productGroupList;
    }
}
