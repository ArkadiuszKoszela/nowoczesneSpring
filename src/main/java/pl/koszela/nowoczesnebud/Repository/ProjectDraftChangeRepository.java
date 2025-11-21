package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProjectDraftChange;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDraftChangeRepository extends JpaRepository<ProjectDraftChange, Long> {
    
    List<ProjectDraftChange> findByProjectIdAndCategory(Long projectId, String category);
    
    List<ProjectDraftChange> findByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
    
    Optional<ProjectDraftChange> findByProjectIdAndProductIdAndCategory(Long projectId, Long productId, String category);
}


