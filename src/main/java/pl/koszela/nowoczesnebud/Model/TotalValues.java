package pl.koszela.nowoczesnebud.Model;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Embeddable
public class BaseEntity {

    private Double unitPurchasePrice = 0.0;
    private Double totalPriceAfterDiscount = 0.0;
    private Double totalPricePurchase = 0.0;
    private Double totalProfit = 0.0;

    public Double getUnitPurchasePrice() {
        return unitPurchasePrice;
    }

    public void setUnitPurchasePrice(Double unitPurchasePrice) {
        this.unitPurchasePrice = unitPurchasePrice;
    }

    public Double getTotalPriceAfterDiscount() {
        return totalPriceAfterDiscount;
    }

    public void setTotalPriceAfterDiscount(Double totalPriceAfterDiscount) {
        this.totalPriceAfterDiscount = totalPriceAfterDiscount;
    }

    public Double getTotalPricePurchase() {
        return totalPricePurchase;
    }

    public void setTotalPricePurchase(Double totalPricePurchase) {
        this.totalPricePurchase = totalPricePurchase;
    }

    public Double getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(Double totalProfit) {
        this.totalProfit = totalProfit;
    }
}
