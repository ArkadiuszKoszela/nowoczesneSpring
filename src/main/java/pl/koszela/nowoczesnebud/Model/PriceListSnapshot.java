package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model PriceListSnapshot - snapshot cennika dla określonej daty i kategorii
 * Snapshoty tworzą się przy imporcie cennika lub ręcznie przez admina
 * Wiele projektów może korzystać z tego samego snapshotu
 */
@Data
@Entity
@Table(name = "price_list_snapshots")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PriceListSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Data snapshotu (data dla której snapshot jest ważny)
     * Projekty używają snapshotu z daty utworzenia projektu
     */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;

    /**
     * Kategoria produktów (TILE, GUTTER, ACCESSORY)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    /**
     * RELACJA: Jeden snapshot → Wiele pozycji
     */
    @OneToMany(mappedBy = "priceListSnapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"priceListSnapshot", "hibernateLazyInitializer", "handler"})
    private List<PriceListSnapshotItem> items = new ArrayList<>();

    /**
     * Data utworzenia snapshotu w bazie
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}














