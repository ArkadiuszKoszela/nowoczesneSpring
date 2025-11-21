package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.ProjectProduct;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectProductRepository extends JpaRepository<ProjectProduct, Long> {
    
    /**
     * Znajdź wszystkie produkty projektu dla danej kategorii
     * @param projectId ID projektu
     * @param category Kategoria (TILE, GUTTER, ACCESSORY)
     * @return Lista produktów projektu
     */
    List<ProjectProduct> findByProjectIdAndCategory(Long projectId, ProductCategory category);
    
    /**
     * Znajdź wszystkie produkty projektu
     * @param projectId ID projektu
     * @return Lista wszystkich produktów projektu
     */
    List<ProjectProduct> findByProjectId(Long projectId);
    
    /**
     * Znajdź produkt projektu po ID produktu z cennika
     * @param projectId ID projektu
     * @param productId ID produktu z tabeli Product
     * @return Opcjonalny produkt projektu
     */
    Optional<ProjectProduct> findByProjectIdAndProductId(Long projectId, Long productId);
    
    /**
     * Usuń wszystkie produkty projektu
     * @param projectId ID projektu
     */
    void deleteByProjectId(Long projectId);
}

