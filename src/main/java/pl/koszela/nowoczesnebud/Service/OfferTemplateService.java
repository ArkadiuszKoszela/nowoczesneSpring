package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import pl.koszela.nowoczesnebud.Model.OfferTemplate;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Repository.OfferTemplateRepository;

import java.util.List;
import java.util.Optional;

/**
 * Serwis do zarządzania szablonami ofert
 */
@Service
public class OfferTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(OfferTemplateService.class);
    
    private final OfferTemplateRepository templateRepository;
    private final SpringTemplateEngine templateEngine;

    public OfferTemplateService(OfferTemplateRepository templateRepository,
                               @Qualifier("stringTemplateEngine") SpringTemplateEngine templateEngine) {
        this.templateRepository = templateRepository;
        this.templateEngine = templateEngine;
    }

    /**
     * Pobierz wszystkie szablony
     */
    public List<OfferTemplate> getAllTemplates() {
        logger.info("Pobieranie wszystkich szablonów");
        return templateRepository.findAll();
    }

    /**
     * Pobierz szablon po ID
     */
    public Optional<OfferTemplate> getTemplateById(Long id) {
        logger.info("Pobieranie szablonu ID: {}", id);
        return templateRepository.findById(id);
    }

    /**
     * Pobierz domyślny szablon
     * ⚠️ WAŻNE: Jeśli jest więcej niż jeden domyślny szablon, zwraca pierwszy
     */
    public Optional<OfferTemplate> getDefaultTemplate() {
        logger.info("Pobieranie domyślnego szablonu");
        // ⚠️ WAŻNE: Używamy findAll() i filtrujemy, bo findByIsDefaultTrue() rzuca wyjątek gdy jest więcej niż 1 domyślny szablon
        List<OfferTemplate> defaultTemplates = templateRepository.findAll().stream()
                .filter(t -> t.getIsDefault() != null && t.getIsDefault())
                .collect(java.util.stream.Collectors.toList());
        
        if (defaultTemplates.isEmpty()) {
            return Optional.empty();
        } else if (defaultTemplates.size() > 1) {
            logger.warn("⚠️ Znaleziono {} domyślnych szablonów (powinien być tylko 1)! Zwracam pierwszy.", defaultTemplates.size());
        }
        
        return Optional.of(defaultTemplates.get(0));
    }

    /**
     * Zapisz szablon (tworzy nowy lub aktualizuje istniejący)
     */
    @Transactional
    public OfferTemplate saveTemplate(OfferTemplate template) {
        logger.info("Zapisywanie szablonu: {}", template.getName());
        
        // Jeśli ustawiamy jako domyślny, usuń domyślny status z innych szablonów
        if (template.getIsDefault() != null && template.getIsDefault()) {
            // ⚠️ WAŻNE: Używamy findAll() i filtrujemy, bo findByIsDefaultTrue() rzuca wyjątek gdy jest więcej niż 1 domyślny szablon
            List<OfferTemplate> defaultTemplates = templateRepository.findAll().stream()
                    .filter(t -> t.getIsDefault() != null && t.getIsDefault())
                    .filter(t -> template.getId() == null || !t.getId().equals(template.getId())) // Pomiń aktualnie zapisywany szablon
                    .collect(java.util.stream.Collectors.toList());
            
            for (OfferTemplate defaultTemplate : defaultTemplates) {
                defaultTemplate.setIsDefault(false);
                templateRepository.save(defaultTemplate);
                logger.info("Usunięto domyślny status z szablonu ID: {}", defaultTemplate.getId());
            }
        }
        
        OfferTemplate saved = templateRepository.save(template);
        logger.info("Szablon zapisany: ID={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Usuń szablon
     */
    @Transactional
    public void deleteTemplate(Long id) {
        logger.info("Usuwanie szablonu ID: {}", id);
        
        Optional<OfferTemplate> templateOpt = templateRepository.findById(id);
        if (templateOpt.isPresent()) {
            OfferTemplate template = templateOpt.get();
            
            // Nie pozwól usunąć domyślnego szablonu
            if (template.getIsDefault() != null && template.getIsDefault()) {
                throw new IllegalStateException("Nie można usunąć domyślnego szablonu. Najpierw ustaw inny szablon jako domyślny.");
            }
            
            templateRepository.deleteById(id);
            logger.info("Szablon usunięty: ID={}", id);
        } else {
            throw new IllegalArgumentException("Szablon o ID " + id + " nie istnieje");
        }
    }

    /**
     * Ustaw szablon jako domyślny
     */
    @Transactional
    public OfferTemplate setDefaultTemplate(Long id) {
        logger.info("Ustawianie szablonu ID {} jako domyślnego", id);
        
        Optional<OfferTemplate> templateOpt = templateRepository.findById(id);
        if (templateOpt.isEmpty()) {
            throw new IllegalArgumentException("Szablon o ID " + id + " nie istnieje");
        }
        
        // Usuń domyślny status z innych szablonów
        // ⚠️ WAŻNE: Używamy findAll() i filtrujemy, bo findByIsDefaultTrue() rzuca wyjątek gdy jest więcej niż 1 domyślny szablon
        List<OfferTemplate> defaultTemplates = templateRepository.findAll().stream()
                .filter(t -> t.getIsDefault() != null && t.getIsDefault())
                .filter(t -> !t.getId().equals(id)) // Pomiń aktualnie ustawiany szablon
                .collect(java.util.stream.Collectors.toList());
        
        for (OfferTemplate defaultTemplate : defaultTemplates) {
            defaultTemplate.setIsDefault(false);
            templateRepository.save(defaultTemplate);
            logger.info("Usunięto domyślny status z szablonu ID: {}", defaultTemplate.getId());
        }
        
        // Ustaw nowy domyślny szablon
        OfferTemplate template = templateOpt.get();
        template.setIsDefault(true);
        OfferTemplate saved = templateRepository.save(template);
        
        logger.info("Szablon ID {} ustawiony jako domyślny", id);
        return saved;
    }

    /**
     * Renderuj szablon HTML z danymi projektu
     * Używa Thymeleaf do podmiany placeholderów
     */
    public String renderTemplate(OfferTemplate template, Project project) {
        logger.debug("Renderowanie szablonu ID {} dla projektu ID {}", template.getId(), project.getId());
        
        // Przygotuj dane dla Thymeleaf
        Context context = new Context();
        context.setVariable("project", project);
        context.setVariable("client", project.getClient());
        
        // TODO: Dodać produkty z snapshotów (podobnie jak w CreateOffer.java)
        // Na razie zwracamy podstawowy HTML z podstawionymi danymi
        
        // Jeśli szablon ma HTML content, użyj go
        if (template.getHtmlContent() != null && !template.getHtmlContent().isEmpty()) {
            // Renderuj HTML przez Thymeleaf
            String renderedHtml = templateEngine.process(template.getHtmlContent(), context);
            
            // Dodaj CSS jeśli istnieje
            if (template.getCssContent() != null && !template.getCssContent().isEmpty()) {
                renderedHtml = "<style>" + template.getCssContent() + "</style>\n" + renderedHtml;
            }
            
            return renderedHtml;
        }
        
        // Fallback - zwróć pusty HTML
        return "<html><body><p>Szablon nie ma zawartości HTML</p></body></html>";
    }

    /**
     * Tworzy domyślny szablon oferty jeśli nie istnieje
     * Wywoływane przy starcie aplikacji
     */
    @Transactional
    public void createDefaultTemplateIfNotExists() {
        // Najpierw zaktualizuj istniejące szablony (napraw starą składnię daty)
        updateTemplatesWithOldDateSyntax();
        
        Optional<OfferTemplate> existingDefault = templateRepository.findByIsDefaultTrue();
        if (existingDefault.isPresent()) {
            logger.info("Domyślny szablon już istnieje: ID={}, name={}", 
                existingDefault.get().getId(), existingDefault.get().getName());
            return;
        }
        
        logger.info("Tworzenie domyślnego szablonu oferty...");
        
        OfferTemplate defaultTemplate = new OfferTemplate();
        defaultTemplate.setName("Domyślny szablon oferty");
        defaultTemplate.setDescription("Podstawowy szablon oferty utworzony automatycznie");
        defaultTemplate.setIsDefault(true);
        
        // Podstawowy HTML z placeholderami Thymeleaf
        String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
            "<h1 style=\"color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px;\">Oferta handlowa</h1>" +
            "<div style=\"margin-top: 30px;\">" +
            "<h2>Dane projektu</h2>" +
            "<p><strong>Klient:</strong> [[${project.client.name}]] [[${project.client.surname}]]</p>" +
            "<p><strong>Data:</strong> [[${formattedDate}]]</p>" +
            "</div>" +
            "<div style=\"margin-top: 30px;\">" +
            "<h2>Dane klienta</h2>" +
            "<p th:if=\"${client != null}\"><strong>Imię i nazwisko:</strong> <span th:text=\"${client.name + ' ' + client.surname}\"></span></p>" +
            "<p th:if=\"${client != null && client.phone != null}\"><strong>Telefon:</strong> [[${client.phone}]]</p>" +
            "<p th:if=\"${client != null && client.email != null}\"><strong>Email:</strong> [[${client.email}]]</p>" +
            "</div>" +
            "<div style=\"margin-top: 30px;\">" +
            "<h2>Produkty</h2>" +
            "<div th:if=\"${allProducts != null && !allProducts.isEmpty()}\">" +
            "<table style=\"width: 100%; border-collapse: collapse; margin-top: 10px;\">" +
            "<thead><tr style=\"background-color: #4CAF50; color: white;\">" +
            "<th style=\"padding: 10px; text-align: left; border: 1px solid #ddd;\">Nazwa</th>" +
            "<th style=\"padding: 10px; text-align: right; border: 1px solid #ddd;\">Ilość</th>" +
            "<th style=\"padding: 10px; text-align: right; border: 1px solid #ddd;\">Cena</th>" +
            "<th style=\"padding: 10px; text-align: right; border: 1px solid #ddd;\">Wartość</th>" +
            "</tr></thead>" +
            "<tbody>" +
            "<tr th:each=\"product : ${allProducts}\" style=\"border-bottom: 1px solid #ddd;\">" +
            "<td style=\"padding: 8px; border: 1px solid #ddd;\">[[${product.name}]]</td>" +
            "<td style=\"padding: 8px; text-align: right; border: 1px solid #ddd;\">[[${#numbers.formatDecimal(product.quantity, 0, 2)}]]</td>" +
            "<td style=\"padding: 8px; text-align: right; border: 1px solid #ddd;\">[[${#numbers.formatDecimal(product.sellingPrice, 0, 2)}]] PLN</td>" +
            "<td style=\"padding: 8px; text-align: right; border: 1px solid #ddd;\">[[${#numbers.formatDecimal(product.sellingPrice * product.quantity, 0, 2)}]] PLN</td>" +
            "</tr>" +
            "</tbody>" +
            "</table>" +
            "<div style=\"margin-top: 20px; text-align: right;\">" +
            "<p style=\"font-size: 18px; font-weight: bold;\"><strong>Suma:</strong> [[${#numbers.formatDecimal(mainTotal, 0, 2)}]] PLN</p>" +
            "</div>" +
            "</div>" +
            "<p th:if=\"${allProducts == null || allProducts.isEmpty()}\" style=\"color: #999; font-style: italic;\">Brak produktów w ofercie</p>" +
            "</div>" +
            "</div>";
        
        defaultTemplate.setHtmlContent(htmlContent);
        
        // Podstawowy CSS
        String cssContent = "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; } " +
            "h1, h2 { color: #333; } " +
            "table { width: 100%; border-collapse: collapse; background-color: white; }";
        
        defaultTemplate.setCssContent(cssContent);
        
        OfferTemplate saved = templateRepository.save(defaultTemplate);
        logger.info("✅ Domyślny szablon utworzony: ID={}, name={}", saved.getId(), saved.getName());
    }
    
    /**
     * Aktualizuje wszystkie szablony, zamieniając starą składnię daty na nową
     * Zamienia: #dates.format(project.createdAt, 'dd.MM.yyyy') -> formattedDate
     * Usuwa również odwołania do nieistniejących pól (np. client.nip)
     */
    @Transactional
    public void updateTemplatesWithOldDateSyntax() {
        List<OfferTemplate> allTemplates = templateRepository.findAll();
        int updatedCount = 0;
        
        for (OfferTemplate template : allTemplates) {
            boolean updated = false;
            String htmlContent = template.getHtmlContent();
            
            if (htmlContent == null) {
                continue;
            }
            
            // Napraw składnię daty
            if (htmlContent.contains("#dates.format(project.createdAt")) {
                String replacement = java.util.regex.Matcher.quoteReplacement("[[${formattedDate}]]");
                htmlContent = htmlContent.replaceAll(
                    "\\[\\[\\$\\{#dates\\.format\\(project\\.createdAt,\\s*['\"]([^'\"]+)['\"]\\)\\}\\]\\]",
                    replacement
                );
                updated = true;
                logger.info("🔄 Zaktualizowano szablon ID={}, name={} - zamieniono składnię daty", 
                    template.getId(), template.getName());
            }
            
            // Usuń odwołania do nieistniejącego pola client.nip
            // SpEL ocenia wyrażenie przed sprawdzeniem warunku, więc musimy usunąć całe tagi
            if (htmlContent.contains("client.nip")) {
                // Usuń całe tagi <p> zawierające client.nip (non-greedy matching)
                String replacement = java.util.regex.Matcher.quoteReplacement("");
                htmlContent = htmlContent.replaceAll(
                    "(?s)<p[^>]*th:if=\"[^\"]*client\\.nip[^\"]*\"[^>]*>.*?</p>",
                    replacement
                );
                updated = true;
                logger.info("🔄 Zaktualizowano szablon ID={}, name={} - usunięto tagi z client.nip", 
                    template.getId(), template.getName());
            }
            
            if (updated) {
                template.setHtmlContent(htmlContent);
                templateRepository.save(template);
                updatedCount++;
            }
        }
        
        if (updatedCount > 0) {
            logger.info("✅ Zaktualizowano {} szablon(ów) - naprawiono składnię daty i usunięto nieistniejące pola", updatedCount);
        }
    }
}

