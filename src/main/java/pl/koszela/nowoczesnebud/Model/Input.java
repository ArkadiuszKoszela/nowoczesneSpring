package pl.koszela.nowoczesnebud.Model;

import javax.persistence.*;

@Embeddable
public class Input {

    public Input(String name, double quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    private String name;
    private double quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}
