package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.OfferTemplate;
import pl.koszela.nowoczesnebud.Service.OfferTemplateService;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Kontroler do zarządzania szablonami ofert
 * CORS zarządzany globalnie przez WebConfig
 */
@RestController
@RequestMapping("/api/offer-templates")
public class OfferTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(OfferTemplateController.class);
    
    private final OfferTemplateService templateService;

    public OfferTemplateController(OfferTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Pobierz wszystkie szablony
     * GET /api/offer-templates
     */
    @GetMapping
    public ResponseEntity<List<OfferTemplate>> getAllTemplates() {
        logger.info("GET /api/offer-templates - pobieranie wszystkich szablonów");
        try {
            List<OfferTemplate> templates = templateService.getAllTemplates();
            logger.info("Znaleziono {} szablonów", templates.size());
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania szablonów", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pobierz szablon po ID
     * GET /api/offer-templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OfferTemplate> getTemplateById(@PathVariable Long id) {
        logger.info("GET /api/offer-templates/{} - pobieranie szablonu", id);
        
        Optional<OfferTemplate> templateOpt = templateService.getTemplateById(id);
        if (templateOpt.isPresent()) {
            return ResponseEntity.ok(templateOpt.get());
        } else {
            logger.warn("Szablon o ID {} nie został znaleziony", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Pobierz domyślny szablon
     * GET /api/offer-templates/default
     */
    @GetMapping("/default")
    public ResponseEntity<OfferTemplate> getDefaultTemplate() {
        logger.info("GET /api/offer-templates/default - pobieranie domyślnego szablonu");
        
        Optional<OfferTemplate> templateOpt = templateService.getDefaultTemplate();
        if (templateOpt.isPresent()) {
            return ResponseEntity.ok(templateOpt.get());
        } else {
            logger.warn("Brak domyślnego szablonu");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Utwórz nowy szablon
     * POST /api/offer-templates
     */
    @PostMapping
    public ResponseEntity<OfferTemplate> createTemplate(@Valid @RequestBody OfferTemplate template) {
        logger.info("POST /api/offer-templates - tworzenie szablonu: {}", template != null ? template.getName() : "null");
        
        try {
            if (template == null) {
                logger.error("Template jest null");
                return ResponseEntity.badRequest().build();
            }
            
            OfferTemplate saved = templateService.saveTemplate(template);
            logger.info("Szablon utworzony pomyślnie: ID={}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            logger.error("Błąd podczas tworzenia szablonu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Aktualizuj szablon
     * PUT /api/offer-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<OfferTemplate> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody OfferTemplate template) {
        logger.info("PUT /api/offer-templates/{} - aktualizacja szablonu", id);
        
        if (!id.equals(template.getId())) {
            logger.warn("Niezgodność ID w ścieżce ({}) i body ({})", id, template.getId());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OfferTemplate saved = templateService.saveTemplate(template);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Błąd podczas aktualizacji szablonu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Usuń szablon
     * DELETE /api/offer-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        logger.info("DELETE /api/offer-templates/{} - usuwanie szablonu", id);
        
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            logger.warn("Nie można usunąć szablonu: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Szablon nie istnieje: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Błąd podczas usuwania szablonu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Ustaw szablon jako domyślny
     * POST /api/offer-templates/{id}/set-default
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<OfferTemplate> setDefaultTemplate(@PathVariable Long id) {
        logger.info("POST /api/offer-templates/{}/set-default - ustawianie jako domyślny", id);
        
        try {
            OfferTemplate saved = templateService.setDefaultTemplate(id);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.warn("Szablon nie istnieje: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Błąd podczas ustawiania domyślnego szablonu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

