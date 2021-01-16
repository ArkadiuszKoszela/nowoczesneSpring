package pl.koszela.nowoczesnebud.Model;

import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Table(name = "TILE_TO_OFFER")
public class TileToOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String manufacturer;
    private double unitDetalPrice;
    private double quantityConverter;
    private double totalPriceAfterDiscount;
    private double totalPriceDetal;
    private double totalProfit;
    @ManyToOne
    private CommercialOffer commercialOffer;
}
