package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.koszela.nowoczesnebud.Model.ClientCalendarEventLink;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientCalendarEventLinkRepository extends JpaRepository<ClientCalendarEventLink, Long> {
    Optional<ClientCalendarEventLink> findByUserIdAndGoogleEventId(Long userId, String googleEventId);
    List<ClientCalendarEventLink> findByUserIdAndClientId(Long userId, Long clientId);
}

