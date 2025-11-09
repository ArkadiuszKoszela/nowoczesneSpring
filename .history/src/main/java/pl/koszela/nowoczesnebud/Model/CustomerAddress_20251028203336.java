package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class CustomerAddress {
    
    private String address;
    private Double latitude;
    private Double longitude;
    private Double zoom;
}

