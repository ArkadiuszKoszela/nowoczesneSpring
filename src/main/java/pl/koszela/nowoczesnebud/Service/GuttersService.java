package pl.koszela.nowoczesnebud.Service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.GuttersRepository;
import pl.koszela.nowoczesnebud.Repository.ProductGroupRepository;
import pl.koszela.nowoczesnebud.Repository.ProductTypeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GuttersService {

    private final GuttersRepository guttersRepository;
    private final ServiceCsv serviceCsv;
    private final ProductGroupRepository productGroupRepository;
    private final ProductTypeRepository productTypeRepository;
    private final QuantityService quantityService;

    public GuttersService(GuttersRepository guttersRepository, ServiceCsv serviceCsv,
                          ProductGroupRepository productGroupRepository,
                          ProductTypeRepository productTypeRepository,
                          @Lazy QuantityService quantityService) {
        this.guttersRepository = Objects.requireNonNull(guttersRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
        this.productGroupRepository = productGroupRepository;
        this.productTypeRepository = productTypeRepository;
        this.quantityService = quantityService;
    }

    public List<Gutter> getAllGutters() {
        if (CollectionUtils.isEmpty(guttersRepository.findAll())) {
            List<Gutter> gutterList = serviceCsv.readAndSaveGutters("src/main/resources/assets/rynny");
            for (Gutter gutter : gutterList) {
                for (ProductGroup productGroup : gutter.getProductGroupList()) {
                    List<ProductType> productTypeList =
                            productTypeRepository.saveAll(productGroup.getProductTypeList());
                    productGroup.setProductTypeList(productTypeList);
                }
                List<ProductGroup> productGroupList = productGroupRepository.saveAll(gutter.getProductGroupList());
                gutter.setProductGroupList(productGroupList);
            }
            return guttersRepository.saveAll(gutterList);
        }
        return guttersRepository.findAll();
    }

    public List<Gutter> saveAll (List<Gutter> gutterList) {
        return guttersRepository.saveAll(gutterList);
    }

    public List<Gutter> getDiscounts() {
        return guttersRepository.findDiscounts();
    }

    public List<ProductGroup> getProductGroups(long id) {
        return productGroupRepository.findProductsGroupForGutter(id);
    }

    public List<ProductType> getProductTypes(long id) {
        return productTypeRepository.findProductsTypes(id);
    }

    public List<Gutter> saveDiscounts(Gutter tileToSave) {
        Optional<Gutter> gutters = guttersRepository.findById(tileToSave.getId());
        if (!gutters.isPresent())
            return new ArrayList<>();
        gutters.get().setBasicDiscount(tileToSave.getBasicDiscount());
        gutters.get().setAdditionalDiscount(tileToSave.getAdditionalDiscount());
        gutters.get().setPromotionDiscount(tileToSave.getPromotionDiscount());
        gutters.get().setSkontoDiscount(tileToSave.getSkontoDiscount());
        guttersRepository.save(gutters.get());
        return getDiscounts();
    }

    public DTO editType(DTO dto) {
        long idGutter = productGroupRepository.findIdGutter(dto.getProductGroup().getId());
        Optional<Gutter> gutter = guttersRepository.findById(idGutter);
        if (!gutter.isPresent())
            return dto;
        return quantityService.setQuantity(dto, gutter.get());
    }

    public ProductGroup setOption(ProductGroup updateProductGroup) {
        List<ProductType> productTypeList = productTypeRepository.findProductsTypes(updateProductGroup.getId());
        updateProductGroup.setProductTypeList(productTypeList);
        return productGroupRepository.save(updateProductGroup);
    }
}
