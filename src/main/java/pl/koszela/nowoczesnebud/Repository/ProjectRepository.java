package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    /**
     * Znajdź wszystkie projekty z załadowanymi relacjami
     * JOIN FETCH zapewnia że client jest załadowany (unika lazy proxy)
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN FETCH p.client ORDER BY p.createdAt DESC")
    List<Project> findAllWithClient();
    
    /**
     * Znajdź projekt dla danego klienta (OneToOne - jeden klient ma jeden projekt)
     * JOIN FETCH zapewnia że client jest załadowany (unika lazy proxy)
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN FETCH p.client WHERE p.client.id = :clientId")
    Optional<Project> findByClientId(@Param("clientId") Long clientId);
    
    /**
     * Znajdź projekt po ID z załadowanym klientem
     * JOIN FETCH zapewnia że client jest załadowany (unika lazy proxy)
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN FETCH p.client WHERE p.id = :id")
    Optional<Project> findByIdWithClient(@Param("id") Long id);
    
    /**
     * Znajdź projekt po ID z załadowanym klientem i inputami
     * JOIN FETCH zapewnia że client i inputs są załadowane (unika lazy proxy)
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN FETCH p.client LEFT JOIN FETCH p.inputs WHERE p.id = :id")
    Optional<Project> findByIdWithClientAndInputs(@Param("id") Long id);
    
    // TODO: Metoda existsBySnapshotDate została usunięta - snapshotDate nie istnieje w nowym modelu
}

