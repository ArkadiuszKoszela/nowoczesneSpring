package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.Tile;

import java.util.Collection;
import java.util.List;

@Repository
public interface TileRepository extends JpaRepository<Tile, Long> {

    @Query (value = "SELECT distinct t.manufacturer FROM tiles t", nativeQuery = true)
    List<String> findManufacturers();

    @Query (value = "SELECT id, basic_discount, promotion_discount, additional_discount, skonto_discount, manufacturer " +
            "FROM tile", nativeQuery = true)
    List<Tile> findDiscounts();

    @Query (value = "SELECT * from tile where  " +
            "FROM tile", nativeQuery = true)
    Tile findTileByGroupId (@Param("id") long id);
}
