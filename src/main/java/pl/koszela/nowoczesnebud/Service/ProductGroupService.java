package pl.koszela.nowoczesnebud.Service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Repository.ProductGroupRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductGroupService {

    private final ProductGroupRepository productGroupRepository;
    private final ProductTypeService productTypeService;

    public ProductGroupService(ProductGroupRepository productGroupRepository,
                               @Lazy ProductTypeService productTypeService) {
        this.productGroupRepository = productGroupRepository;
        this.productTypeService = productTypeService;
    }

    public ProductGroup setOption(ProductGroup updateProductGroup) {
        List<ProductType> productTypeList = productTypeService.findProductTypesByProductGroupId(updateProductGroup.getId());
        updateProductGroup.setProductTypeList(productTypeList);
        return save(updateProductGroup);
    }

    public List<ProductGroup> getProductGroupsForTile(long id) {
        return productGroupRepository.findProductsGroupForTile(id);
    }

    public ProductGroup findById (long id) {
        return productGroupRepository.findById(id).orElse(null);
    }

    public List<ProductGroup> saveDiscounts(ProductType productTypeToSave) {
        if (productTypeToSave.getMapperName().equalsIgnoreCase("Powierzchnia polaci")) {
            double purchasePrice = productTypeService.calculatePurchasePrice(productTypeToSave.getDetalPrice(), productTypeToSave);
            productTypeToSave.setPurchasePrice(purchasePrice);
            productTypeService.save(productTypeToSave);
            long idGroup = productTypeService.findIdGroupOfType(productTypeToSave.getId());
            ProductGroup productGroup = findById(idGroup);
            double sumPurchasePrice = productGroup.getProductTypeList().stream().mapToDouble(
                    e -> e.getQuantity() * e.getPurchasePrice()).sum();
            productGroup.setTotalPriceAfterDiscount(QuantityService.setScale(sumPurchasePrice));
            save(productGroup);
            return getAllProductGroups ();
        }
        long idGroup = productTypeService.findIdGroupOfType(productTypeToSave.getId());
        ProductGroup productGroup = findById(idGroup);
        List<ProductType> productTypeListToEdit =
                productGroup.getProductTypeList().stream().filter(e -> !e.getMapperName().equalsIgnoreCase(
                        "Powierzchnia polaci")).collect(Collectors.toList());
        for (ProductType productType: productTypeListToEdit) {
            productType.setBasicDiscount(productTypeToSave.getBasicDiscount());
            productType.setAdditionalDiscount(productTypeToSave.getAdditionalDiscount());
            productType.setPromotionDiscount(productTypeToSave.getPromotionDiscount());
            productType.setSkontoDiscount(productTypeToSave.getSkontoDiscount());
            double purchasePrice = productTypeService.calculatePurchasePrice(productType.getDetalPrice(), productType);
            productType.setPurchasePrice(purchasePrice);
        }
        productTypeService.saveAll(productTypeListToEdit);
        long idGroup1 = productTypeService.findIdGroupOfType(productTypeToSave.getId());
        ProductGroup productGroup1 = findById(idGroup1);
        double sumPurchasePrice = productGroup1.getProductTypeList().stream().mapToDouble(
                e -> e.getQuantity() * e.getPurchasePrice()).sum();
        productGroup1.setTotalPriceAfterDiscount(QuantityService.setScale(sumPurchasePrice));
        save(productGroup1);
        return getAllProductGroups ();
    }

    public List<ProductGroup> getAllProductGroups () {
        return productGroupRepository.findAll();
    }

    public ProductGroup save (ProductGroup toSave) {
        return productGroupRepository.save(toSave);
    }

    public List<ProductGroup> saveAll (List<ProductGroup> toSave) {
        return productGroupRepository.saveAll(toSave);
    }

    public List<ProductGroup> getProductGroupsForGutter(long id) {
        return productGroupRepository.findProductsGroupForGutter(id);
    }

    public long findIdTile (long id) {
        return productGroupRepository.findIdTile(id);
    }

    public List<ProductGroup> calculateMargin(Integer margin, Integer discount, List<ProductGroup> productGroupList) {
        for (ProductGroup productGroup: productGroupList) {
            for (ProductType productType: productGroup.getProductTypeList()) {
                if (margin != null) {
                    double sellingPrice = productType.getPurchasePrice() * (100 + margin) / 100;
                    productType.setSellingPrice(QuantityService.setScale(sellingPrice));
                }
                if (discount != null) {
                    double sellingPrice = productType.getDetalPrice() * (100 - discount) / 100;
                    productType.setSellingPrice(QuantityService.setScale(sellingPrice));
                }
            }
            double sumSellingPrice = productGroup.getProductTypeList().stream().mapToDouble(
                    productType -> productType.getQuantity() * productType.getSellingPrice()).sum();
            productGroup.setTotalSellingPrice(QuantityService.setScale(sumSellingPrice));
        }
        return productGroupList;
    }

    public ProductGroup findMainProductGroup () {
        return productGroupRepository.findProductGroupIsOptionIsMain ().stream().findFirst().orElse(null);
    }

    public boolean hasOnlyOneMainProductGroup () {
        List<ProductGroup> productGroups = productGroupRepository.findProductGroupIsOptionIsMain();
        return productGroups != null && productGroups.size() > 1;
    }

    public List<ProductGroup> findOptionProductGroups () {
        return productGroupRepository.findProductGroupIsOptionIsOption ();
    }

    public double finCheapestOption () {
        return findOptionProductGroups().stream().mapToDouble(ProductGroup::getTotalSellingPrice).min().orElse(0.0);
    }
}
