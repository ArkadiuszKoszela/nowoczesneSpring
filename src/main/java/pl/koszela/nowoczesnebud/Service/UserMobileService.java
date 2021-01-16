package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.UserMobile;
import pl.koszela.nowoczesnebud.Repository.UserMobileRepository;

import java.util.Optional;

@Service
public class UserMobileService {

    private final UserMobileRepository userMobileRepository;

    public UserMobileService(UserMobileRepository userMobileRepository) {
        this.userMobileRepository = userMobileRepository;
    }

    public boolean getUser(UserMobile userMobile) {
        Optional<UserMobile> optionalUserMobile = userMobileRepository
                .findByUsernameAndPassword(userMobile.getUsername(), userMobile.getPassword());
        return optionalUserMobile.isPresent();
    }

    public UserMobile save (UserMobile userMobile) {
        return userMobileRepository.save(userMobile);
    }
}
