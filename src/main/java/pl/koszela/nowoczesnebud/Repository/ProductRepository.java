package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.Product;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Znajdź wszystkie produkty danej kategorii posortowane po displayOrder
     * Sortowanie: najpierw po manufacturer, potem po groupName, na końcu po displayOrder
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category ORDER BY p.manufacturer, p.groupName, p.displayOrder")
    List<Product> findByCategory(@Param("category") ProductCategory category);

    List<Product> findByCategoryAndManufacturer(ProductCategory category, String manufacturer);

    List<Product> findByManufacturer(String manufacturer);

    @Query("SELECT DISTINCT p.manufacturer FROM Product p WHERE p.category = :category")
    List<String> findDistinctManufacturersByCategory(@Param("category") ProductCategory category);

    List<Product> findByMapperName(String mapperName);
    
    @Query("SELECT DISTINCT p.groupName FROM Product p WHERE p.category = :category AND p.manufacturer = :manufacturer")
    List<String> findDistinctGroupNamesByCategoryAndManufacturer(@Param("category") ProductCategory category, @Param("manufacturer") String manufacturer);
    
    /**
     * Znajdź wszystkie produkty danej kategorii posortowane po displayOrder
     * Sortowanie: najpierw po manufacturer, potem po groupName, na końcu po displayOrder
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category ORDER BY p.manufacturer, p.groupName, p.displayOrder")
    List<Product> findByCategoryOrderByDisplayOrder(@Param("category") ProductCategory category);
}































