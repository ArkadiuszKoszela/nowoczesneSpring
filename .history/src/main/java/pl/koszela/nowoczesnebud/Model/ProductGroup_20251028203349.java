package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Grupa produktów dla wizualnego grupowania (opcjonalne)
 * Używane przy generowaniu ofert do grupowania produktów
 */
@Data
@Entity
@Table(name = "product_groups")
public class ProductGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    // Czy to główna opcja w ofercie
    private Boolean isMainOption;

    // Produkty w tej grupie
    @ManyToMany
    @JoinTable(
        name = "product_group_items",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products = new ArrayList<>();

    // Sumy dla grupy
    private Double totalRetailPrice = 0.0;
    private Double totalPurchasePrice = 0.0;
    private Double totalSellingPrice = 0.0;
    private Double totalProfit = 0.0;
}
