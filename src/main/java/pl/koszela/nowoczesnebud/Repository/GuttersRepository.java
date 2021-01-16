package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.Gutter;

import java.util.List;

@Repository
public interface GuttersRepository extends JpaRepository<Gutter, Long> {

    @Query(value = "SELECT id, basic_discount, promotion_discount, additional_discount, skonto_discount, manufacturer " +
            "FROM gutters", nativeQuery = true)
    List<Gutter> findDiscounts();
}
