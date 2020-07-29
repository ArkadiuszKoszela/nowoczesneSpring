package pl.koszela.nowoczesnebud.Model;

import org.springframework.beans.factory.annotation.Value;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Embeddable
public class TotalValues {

    @Value("0.0")
    private Double totalPriceAfterDiscount;
    @Value("0.0")
    private Double totalPricePurchase;
    @Value("0.0")
    private Double totalProfit;

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
