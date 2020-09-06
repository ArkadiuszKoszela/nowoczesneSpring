package pl.koszela.nowoczesnebud.Model;

public class TilesDTO {

    private String name;
    private String manufacturer;
    private double quantity;
    private double unitDetalPrice;
    private double quantityConverter;
    private double totalPriceAfterDiscount;
    private double totalPriceDetal;
    private double totalProfit;

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

    public double getTotalPriceAfterDiscount() {
        return totalPriceAfterDiscount;
    }

    public void setTotalPriceAfterDiscount(double totalPriceAfterDiscount) {
        this.totalPriceAfterDiscount = totalPriceAfterDiscount;
    }

    public double getTotalPriceDetal() {
        return totalPriceDetal;
    }

    public void setTotalPriceDetal(double totalPriceDetal) {
        this.totalPriceDetal = totalPriceDetal;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(double totalProfit) {
        this.totalProfit = totalProfit;
    }
}
