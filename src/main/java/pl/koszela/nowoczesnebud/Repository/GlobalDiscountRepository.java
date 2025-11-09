package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount;
import pl.koszela.nowoczesnebud.Model.GlobalDiscount.DiscountType;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalDiscountRepository extends JpaRepository<GlobalDiscount, Long> {

    /**
     * Znajdź wszystkie rabaty dla danej kategorii
     */
    List<GlobalDiscount> findByCategory(ProductCategory category);

    /**
     * Znajdź aktywne rabaty dla danej kategorii
     */
    List<GlobalDiscount> findByCategoryAndActiveTrue(ProductCategory category);

    /**
     * Znajdź rabat dla kategorii i typu
     */
    Optional<GlobalDiscount> findByCategoryAndType(ProductCategory category, DiscountType type);

    /**
     * Znajdź aktywny rabat dla kategorii i typu
     */
    Optional<GlobalDiscount> findByCategoryAndTypeAndActiveTrue(ProductCategory category, DiscountType type);

    /**
     * Znajdź aktualnie ważne rabaty (aktywne + w okresie ważności)
     */
    @Query("SELECT g FROM GlobalDiscount g WHERE g.category = :category " +
           "AND g.active = true " +
           "AND :currentDate BETWEEN g.validFrom AND g.validTo")
    List<GlobalDiscount> findCurrentlyValidDiscounts(
            @Param("category") ProductCategory category,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Znajdź aktualnie ważny rabat główny
     */
    @Query("SELECT g FROM GlobalDiscount g WHERE g.category = :category " +
           "AND g.type = 'MAIN' " +
           "AND g.active = true " +
           "AND :currentDate BETWEEN g.validFrom AND g.validTo")
    Optional<GlobalDiscount> findCurrentMainDiscount(
            @Param("category") ProductCategory category,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Znajdź aktualnie ważny rabat opcjonalny
     */
    @Query("SELECT g FROM GlobalDiscount g WHERE g.category = :category " +
           "AND g.type = 'OPTIONAL' " +
           "AND g.active = true " +
           "AND :currentDate BETWEEN g.validFrom AND g.validTo")
    Optional<GlobalDiscount> findCurrentOptionalDiscount(
            @Param("category") ProductCategory category,
            @Param("currentDate") LocalDate currentDate);
}














