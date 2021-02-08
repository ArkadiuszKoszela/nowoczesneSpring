package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Gutter {

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    private String manufacturer;

    @OneToMany(targetEntity = ProductGroup.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "gutter_id")
    @JsonIgnore
    List<ProductGroup> productGroupList = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public List<ProductGroup> getProductGroupList() {
        return productGroupList;
    }

    public void setProductGroupList(List<ProductGroup> productGroupList) {
        this.productGroupList = productGroupList;
    }
}
