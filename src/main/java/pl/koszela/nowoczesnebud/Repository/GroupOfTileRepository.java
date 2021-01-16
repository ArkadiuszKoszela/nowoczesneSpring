package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.GroupOfTile;
import pl.koszela.nowoczesnebud.Model.Tile;
import pl.koszela.nowoczesnebud.Model.TypeOfTile;

import java.util.List;

@Repository
public interface GroupOfTileRepository extends JpaRepository<GroupOfTile, Long> {

    @Query(value = "SELECT g.tile_id from group_of_tile g where id = :id", nativeQuery = true)
    long findIdTile(@Param("id") long id);
}
