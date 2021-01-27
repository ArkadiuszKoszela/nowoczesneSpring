package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class QuantityService {

    private final ProductTypeRepository productTypeRepository;
    private final ProductGroupRepository productGroupRepository;
    private final TilesService tilesService;
    private final GuttersService guttersService;

    public QuantityService(ProductTypeRepository productTypeRepository, ProductGroupRepository productGroupRepository, TilesService tilesService, GuttersService guttersService) {
        this.productTypeRepository = productTypeRepository;
        this.productGroupRepository = productGroupRepository;
        this.tilesService = tilesService;
        this.guttersService = guttersService;
    }

    public List<Tile> filledQuantityInTiles(List<Input> inputList) {
        List<ProductType> productTypeList = productTypeRepository.findAll();
        List<ProductType> productTypeOfTilesToUpdate = new ArrayList<>();
        for (ProductType productType : productTypeList) {
                for (Input input : inputList) {
                    double quantityToSet = input.getQuantity();
                    String mapperValue = input.getMapperName();
                    String mapper = productType.getMapperName();
                    if (mapperValue.equalsIgnoreCase(mapper))
                        calculateQuantity (productType, quantityToSet);
                    productTypeOfTilesToUpdate.add (productType);
            }
        }
        productTypeRepository.saveAll(productTypeOfTilesToUpdate);

        List<Tile> allTiles = tilesService.getAllTiles ();

        for (Tile tile : allTiles) {
            for (ProductGroup productGroup : tile.getProductGroupList()) {
                double sumPrice = productGroup.getProductTypeList().stream().mapToDouble(
                        productType -> productType.getPrice() * productType.getQuantity()).sum();
                double sumDetalPrice = productGroup.getProductTypeList().stream().mapToDouble(
                        productType -> BigDecimal.valueOf(productType.getUnitDetalPrice().doubleValue() * productType.getQuantity()).setScale(2, RoundingMode.HALF_UP).doubleValue()).sum();
                productGroup.setTotalPriceDetal(BigDecimal.valueOf(sumDetalPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
                productGroup.setTotalPriceAfterDiscount(sumPrice);
                productGroup.setTotalProfit(calculateTotalProfit(productGroup));
            }
        }
        return tilesService.saveAllTiles(allTiles);
    }

    public List<Gutter> filledQuantityInGutters(List<Input> inputList) {
        List<ProductType> productTypeList = productTypeRepository.findAll();
        List<ProductType> productTypeOfTilesToUpdate = new ArrayList<>();
        for (ProductType productType : productTypeList) {
            for (Input input : inputList) {
                double quantityToSet = input.getQuantity();
                String mapperValue = input.getMapperName();
                String mapper = productType.getMapperName();
                if (mapperValue.equalsIgnoreCase(mapper))
                    calculateQuantity (productType, quantityToSet);
                productTypeOfTilesToUpdate.add (productType);
            }
        }
        productTypeRepository.saveAll(productTypeOfTilesToUpdate);

        List<Gutter> allGutters = guttersService.getAllGutters();

        for (Gutter gutter : allGutters) {
            for (ProductGroup productGroup : gutter.getProductGroupList()) {
                double sumPrice = productGroup.getProductTypeList().stream().mapToDouble(
                        productType -> productType.getPrice() * productType.getQuantity()).sum();
                double sumDetalPrice = productGroup.getProductTypeList().stream().mapToDouble(
                        productType -> BigDecimal.valueOf(productType.getUnitDetalPrice().doubleValue() * productType.getQuantity()).setScale(2, RoundingMode.HALF_UP).doubleValue()).sum();
                productGroup.setTotalPriceDetal(BigDecimal.valueOf(sumDetalPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
                productGroup.setTotalPriceAfterDiscount(sumPrice);
                productGroup.setTotalProfit(calculateTotalProfit(productGroup));
            }
        }

        return guttersService.saveAll(allGutters);
    }

    public DTO setQuantity (DTO dto, Tile tile) {
        BigDecimal price = calculatePriceAfterDiscount (dto.getProductType().getUnitDetalPrice(),
                tile.getBasicDiscount(),
                tile.getAdditionalDiscount(), tile.getPromotionDiscount(), tile.getSkontoDiscount());
        dto.getProductType().setPrice(price.doubleValue());

        productTypeRepository.save(dto.getProductType());
        Optional<ProductGroup> productGroup = productGroupRepository.findById(dto.getProductGroup().getId());
        if (!productGroup.isPresent())
            return null;
        dto.getProductGroup().setProductTypeList(productGroup.get().getProductTypeList());

        double sumPrice = dto.getProductGroup().getProductTypeList().stream().mapToDouble(
                productType -> productType.getPrice() * productType.getQuantity()).sum();
        double sumDetalPrice = dto.getProductGroup().getProductTypeList().stream().mapToDouble(
                productType -> BigDecimal.valueOf(productType.getUnitDetalPrice().doubleValue() * productType.getQuantity()).setScale(2, RoundingMode.HALF_UP).doubleValue()).sum();
        dto.getProductGroup().setTotalPriceDetal(BigDecimal.valueOf(sumDetalPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
        dto.getProductGroup().setTotalPriceAfterDiscount(sumPrice);
        dto.getProductGroup().setTotalProfit(calculateTotalProfit(dto.getProductGroup()));

        productGroupRepository.save(dto.getProductGroup());
        return dto;
    }

    public DTO setQuantity (DTO dto, Gutter gutter) {
        BigDecimal price = calculatePriceAfterDiscount (dto.getProductType().getUnitDetalPrice(),
                gutter.getBasicDiscount(),
                gutter.getAdditionalDiscount(), gutter.getPromotionDiscount(), gutter.getSkontoDiscount());
        dto.getProductType().setPrice(price.doubleValue());

        productTypeRepository.save(dto.getProductType());
        Optional<ProductGroup> productGroup = productGroupRepository.findById(dto.getProductGroup().getId());
        if (!productGroup.isPresent())
            return null;
        dto.getProductGroup().setProductTypeList(productGroup.get().getProductTypeList());

        double sumPrice = dto.getProductGroup().getProductTypeList().stream().mapToDouble(
                productType -> productType.getPrice() * productType.getQuantity()).sum();
        dto.getProductGroup().setTotalPriceDetal(BigDecimal.valueOf(sumPrice).setScale(2, RoundingMode.HALF_UP).doubleValue());
        dto.getProductGroup().setTotalPriceAfterDiscount(sumPrice);
        dto.getProductGroup().setTotalProfit(calculateTotalProfit(dto.getProductGroup()));

        productGroupRepository.save(dto.getProductGroup());
        return dto;
    }

    public BigDecimal calculatePriceAfterDiscount(BigDecimal price, int discount1, int discount2, int discount3, int discount4) {
        int sumDiscounts = discount1 + discount2 + discount3 + discount4;
        return price.multiply(BigDecimal.valueOf(convertPercents(sumDiscounts))).setScale(2, RoundingMode.HALF_UP);
    }

    private double calculateTotalProfit(ProductGroup tile) {
        return BigDecimal.valueOf(tile.getTotalPriceDetal()
                - tile.getTotalPriceAfterDiscount()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double convertPercents(double value) {
        return (100 - value) / 100;
    }

    private void calculateQuantity(ProductType productType, double valueToSet) {
        double quantity = BigDecimal.valueOf(valueToSet)
                .multiply(productType.getQuantityConverter()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        productType.setQuantity(quantity);
        double price = BigDecimal.valueOf(quantity).multiply(productType.getUnitDetalPrice()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        productType.setPrice(price);
    }
}

