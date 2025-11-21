package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProjectDraftInput;

import java.util.List;

@Repository
public interface ProjectDraftInputRepository extends JpaRepository<ProjectDraftInput, Long> {
    
    /**
     * Znajduje wszystkie draft inputs dla projektu
     */
    List<ProjectDraftInput> findByProjectId(Long projectId);
    
    /**
     * Usuwa wszystkie draft inputs dla projektu
     */
    void deleteByProjectId(Long projectId);
}


