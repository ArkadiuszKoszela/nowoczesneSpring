package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.Tiles;

import java.util.List;

@Transactional
@Repository
public interface TilesRepository extends JpaRepository<Tiles, Long> {

    List<Tiles> findByNameContainingIgnoreCase (String name);
}
