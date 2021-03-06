package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.Accessory;

@Repository
public interface AccessoriesRepository extends JpaRepository<Accessory, Long> {
}
