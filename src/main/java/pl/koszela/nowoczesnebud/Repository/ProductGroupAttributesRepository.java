package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.ProductGroupAttributes;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductGroupAttributesRepository extends JpaRepository<ProductGroupAttributes, Long> {

    /**
     * Znajdź atrybuty dla konkretnej grupy produktowej
     */
    Optional<ProductGroupAttributes> findByCategoryAndManufacturerAndGroupName(
            ProductCategory category,
            String manufacturer,
            String groupName
    );

    /**
     * Znajdź wszystkie atrybuty dla danej kategorii (do słownika sugestii)
     */
    List<ProductGroupAttributes> findByCategory(ProductCategory category);

    /**
     * Usuń atrybuty dla grupy produktowej
     */
    void deleteByCategoryAndManufacturerAndGroupName(
            ProductCategory category,
            String manufacturer,
            String groupName
    );
}

