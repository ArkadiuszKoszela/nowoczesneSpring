package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProductGroup;
import pl.koszela.nowoczesnebud.Model.ProductType;

import java.util.List;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    @Query(value = "SELECT g.product_group_id from product_type g where id = :id", nativeQuery = true)
    long findIdGroupOfType(@Param("id") long id);

    @Query(value = "SELECT * from product_type g where g.product_group_id = :id", nativeQuery = true)
    List<ProductType> findProductsTypes (@Param("id") long id);
}
