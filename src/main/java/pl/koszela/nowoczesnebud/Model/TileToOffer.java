package pl.koszela.nowoczesnebud.Model;

import javax.persistence.*;

@Entity
@Table(name = "TILE_TO_OFFER")
public class TileToOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String manufacturer;
    private double quantity;
    private double unitDetalPrice;
    private double quantityConverter;
    @ManyToOne
    private CommercialOffer commercialOffer;

    public TileToOffer() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getUnitDetalPrice() {
        return unitDetalPrice;
    }

    public void setUnitDetalPrice(double unitDetalPrice) {
        this.unitDetalPrice = unitDetalPrice;
    }

    public double getQuantityConverter() {
        return quantityConverter;
    }

    public void setQuantityConverter(double quantityConverter) {
        this.quantityConverter = quantityConverter;
    }

    public CommercialOffer getCommercialOffer() {
        return commercialOffer;
    }

    public void setCommercialOffer(CommercialOffer commercialOffer) {
        this.commercialOffer = commercialOffer;
    }
}
