package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ClientStatusHistory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientStatusHistoryRepository extends JpaRepository<ClientStatusHistory, Long> {
    Optional<ClientStatusHistory> findTopByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);
    Optional<ClientStatusHistory> findTopByProjectIdAndToStatusIsNotNullOrderByCreatedAtDescIdDesc(Long projectId);
    List<ClientStatusHistory> findByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);
    boolean existsByToStatusId(Long statusId);
}

