package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.poiji.annotation.ExcelCellName;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Tile {

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    private String manufacturer;

    @OneToMany(targetEntity = ProductGroup.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "tile_id")
    @JsonIgnore
    private List<ProductGroup> productGroupList = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setProductGroupList(List<ProductGroup> productGroupList) {
        this.productGroupList = productGroupList;
    }

    public void setCreateDateTime(LocalDateTime createDateTime) {
        this.createDateTime = createDateTime;
    }

    public void setUpdateDateTime(LocalDateTime updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    public long getId() {
        return id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public List<ProductGroup> getProductGroupList() {
        return productGroupList;
    }

    public LocalDateTime getCreateDateTime() {
        return createDateTime;
    }

    public LocalDateTime getUpdateDateTime() {
        return updateDateTime;
    }
}
