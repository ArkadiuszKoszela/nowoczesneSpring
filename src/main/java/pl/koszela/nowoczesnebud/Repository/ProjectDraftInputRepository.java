package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Używa natywnego SQL DELETE, aby uniknąć problemów z batch delete, gdy rekordy już nie istnieją
     */
    @Modifying
    @Query("DELETE FROM ProjectDraftInput d WHERE d.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}


