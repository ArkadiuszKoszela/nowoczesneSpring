package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ProductGroup;

import java.util.List;

@Repository
public interface ProductGroupRepository extends JpaRepository<ProductGroup, Long> {

    @Query(value = "SELECT g.tile_id from product_group g where id = :id", nativeQuery = true)
    long findIdTile(@Param("id") long id);

    @Query(value = "SELECT g.gutter_id from product_group g where id = :id", nativeQuery = true)
    long findIdGutter(@Param("id") long id);

    @Query(value = "SELECT * from product_group where option_name is not null", nativeQuery = true)
    List<ProductGroup> findProductGroupIsOptionIsNotNull ();

    @Query(value = "SELECT * from product_group where option_name = 1", nativeQuery = true)
    ProductGroup findProductGroupIsOptionIsMain ();

    @Query(value = "SELECT * from product_group g where g.tile_id = :id", nativeQuery = true)
    List<ProductGroup> findProductsGroupForTile(@Param("id") long id);

    @Query(value = "SELECT * from product_group g where g.gutter_id = :id", nativeQuery = true)
    List<ProductGroup> findProductsGroupForGutter(@Param("id") long id);

}
