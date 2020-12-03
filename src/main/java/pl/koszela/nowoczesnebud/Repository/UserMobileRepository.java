package pl.koszela.nowoczesnebud.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.koszela.nowoczesnebud.Model.UserMobile;

import java.util.Optional;

public interface UserMobileRepository extends JpaRepository<UserMobile, Long> {

    Optional<UserMobile> findByUsername (String username);
}
