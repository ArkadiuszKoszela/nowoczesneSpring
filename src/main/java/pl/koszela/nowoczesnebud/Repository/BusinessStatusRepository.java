package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.BusinessStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessStatusRepository extends JpaRepository<BusinessStatus, Long> {
    List<BusinessStatus> findAllByOrderBySortOrderAscIdAsc();
    List<BusinessStatus> findByActiveTrueOrderBySortOrderAscIdAsc();
    Optional<BusinessStatus> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    Optional<BusinessStatus> findTopByOrderBySortOrderDescIdDesc();
}

