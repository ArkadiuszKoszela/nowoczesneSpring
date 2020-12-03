package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.GroupOfTiles;
import pl.koszela.nowoczesnebud.Model.TilesInput;

import java.util.List;

@Repository
public interface GroupOfTilesInputRepository extends JpaRepository<GroupOfTiles, Long> {
    GroupOfTiles findGroupOfTilesById (long id);
}
