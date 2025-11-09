package pl.koszela.nowoczesnebud.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Konfiguracja Thymeleaf do renderowania szablonów z stringów
 * Używa SpringTemplateEngine zamiast zwykłego TemplateEngine
 * aby korzystać z SpEL zamiast OGNL (lepsza kompatybilność z Spring Boot)
 */
@Configuration
public class ThymeleafConfig {

    @Bean(name = "stringTemplateEngine")
    @Primary
    public SpringTemplateEngine stringTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false); // Wyłącz cache dla szablonów z bazy danych
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}

