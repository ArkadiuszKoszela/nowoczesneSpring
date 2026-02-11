package pl.koszela.nowoczesnebud.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.koszela.nowoczesnebud.Model.AppUser;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class AuthMigrationConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuthMigrationConfig.class);

    private final AppUserRepository appUserRepository;

    public AuthMigrationConfig(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @PostConstruct
    public void migrateLegacyUsersEmailVerification() {
        List<AppUser> legacyUsers = appUserRepository.findByEmailVerifiedIsNull();
        if (legacyUsers.isEmpty()) {
            return;
        }
        legacyUsers.forEach(user -> user.setEmailVerified(true));
        appUserRepository.saveAll(legacyUsers);
        logger.info("Zaktualizowano {} istniejących kont: emailVerified=true (migracja zgodności).", legacyUsers.size());
    }
}
