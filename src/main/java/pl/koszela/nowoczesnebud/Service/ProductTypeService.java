package pl.koszela.nowoczesnebud.Service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;
import pl.koszela.nowoczesnebud.Repository.ProductTypeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProductTypeService {

    private final ProductTypeRepository productTypeRepository;
    private final ProductGroupService productGroupService;

    public ProductTypeService(ProductTypeRepository productTypeRepository,
                              @Lazy ProductGroupService productGroupService) {
        this.productTypeRepository = productTypeRepository;
        this.productGroupService = productGroupService;
    }

    public List<ProductType> findProductTypesByProductGroupId (long id) {
        return productTypeRepository.findProductsTypes(id);
    }

    public ProductType getProductType(int value, ProductGroup productGroupToFind) {
        ProductGroup productGroup = productGroupService.findById(productGroupToFind.getId());
        if (value == 1)
            return productGroup.getProductTypeList().stream().filter(e -> e.getMapperName().equalsIgnoreCase(
                    "Powierzchnia polaci")).findFirst().orElse(null);
        else
            return productGroup.getProductTypeList().stream().filter(e -> !e.getMapperName().equalsIgnoreCase(
                    "Powierzchnia polaci")).findFirst().orElse(null);
    }

    public long findIdGroupOfType(long id) {
        return productTypeRepository.findIdGroupOfType(id);
    }

    public List<ProductType> saveAll (List<ProductType> toSave) {
        return productTypeRepository.saveAll(toSave);
    }

    public ProductType save (ProductType toSave) {
        return productTypeRepository.save(toSave);
    }

    public double calculatePurchasePrice(double price, ProductType productType) {
        double purchasePrice = price * calculatePercentage(productType.getBasicDiscount()) * calculatePercentage(
                productType.getAdditionalDiscount()) * calculatePercentage(
                productType.getPromotionDiscount()) * calculatePercentage(productType.getSkontoDiscount());
        return QuantityService.setScale(purchasePrice);
    }

    private double calculatePercentage(double value) {
        return (100 - value) / 100;
    }

    public double calculateDetalPrice(ProductType productType) {
        double unitPrice =
                productType.getPurchasePrice() * (100 + productType.getMarginUnitDetalPrice()) / 100;
        return QuantityService.setScale(unitPrice);
    }
}
