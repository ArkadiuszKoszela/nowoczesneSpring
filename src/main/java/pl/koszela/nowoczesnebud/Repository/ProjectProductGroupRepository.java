package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.ProjectProductGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectProductGroupRepository extends JpaRepository<ProjectProductGroup, Long> {
    
    /**
     * Znajdź wszystkie grupy produktowe projektu dla danej kategorii
     * @param projectId ID projektu
     * @param category Kategoria (TILE, GUTTER, ACCESSORY)
     * @return Lista grup produktowych projektu
     */
    List<ProjectProductGroup> findByProjectIdAndCategory(Long projectId, ProductCategory category);
    
    /**
     * Znajdź wszystkie grupy produktowe projektu
     * @param projectId ID projektu
     * @return Lista wszystkich grup produktowych projektu
     */
    List<ProjectProductGroup> findByProjectId(Long projectId);
    
    /**
     * Znajdź grupę produktową po producenta, nazwie grupy i kategorii
     * @param projectId ID projektu
     * @param category Kategoria (TILE, GUTTER, ACCESSORY)
     * @param manufacturer Producent (np. "CANTUS")
     * @param groupName Nazwa grupy (np. "czarna ang NUANE")
     * @return Opcjonalna grupa produktowa
     */
    Optional<ProjectProductGroup> findByProjectIdAndCategoryAndManufacturerAndGroupName(
        Long projectId, 
        ProductCategory category, 
        String manufacturer, 
        String groupName
    );
    
    /**
     * Usuń wszystkie grupy produktowe projektu
     * @param projectId ID projektu
     */
    void deleteByProjectId(Long projectId);
}

