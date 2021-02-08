package pl.koszela.nowoczesnebud.Service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class QuantityService {

    private final TilesService tilesService;
    private final GuttersService guttersService;
    private final ProductTypeService productTypeService;
    private final ProductGroupService productGroupService;

    public QuantityService(@Lazy TilesService tilesService, @Lazy GuttersService guttersService,
                           @Lazy ProductTypeService productTypeService,
                           @Lazy ProductGroupService productGroupService) {
        this.tilesService = tilesService;
        this.guttersService = guttersService;
        this.productTypeService = productTypeService;
        this.productGroupService = productGroupService;
    }

    public List<Tile> filledQuantityInTiles(List<Input> inputList) {
        List<Tile> allTiles = tilesService.getAllTiles();
        for (Tile tile : allTiles) {
            for (ProductGroup productGroup : tile.getProductGroupList()) {
                for (ProductType productType : productGroup.getProductTypeList()) {
                    for (Input input : inputList) {
                        double quantityToSet = input.getQuantity();
                        String mapperValue = input.getMapperName();
                        String mapper = productType.getMapperName();
                        if (!mapperValue.equalsIgnoreCase(mapper))
                            continue;
                        productType = calculateProductType(productType, quantityToSet);
                    }
                }
                productGroup = calculateProductGroup(productGroup);
            }
        }
        return tilesService.saveAllTiles(allTiles);
    }

    public List<Gutter> filledQuantityInGutters(List<Input> inputList) {
        List<Gutter> allGutters = guttersService.getAllGutters();
        for (Gutter gutter : allGutters) {
            for (ProductGroup productGroup : gutter.getProductGroupList()) {
                for (ProductType productType : productGroup.getProductTypeList()) {
                    for (Input input : inputList) {
                        double quantityToSet = input.getQuantity();
                        String mapperValue = input.getMapperName();
                        String mapper = productType.getMapperName();
                        if (!mapperValue.equalsIgnoreCase(mapper))
                            continue;
                        productType = calculateProductType(productType, quantityToSet);
                    }
                }
                productGroup = calculateProductGroup(productGroup);
            }
        }
        return guttersService.saveAll(allGutters);
    }

    private ProductGroup calculateProductGroup(ProductGroup productGroup) {
        double sumPrice = productGroup.getProductTypeList().stream()
                .mapToDouble(productType -> productType.getPurchasePrice() * productType.getQuantity())
                .sum();
        double sumDetalPrice = productGroup.getProductTypeList().stream()
                .mapToDouble(productType -> productType.getDetalPrice() * productType.getQuantity())
                .sum();
        productGroup.setTotalPriceDetal(setScale(sumDetalPrice));
        productGroup.setTotalPriceAfterDiscount(setScale(sumPrice));
        productGroup.setTotalProfit(calculateTotalProfit(productGroup));
        return productGroup;
    }

    public DTO setQuantity(DTO dto) {
        productTypeService.save(dto.getProductType());
        ProductGroup productGroup = productGroupService.findById(dto.getProductGroup().getId());
        dto.getProductGroup().setProductTypeList(productGroup.getProductTypeList());
        List<ProductGroup> productGroupList = productGroupService.calculateMargin(null, null,
                Collections.singletonList(productGroup));
        dto.setProductGroup (productGroupList.get(0));
        dto.setProductGroup(calculateProductGroup(dto.getProductGroup()));
        productGroupService.save(dto.getProductGroup());
        return dto;
    }

    public double calculatePriceAfterDiscount(double price, int discount1, int discount2, int discount3,
                                              int discount4) {
        double priceAfterDiscount =
                price * calculatePercentage(discount1) * calculatePercentage(discount2) * calculatePercentage(
                        discount3) * calculatePercentage(discount4);
        return setScale(priceAfterDiscount);
    }

    double calculateTotalProfit(ProductGroup tile) {
        double totalProfit = tile.getTotalPriceDetal()
                - tile.getTotalPriceAfterDiscount();
        return setScale(totalProfit);
    }

    private ProductType calculateProductType(ProductType productType, double valueToSet) {
        double quantity = valueToSet * productType.getQuantityConverter();
        productType.setQuantity(setScale(quantity));
        double price = calculatePriceAfterDiscount(productType.getDetalPrice(), productType.getBasicDiscount(),
                productType.getAdditionalDiscount(),productType.getPromotionDiscount(), productType.getSkontoDiscount());
        productType.setPurchasePrice(price);
        return productType;
    }

    private double calculatePercentage(double value) {
        return (100 - value) / 100;
    }

    public static double setScale (double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

