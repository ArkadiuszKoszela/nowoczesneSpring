package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Atrybuty dla GRUPY produktowej (manufacturer + groupName)
 * Zamiast duplikować atrybuty dla każdego produktu w grupie (50-200 produktów),
 * przechowujemy je raz dla całej grupy.
 * 
 * Przykład:
 * - category: TILE
 * - manufacturer: CANTUS
 * - groupName: Nuance
 * - attributes: {"kolor":["czerwony","brązowy"],"kształt":["płaska"],"materiał":["beton"]}
 */
@Data
@Entity
@Table(name = "product_group_attributes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"category", "manufacturer", "group_name"}))
public class ProductGroupAttributes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kategoria produktu (TILE, GUTTER, ACCESSORY)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    /**
     * Producent (np. CANTUS, CREATON)
     */
    @Column(nullable = false)
    private String manufacturer;

    /**
     * Nazwa grupy produktowej (np. Nuance, Balance)
     */
    @Column(name = "group_name", nullable = false)
    private String groupName;

    /**
     * Atrybuty grupy produktowej w formacie JSON
     * Przykład: {"kolor":["czerwony","brązowy"],"kształt":["płaska"],"materiał":["beton"]}
     * - Klucz: nazwa atrybutu (np. "kolor", "kształt", "materiał", "powłoka")
     * - Wartość: tablica wartości (grupa może mieć wiele wartości tego samego atrybutu)
     */
    @Column(name = "attributes", length = 4000)
    private String attributes;

    /**
     * Data utworzenia
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data ostatniej aktualizacji
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

