package pl.koszela.nowoczesnebud.Security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.AppUser;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public UserDetailsServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(username)
                .or(() -> appUserRepository.findByUsernameIgnoreCase(username))
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika: " + username));

        return new User(
                appUser.getUsername(),
                appUser.getPassword(),
                appUser.getEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole()))
        );
    }
}
