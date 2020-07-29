package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.Accessories;

@Transactional
@Repository
public interface AccessoriesRepository extends JpaRepository<Accessories, Long> {
}
