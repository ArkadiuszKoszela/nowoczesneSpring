package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.GoogleCalendarConnection;

import java.util.Optional;

@Repository
public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, Long> {
    Optional<GoogleCalendarConnection> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);

    @Modifying
    @Query("delete from GoogleCalendarConnection c where c.user.id = :userId")
    int deleteConnectionByUserId(@Param("userId") Long userId);
}

