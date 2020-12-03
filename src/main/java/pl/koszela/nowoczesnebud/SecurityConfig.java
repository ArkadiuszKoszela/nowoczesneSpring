package pl.koszela.nowoczesnebud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import pl.koszela.nowoczesnebud.Repository.UserMobileRepository;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserMobileRepository userMobileRepository;


}
