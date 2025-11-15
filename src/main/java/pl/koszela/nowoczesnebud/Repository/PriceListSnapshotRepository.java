package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshot;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListSnapshotRepository extends JpaRepository<PriceListSnapshot, Long> {

    /**
     * Znajdź snapshot dla kategorii i daty (dokładna data)
     */
    @Query("SELECT s FROM PriceListSnapshot s WHERE s.category = :category AND s.snapshotDate = :date")
    Optional<PriceListSnapshot> findByCategoryAndDate(@Param("category") ProductCategory category, 
                                                       @Param("date") LocalDateTime date);

    /**
     * Znajdź najbliższy wcześniejszy snapshot dla kategorii i daty
     * (używane gdy projekt szuka snapshotu dla swojej daty utworzenia)
     */
    @Query("SELECT s FROM PriceListSnapshot s WHERE s.category = :category AND s.snapshotDate <= :date ORDER BY s.snapshotDate DESC")
    List<PriceListSnapshot> findClosestEarlierSnapshot(@Param("category") ProductCategory category, 
                                                         @Param("date") LocalDateTime date);

    /**
     * Znajdź wszystkie snapshoty dla kategorii (posortowane po dacie)
     */
    @Query("SELECT s FROM PriceListSnapshot s WHERE s.category = :category ORDER BY s.snapshotDate DESC")
    List<PriceListSnapshot> findByCategory(@Param("category") ProductCategory category);

    /**
     * Znajdź snapshot z załadowanymi pozycjami (items)
     */
    @Query("SELECT DISTINCT s FROM PriceListSnapshot s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<PriceListSnapshot> findByIdWithItems(@Param("id") Long id);
}














