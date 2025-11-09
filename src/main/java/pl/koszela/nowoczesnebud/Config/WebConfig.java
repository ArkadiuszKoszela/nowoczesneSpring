package pl.koszela.nowoczesnebud.Config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Globalna konfiguracja CORS - zastępuje duplikowane @CrossOrigin w kontrolerach
 * + konfiguracja Jackson dla lepszej obsługi deserializacji
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:4200",
                    "https://angular-nowoczesne-af04d5c56981.herokuapp.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight requests for 1 hour
    }

    /**
     * Konfiguracja Jackson ObjectMapper dla lepszej obsługi deserializacji
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        // Ignoruj nieznane właściwości (nie rzucaj błędu)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Pozwól na deserializację null do prymitywów (ustawi na 0 lub false)
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // Ignoruj nieprawidłowe typy (np. string zamiast liczby) - lepiej użyć custom deserializera
        objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        return objectMapper;
    }
}








