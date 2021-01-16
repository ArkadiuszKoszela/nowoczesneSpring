package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import java.util.List;

@Data
public class TilesDTO {

    private String manufacturer;
    private String typeOfTileName;
    private List<TypeOfTile> typeOfTileList;
    private double quantity;
    private double unitDetalPrice;
    private double quantityConverter;
    private double totalPriceAfterDiscount;
    private double totalPriceDetal;
    private double totalProfit;
}
