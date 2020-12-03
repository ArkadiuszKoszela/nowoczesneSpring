package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

@Data
public class TilesDTO {

    private String name;
    private String manufacturer;
    private double quantity;
    private double unitDetalPrice;
    private double quantityConverter;
    private double totalPriceAfterDiscount;
    private double totalPriceDetal;
    private double totalProfit;
}
