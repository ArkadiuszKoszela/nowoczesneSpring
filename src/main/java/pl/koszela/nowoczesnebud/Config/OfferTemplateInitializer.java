package pl.koszela.nowoczesnebud.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.koszela.nowoczesnebud.Service.OfferTemplateService;

/**
 * Inicjalizator domyślnego szablonu oferty przy starcie aplikacji
 */
@Component
public class OfferTemplateInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OfferTemplateInitializer.class);
    
    private final OfferTemplateService templateService;

    public OfferTemplateInitializer(OfferTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🔧 Inicjalizacja domyślnego szablonu oferty...");
        try {
            templateService.createDefaultTemplateIfNotExists();
            logger.info("✅ Inicjalizacja szablonu zakończona");
        } catch (Exception e) {
            logger.error("❌ Błąd podczas inicjalizacji domyślnego szablonu", e);
        }
    }
}













