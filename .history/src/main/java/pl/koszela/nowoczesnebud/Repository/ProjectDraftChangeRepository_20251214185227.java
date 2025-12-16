package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProjectDraftChange;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDraftChangeRepository extends JpaRepository<ProjectDraftChange, Long> {
    
    List<ProjectDraftChange> findByProjectIdAndCategory(Long projectId, String category);
    
    List<ProjectDraftChange> findByProjectId(Long projectId);
    
    /**
     * Usuwa wszystkie draft changes dla projektu
     * Używa natywnego SQL DELETE dla lepszej wydajności przy dużej liczbie rekordów
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM project_draft_changes_ws WHERE project_id = :projectId", nativeQuery = true)
    void deleteByProjectId(@Param("projectId") Long projectId);
    
    /**
     * Usuwa wszystkie draft changes dla projektu i kategorii
     * Używa natywnego SQL DELETE, aby uniknąć problemów z batch delete, gdy rekordy już nie istnieją
     */
    @Modifying
    @Query("DELETE FROM ProjectDraftChange d WHERE d.projectId = :projectId AND d.category = :category")
    void deleteByProjectIdAndCategory(@Param("projectId") Long projectId, @Param("category") String category);
    
    Optional<ProjectDraftChange> findByProjectIdAndProductIdAndCategory(Long projectId, Long productId, String category);
    
}


