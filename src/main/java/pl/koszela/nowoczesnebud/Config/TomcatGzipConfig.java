package pl.koszela.nowoczesnebud.Config;

import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KONFIGURACJA GZIP DLA TOMCAT
 * 
 * Wymusza kompresję GZIP dla dużych JSON response (8775 produktów TILE = ~7.5MB)
 * Bez GZIP: 16 sekund transferu
 * Z GZIP: ~2-3 sekundy transferu (70-80% redukcja rozmiaru)
 * 
 * @see https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.enable-response-compression
 */
@Configuration
public class TomcatGzipConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TomcatGzipConfig.class);
    
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                
                // 🔥 WŁĄCZ GZIP (nawet dla localhost!)
                protocol.setCompression("on");
                
                // 📝 MIME types do kompresji
                protocol.setCompressibleMimeType(
                    "application/json," +
                    "application/xml," +
                    "text/html," +
                    "text/xml," +
                    "text/plain," +
                    "application/javascript," +
                    "text/css"
                );
                
                // 📏 Minimalny rozmiar do kompresji (1KB)
                protocol.setCompressionMinSize(1024);
                
                // ⚡ Poziom kompresji (1-9, domyślnie 6)
                // Niższy poziom = szybsza kompresja, ale większy rozmiar
                // Dla dużych JSON (7.5MB) lepiej użyć niższego poziomu (szybsza odpowiedź)
                // Poziom 1: ~10-11s kompresji → ~2-3s (bardzo szybkie, ale większy rozmiar ~15-20%)
                // Poziom 6 (domyślny): ~10-11s kompresji → ~2s (dobra równowaga)
                // Poziom 9: ~15-20s kompresji → ~1.5s (najlepszy rozmiar, ale wolniejsze)
                // ⚠️ UWAGA: Tomcat nie ma bezpośredniej metody setCompressionLevel()
                // Kompresja jest wykonywana przez GzipOutputStream z domyślnym poziomem (6)
                // Aby zmienić poziom, trzeba użyć custom Filter lub Valve
                
                // 🎯 User-Agent patterns (compress for ALL)
                protocol.setNoCompressionUserAgents(null);
                
                // ✅ Logi potwierdzenia
                logger.info("✅ GZIP Compression ENABLED:");
                logger.info("  - Compression: {}", protocol.getCompression());
                logger.info("  - Min Size: {} bytes", protocol.getCompressionMinSize());
                logger.info("  - MIME Types: {}", protocol.getCompressibleMimeType());
                logger.info("  - localhost: FORCE ENABLED (for dev)");
            });
        };
    }
}

