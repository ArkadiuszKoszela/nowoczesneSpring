package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshotItem;

import java.util.List;

@Repository
public interface PriceListSnapshotItemRepository extends JpaRepository<PriceListSnapshotItem, Long> {

    /**
     * Znajdź wszystkie pozycje dla snapshotu
     */
    @Query("SELECT i FROM PriceListSnapshotItem i WHERE i.priceListSnapshot.id = :snapshotId")
    List<PriceListSnapshotItem> findBySnapshotId(@Param("snapshotId") Long snapshotId);
}














