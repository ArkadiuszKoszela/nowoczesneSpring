package pl.koszela.nowoczesnebud.Config;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Konfiguracja bazy danych dla Heroku
 * Parsuje DATABASE_URL z Heroku i ustawia właściwości Spring Boot
 * Format: postgres://user:password@host:port/database
 * lub: mysql://user:password@host:port/database
 */
public class HerokuDatabaseConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, 
                                      org.springframework.boot.SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                URI dbUri = new URI(databaseUrl);
                
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String host = dbUri.getHost();
                int port = dbUri.getPort();
                String path = dbUri.getPath();
                String database = path.startsWith("/") ? path.substring(1) : path;
                
                String scheme = dbUri.getScheme();
                String jdbcUrl;
                
                if ("postgres".equals(scheme) || "postgresql".equals(scheme)) {
                    jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
                } else if ("mysql".equals(scheme)) {
                    jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?rewriteBatchedStatements=true&UseUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false", 
                            host, port, database);
                } else {
                    throw new IllegalArgumentException("Nieobsługiwany typ bazy danych: " + scheme);
                }
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", jdbcUrl);
                properties.put("spring.datasource.username", username);
                properties.put("spring.datasource.password", password);
                
                environment.getPropertySources().addFirst(
                    new MapPropertySource("herokuDatabaseConfig", properties)
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Błąd parsowania DATABASE_URL: " + databaseUrl, e);
            }
        }
    }
}

