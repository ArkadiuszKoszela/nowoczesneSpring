package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.TilesInput;

@Repository
public interface TilesInputRepository extends JpaRepository<TilesInput, Long> {
}
