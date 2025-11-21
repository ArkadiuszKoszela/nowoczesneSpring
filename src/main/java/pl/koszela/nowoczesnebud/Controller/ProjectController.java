package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.DTO.GroupOptionRequest;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.InputRepository;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;
import pl.koszela.nowoczesnebud.Service.PriceCalculationService;
import pl.koszela.nowoczesnebud.Service.ProjectService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Kontroler obsługujący projekty
 * CORS zarządzany globalnie przez WebConfig
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    
    private final ProjectService projectService;
    private final CreateOffer createOffer;
    private final PriceCalculationService priceCalculationService;
    private final ProductRepository productRepository;
    private final InputRepository inputRepository;
    private final pl.koszela.nowoczesnebud.Service.OfferPdfService offerPdfService;

    public ProjectController(ProjectService projectService, 
                            CreateOffer createOffer,
                            PriceCalculationService priceCalculationService,
                            ProductRepository productRepository,
                            InputRepository inputRepository,
                            pl.koszela.nowoczesnebud.Service.OfferPdfService offerPdfService) {
        this.projectService = projectService;
        this.createOffer = createOffer;
        this.priceCalculationService = priceCalculationService;
        this.productRepository = productRepository;
        this.inputRepository = inputRepository;
        this.offerPdfService = offerPdfService;
    }

    /**
     * Pobiera wszystkie projekty
     */
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    /**
     * Pobiera wszystkich klientów (User)
     * ⚠️ WAŻNE: Ten endpoint musi być PRZED /client/{clientId}, aby Spring nie dopasował "clients" jako clientId
     */
    @GetMapping("/clients")
    public List<User> getAllClients() {
        return projectService.getAllClients();
    }

    /**
     * Pobiera projekt dla danego klienta (OneToOne - jeden klient ma jeden projekt)
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Project> getProjectByClient(@PathVariable Long clientId) {
        return projectService.getProjectByClientId(clientId)
            .map(project -> ResponseEntity.ok(project))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Pobiera projekt po ID
     */
    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }
    
    /**
     * Zapisuje dane produktów i grup dla projektu
     * POST /api/projects/{id}/save-data
     */
    @PostMapping("/{projectId}/save-data")
    public ResponseEntity<String> saveProjectData(
            @PathVariable Long projectId,
            @RequestBody pl.koszela.nowoczesnebud.DTO.SaveProjectDataRequest request) {
        logger.info("📥 Request: POST /api/projects/{}/save-data", projectId);
        
        try {
            projectService.saveProjectData(projectId, request);
            return ResponseEntity.ok("Project data saved successfully");
        } catch (Exception e) {
            logger.error("❌ Błąd podczas zapisu danych projektu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error saving project data: " + e.getMessage());
        }
    }
    
    /**
     * Pobiera zapisane dane produktów dla projektu
     * GET /api/projects/{id}/products?category=TILE
     */
    @GetMapping("/{projectId}/products")
    public ResponseEntity<List<pl.koszela.nowoczesnebud.DTO.ProjectProductDTO>> getProjectProducts(
            @PathVariable Long projectId,
            @RequestParam ProductCategory category) {
        logger.info("📥 Request: GET /api/projects/{}/products?category={}", projectId, category);
        
        try {
            List<pl.koszela.nowoczesnebud.DTO.ProjectProductDTO> products = 
                projectService.getProjectProducts(projectId, category);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("❌ Błąd podczas pobierania produktów projektu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Pobiera zapisane opcje grup produktowych dla projektu
     * GET /api/projects/{id}/product-groups?category=TILE
     */
    @GetMapping("/{projectId}/product-groups")
    public ResponseEntity<List<pl.koszela.nowoczesnebud.DTO.ProjectProductGroupDTO>> getProjectProductGroups(
            @PathVariable Long projectId,
            @RequestParam ProductCategory category) {
        logger.info("📥 Request: GET /api/projects/{}/product-groups?category={}", projectId, category);
        
        try {
            List<pl.koszela.nowoczesnebud.DTO.ProjectProductGroupDTO> groups = 
                projectService.getProjectProductGroups(projectId, category);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            logger.error("❌ Błąd podczas pobierania grup produktowych projektu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Porównuje aktualne ceny z cennika z zapisanymi cenami w projekcie
     * GET /api/projects/{id}/products-comparison?category=TILE
     * Zwraca ProductComparisonDTO (Stara vs Nowa cena) dla UI
     */
    @GetMapping("/{projectId}/products-comparison")
    public ResponseEntity<List<pl.koszela.nowoczesnebud.DTO.ProductComparisonDTO>> getProductComparison(
            @PathVariable Long projectId,
            @RequestParam ProductCategory category) {
        logger.info("📥 Request: GET /api/projects/{}/products-comparison?category={}", projectId, category);
        
        try {
            List<pl.koszela.nowoczesnebud.DTO.ProductComparisonDTO> comparison = 
                projectService.getProductComparison(projectId, category);
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            logger.error("❌ Błąd podczas porównania cen produktów: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    // ==================== DRAFT CHANGES ENDPOINTS ====================
    
    /**
     * Zapisuje tymczasowe zmiany (draft changes) dla projektu
     * POST /api/projects/{id}/draft-changes
     * Używane po każdej zmianie marży/rabatu/ceny ręcznej na frontendzie
     */
    @PostMapping("/{projectId}/draft-changes")
    public ResponseEntity<Void> saveDraftChanges(
            @PathVariable Long projectId,
            @RequestBody pl.koszela.nowoczesnebud.DTO.SaveDraftChangesRequest request) {
        logger.info("📥 Request: POST /api/projects/{}/draft-changes (kategoria: {})", projectId, request.getCategory());
        
        try {
            projectService.saveDraftChanges(projectId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("❌ Błąd podczas zapisu draft changes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Pobiera draft changes dla projektu (opcjonalnie filtrowane po kategorii)
     * GET /api/projects/{id}/draft-changes?category=TILE
     */
    @GetMapping("/{projectId}/draft-changes")
    public ResponseEntity<List<pl.koszela.nowoczesnebud.DTO.DraftChangeDTO>> getDraftChanges(
            @PathVariable Long projectId,
            @RequestParam(required = false) String category) {
        logger.info("📥 Request: GET /api/projects/{}/draft-changes?category={}", projectId, category);
        
        try {
            List<pl.koszela.nowoczesnebud.DTO.DraftChangeDTO> draftChanges = 
                projectService.getDraftChanges(projectId, category);
            return ResponseEntity.ok(draftChanges);
        } catch (Exception e) {
            logger.error("❌ Błąd podczas pobierania draft changes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Usuwa wszystkie draft changes dla projektu
     * DELETE /api/projects/{id}/draft-changes
     * Używane do "Cofnij zmiany" lub po zapisaniu projektu
     */
    @DeleteMapping("/{projectId}/draft-changes")
    public ResponseEntity<Void> clearDraftChanges(@PathVariable Long projectId) {
        logger.info("📥 Request: DELETE /api/projects/{}/draft-changes", projectId);
        
        try {
            projectService.clearDraftChanges(projectId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("❌ Błąd podczas usuwania draft changes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== DRAFT INPUTS ====================
    
    /**
     * Zapisuje draft inputs (tymczasowe Input z formularza)
     * POST /api/projects/{id}/draft-inputs
     * Używane po każdej zmianie w formularzu "Wprowadź dane"
     */
    @PostMapping("/{projectId}/draft-inputs")
    public ResponseEntity<Void> saveDraftInputs(
            @PathVariable Long projectId,
            @RequestBody pl.koszela.nowoczesnebud.DTO.SaveDraftInputsRequest request) {
        logger.info("📥 Request: POST /api/projects/{}/draft-inputs", projectId);
        
        try {
            projectService.saveDraftInputs(projectId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("❌ Błąd podczas zapisu draft inputs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Pobiera draft inputs dla projektu
     * GET /api/projects/{id}/draft-inputs
     */
    @GetMapping("/{projectId}/draft-inputs")
    public ResponseEntity<List<pl.koszela.nowoczesnebud.DTO.DraftInputDTO>> getDraftInputs(
            @PathVariable Long projectId) {
        logger.info("📥 Request: GET /api/projects/{}/draft-inputs", projectId);
        
        try {
            List<pl.koszela.nowoczesnebud.DTO.DraftInputDTO> draftInputs = 
                projectService.getDraftInputs(projectId);
            return ResponseEntity.ok(draftInputs);
        } catch (Exception e) {
            logger.error("❌ Błąd podczas pobierania draft inputs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Usuwa wszystkie draft inputs dla projektu
     * DELETE /api/projects/{id}/draft-inputs
     * Używane do "Cofnij zmiany" lub po zapisaniu projektu
     */
    @DeleteMapping("/{projectId}/draft-inputs")
    public ResponseEntity<Void> clearDraftInputs(@PathVariable Long projectId) {
        logger.info("📥 Request: DELETE /api/projects/{}/draft-inputs", projectId);
        
        try {
            projectService.clearDraftInputs(projectId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("❌ Błąd podczas usuwania draft inputs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Zapisuje projekt (tworzy nowy lub aktualizuje istniejący)
     */
    @PostMapping("/save")
    public Project saveProject(@RequestBody Project project) {
        return projectService.save(project);
    }

    /**
     * Usuwa projekt
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * TODO: Przepisać na nowy model - używa ProjectProduct zamiast Input z productId
     * Zapisuje override'y ceny i ilości dla produktów w projekcie
     * POST /api/projects/{projectId}/price-override
     * Przyjmuje listę override'ów: [{ productId, manualSellingPrice?, manualQuantity? }]
     */
    // @PostMapping("/{projectId}/price-override")
    /* ZAKOMENTOWANE - używa starych pół Input (productId, manualSellingPrice, manualQuantity)
    public ResponseEntity<List<Input>> savePriceOverrides(
            @PathVariable Long projectId,
            @RequestBody List<PriceOverrideRequest> overrideRequests) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Input> savedInputs = new ArrayList<>();
            
            for (PriceOverrideRequest request : overrideRequests) {
                if (request.getProductId() == null) {
                    logger.warn("Pominięto override z productId = null");
                    continue;
                }
                
                logger.info("💾 Zapisywanie override dla produktu {}: manualSellingPrice={}, manualPurchasePrice={}, manualQuantity={}", 
                           request.getProductId(), request.getManualSellingPrice(), request.getManualPurchasePrice(), request.getManualQuantity());
                
                // Znajdź istniejący Input z productId dla tego projektu
                Optional<Input> existingInputOpt = project.getInputs().stream()
                    .filter(input -> input.getProductId() != null && 
                            input.getProductId().equals(request.getProductId()))
                    .findFirst();
                
                Input input;
                if (existingInputOpt.isPresent()) {
                    // Aktualizuj istniejący
                    input = existingInputOpt.get();
                    logger.info("✅ Znaleziono istniejący Input (id={}) dla produktu {} - aktualizuję", 
                               input.getId(), request.getProductId());
                    
                    // Jeśli wartości są null, usuń override (ustaw na null)
                    if (request.getManualSellingPrice() != null) {
                        logger.info("  → Ustawiam manualSellingPrice: {} (było: {})", 
                                   request.getManualSellingPrice(), input.getManualSellingPrice());
                        input.setManualSellingPrice(request.getManualSellingPrice());
                    } else if (request.getManualSellingPrice() == null && request.getShouldRemovePrice() != null && request.getShouldRemovePrice()) {
                        input.setManualSellingPrice(null);
                    }
                    if (request.getManualPurchasePrice() != null) {
                        logger.info("  → Ustawiam manualPurchasePrice: {} (było: {})", 
                                   request.getManualPurchasePrice(), input.getManualPurchasePrice());
                        input.setManualPurchasePrice(request.getManualPurchasePrice());
                    } else if (request.getManualPurchasePrice() == null && request.getShouldRemovePrice() != null && request.getShouldRemovePrice()) {
                        input.setManualPurchasePrice(null);
                    }
                    if (request.getManualQuantity() != null) {
                        logger.info("  → Ustawiam manualQuantity: {} (było: {})", 
                                   request.getManualQuantity(), input.getManualQuantity());
                        input.setManualQuantity(request.getManualQuantity());
                    } else if (request.getManualQuantity() == null && request.getShouldRemoveQuantity() != null && request.getShouldRemoveQuantity()) {
                        input.setManualQuantity(null);
                    }
                    
                    // Jeśli wszystkie override'y są null, usuń cały Input (override nie jest już potrzebny)
                    if (input.getManualSellingPrice() == null && input.getManualPurchasePrice() == null && input.getManualQuantity() == null) {
                        project.getInputs().remove(input);
                        inputRepository.delete(input);
                        logger.debug("Usunięto override dla produktu {} (oba override'y były null)", request.getProductId());
                        continue;
                    }
                } else {
                    // Jeśli próbujemy ustawić na null, nie tworzymy nowego Input
                    if (request.getManualSellingPrice() == null && request.getManualPurchasePrice() == null && request.getManualQuantity() == null) {
                        logger.debug("Pominięto tworzenie override'u dla produktu {} (wszystkie wartości są null)", request.getProductId());
                        continue;
                    }
                    
                    // Utwórz nowy Input dla override'u
                    logger.info("➕ Tworzę nowy Input dla produktu {}: manualSellingPrice={}, manualPurchasePrice={}, manualQuantity={}", 
                               request.getProductId(), request.getManualSellingPrice(), request.getManualPurchasePrice(), request.getManualQuantity());
                    input = new Input();
                    input.setProject(project);
                    input.setProductId(request.getProductId());
                    input.setManualSellingPrice(request.getManualSellingPrice());
                    input.setManualPurchasePrice(request.getManualPurchasePrice());
                    input.setManualQuantity(request.getManualQuantity());
                    // name, mapperName, quantity pozostają null (to nie jest Input z formularza)
                    project.getInputs().add(input);
                }
                
                savedInputs.add(input);
            }
            
            // Batch update - zapisz wszystkie Input
            List<Input> saved = inputRepository.saveAll(savedInputs);
            logger.info("Zapisano {} override'ów dla projektu {}", saved.size(), projectId);
            
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            logger.error("Błąd podczas zapisywania override'ów dla projektu {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } */
    
    /**
     * TODO: Przepisać na nowy model - używa ProjectProductGroup zamiast Input z groupManufacturer/groupName
     * Zapisuje opcję (Główna/Opcjonalna) dla grupy produktów w projekcie
     * POST /api/projects/{projectId}/group-option
     * Przyjmuje: { category, manufacturer, groupName, isMainOption }
     */
    // @PostMapping("/{projectId}/group-option")
    /* ZAKOMENTOWANE - używa starych pól Input (groupManufacturer, groupName, isMainOption)
    public ResponseEntity<Input> saveGroupOption(
            @PathVariable Long projectId,
            @RequestBody GroupOptionRequest request) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            
            logger.info("💾 Zapisywanie opcji dla grupy w projekcie {}: {} / {} → {}", 
                       projectId, request.getManufacturer(), request.getGroupName(), request.getIsMainOption());
            
            // Znajdź istniejący Input z groupManufacturer i groupName dla tego projektu
            Optional<Input> existingInputOpt = project.getInputs().stream()
                .filter(input -> input.getGroupManufacturer() != null && 
                        input.getGroupName() != null &&
                        input.getGroupManufacturer().equals(request.getManufacturer()) &&
                        input.getGroupName().equals(request.getGroupName()))
                .findFirst();
            
            Input input;
            if (existingInputOpt.isPresent()) {
                // Aktualizuj istniejący
                input = existingInputOpt.get();
                logger.info("✅ Znaleziono istniejący Input (id={}) dla grupy {} / {} - aktualizuję", 
                           input.getId(), request.getManufacturer(), request.getGroupName());
                
                input.setIsMainOption(request.getIsMainOption());
                
                // Jeśli opcja jest null, usuń Input (opcja nie jest już potrzebna)
                if (input.getIsMainOption() == null) {
                    project.getInputs().remove(input);
                    inputRepository.delete(input);
                    logger.debug("Usunięto opcję dla grupy {} / {} (opcja była null)", 
                               request.getManufacturer(), request.getGroupName());
                    return ResponseEntity.ok().build();
                }
            } else {
                // Jeśli próbujemy ustawić na null, nie tworzymy nowego Input
                if (request.getIsMainOption() == null) {
                    logger.debug("Pominięto tworzenie opcji dla grupy {} / {} (wartość jest null)", 
                               request.getManufacturer(), request.getGroupName());
                    return ResponseEntity.ok().build();
                }
                
                // Utwórz nowy Input dla opcji grupy
                logger.info("➕ Tworzę nowy Input dla grupy {} / {}: isMainOption={}", 
                           request.getManufacturer(), request.getGroupName(), request.getIsMainOption());
                input = new Input();
                input.setProject(project);
                input.setGroupManufacturer(request.getManufacturer());
                input.setGroupName(request.getGroupName());
                input.setIsMainOption(request.getIsMainOption());
                // name, mapperName, quantity, productId pozostają null (to nie jest Input z formularza ani override produktu)
                project.getInputs().add(input);
            }
            
            // Zapisz Input
            Input saved = inputRepository.save(input);
            logger.info("Zapisano opcję dla grupy {} / {} w projekcie {}", 
                       request.getManufacturer(), request.getGroupName(), projectId);
            
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            logger.error("Błąd podczas zapisywania opcji grupy dla projektu {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } */
    
    /**
     * TODO: Przepisać na nowy model - używa ProjectProduct zamiast Input z productId
     * Usuwa wszystkie override'y ceny i ilości dla produktów w projekcie
     * DELETE /api/projects/{projectId}/price-override
     */
    // @DeleteMapping("/{projectId}/price-override")
    /* ZAKOMENTOWANE - używa starych pół Input (productId)
    public ResponseEntity<Void> deleteAllPriceOverrides(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Znajdź wszystkie Input z productId (override'y)
            List<Input> overrideInputs = project.getInputs().stream()
                .filter(input -> input.getProductId() != null)
                .collect(Collectors.toList());
            
            if (!overrideInputs.isEmpty()) {
                // Usuń wszystkie override'y
                project.getInputs().removeAll(overrideInputs);
                inputRepository.deleteAll(overrideInputs);
                logger.info("Usunięto {} override'ów dla projektu {}", overrideInputs.size(), projectId);
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Błąd podczas usuwania override'ów dla projektu {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } */
    
    /**
     * DTO dla requestu override'u ceny/ilości
     */
    public static class PriceOverrideRequest {
        private Long productId;
        private Double manualSellingPrice;
        private Double manualPurchasePrice;
        private Double manualQuantity;
        private Boolean shouldRemovePrice;  // Flaga do usunięcia override ceny
        private Boolean shouldRemoveQuantity;  // Flaga do usunięcia override ilości
        
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public Double getManualSellingPrice() {
            return manualSellingPrice;
        }
        
        public void setManualSellingPrice(Double manualSellingPrice) {
            this.manualSellingPrice = manualSellingPrice;
        }
        
        public Double getManualPurchasePrice() {
            return manualPurchasePrice;
        }
        
        public void setManualPurchasePrice(Double manualPurchasePrice) {
            this.manualPurchasePrice = manualPurchasePrice;
        }
        
        public Double getManualQuantity() {
            return manualQuantity;
        }
        
        public void setManualQuantity(Double manualQuantity) {
            this.manualQuantity = manualQuantity;
        }
        
        public Boolean getShouldRemovePrice() {
            return shouldRemovePrice;
        }
        
        public void setShouldRemovePrice(Boolean shouldRemovePrice) {
            this.shouldRemovePrice = shouldRemovePrice;
        }
        
        public Boolean getShouldRemoveQuantity() {
            return shouldRemoveQuantity;
        }
        
        public void setShouldRemoveQuantity(Boolean shouldRemoveQuantity) {
            this.shouldRemoveQuantity = shouldRemoveQuantity;
        }
    }

    /**
     * Aktualizuje dane klienta (User)
     */
    @PutMapping("/client/{userId}")
    public ResponseEntity<User> updateClient(@PathVariable Long userId, @RequestBody User client) {
        try {
            if (!userId.equals(client.getId())) {
                logger.warn("Niezgodność ID w ścieżce ({}) i body ({})", userId, client.getId());
                return ResponseEntity.badRequest().build();
            }
            
            User updatedClient = projectService.updateClient(client);
            return ResponseEntity.ok(updatedClient);
        } catch (Exception e) {
            logger.error("Błąd podczas aktualizacji klienta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Usuwa klienta (User) wraz z wszystkimi jego projektami
     */
    @DeleteMapping("/client/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        try {
            projectService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Błąd podczas usuwania klienta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * TODO: Przepisać na nowy model - używa ProductProduct zamiast PriceListSnapshot
     * Pobiera produkty ze snapshotu projektu dla danej kategorii
     * Zwraca produkty ze snapshotu + dane z Input (quantity, sellingPrice, isManualPrice)
     */
    // @GetMapping("/{projectId}/snapshot-products")
    /* ZAKOMENTOWANE - używa starego modelu PriceListSnapshot
    public ResponseEntity<List<Product>> getSnapshotProducts(
            @PathVariable Long projectId,
            @RequestParam ProductCategory category) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            // Upewnij się że snapshotDate jest ustawione
            if (project.getSnapshotDate() == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Znajdź snapshot dla daty projektu i kategorii
            Optional<pl.koszela.nowoczesnebud.Model.PriceListSnapshot> snapshotOpt = 
                priceListSnapshotService.findSnapshotForDate(project.getSnapshotDate(), category);
            
            if (!snapshotOpt.isPresent()) {
                logger.warn("Brak snapshotu dla projektu {} kategorii {} daty {}", 
                           projectId, category, project.getSnapshotDate());
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            pl.koszela.nowoczesnebud.Model.PriceListSnapshot snapshot = snapshotOpt.get();
            List<PriceListSnapshotItem> snapshotItems = priceListSnapshotService.getSnapshotItems(snapshot.getId());
            
            // ⚠️ WAŻNE: Wszystkie Input są teraz z formularza (usunęliśmy pola produktowe)
            // Oblicz quantity na podstawie Input z formularza (mapperName matching)
            
            // Mapuj Input z formularza (mapperName -> quantity) dla obliczenia quantity
            final Map<String, Double> formInputQuantityMap;
            if (project.getInputs() != null && !project.getInputs().isEmpty()) {
                formInputQuantityMap = project.getInputs().stream()
                    .filter(input -> input.getMapperName() != null && input.getQuantity() != null && input.getProductId() == null)
                    .collect(Collectors.toMap(
                        input -> input.getMapperName().toLowerCase().trim(),
                        Input::getQuantity,
                        (existing, replacement) -> existing
                    ));
                logger.debug("📊 Znaleziono {} Input z formularza dla obliczenia quantity", formInputQuantityMap.size());
            } else {
                formInputQuantityMap = new HashMap<>();
            }
            
            // Mapuj override'y dla produktów (productId -> manualSellingPrice, manualQuantity)
            final Map<Long, Input> priceOverrideMap;
            if (project.getInputs() != null && !project.getInputs().isEmpty()) {
                priceOverrideMap = project.getInputs().stream()
                    .filter(input -> input.getProductId() != null)
                    .collect(Collectors.toMap(
                        Input::getProductId,
                        input -> input,
                        (existing, replacement) -> existing
                    ));
                logger.debug("📊 Znaleziono {} override'ów dla produktów", priceOverrideMap.size());
            } else {
                priceOverrideMap = new HashMap<>();
            }
            
            // Mapuj opcje dla grup produktów (manufacturer + groupName -> isMainOption)
            final Map<String, Boolean> groupOptionMap;
            if (project.getInputs() != null && !project.getInputs().isEmpty()) {
                groupOptionMap = project.getInputs().stream()
                    .filter(input -> input.getGroupManufacturer() != null && input.getGroupName() != null)
                    .collect(Collectors.toMap(
                        input -> input.getGroupManufacturer() + "|" + input.getGroupName(),
                        Input::getIsMainOption,
                        (existing, replacement) -> replacement
                    ));
                logger.debug("📊 Znaleziono {} opcji dla grup produktów", groupOptionMap.size());
            } else {
                groupOptionMap = new HashMap<>();
            }
            
            // Konwertuj PriceListSnapshotItem na Product DTO i oblicz quantity
            List<Product> products = snapshotItems.stream()
                .map(item -> {
                    Product product = new Product();
                    product.setId(item.getProductId());
                    product.setName(item.getName());
                    product.setManufacturer(item.getManufacturer());
                    product.setGroupName(item.getGroupName());
                    product.setCategory(item.getCategory());
                    product.setMapperName(item.getMapperName());
                    product.setRetailPrice(item.getRetailPrice());
                    product.setPurchasePrice(item.getPurchasePrice());
                    product.setSellingPrice(item.getSellingPrice());
                    // Użyj nowego pola discount (jeśli snapshot ma stare pola, użyj 0.0)
                    product.setDiscount(item.getDiscount() != null ? item.getDiscount() : 0.0);
                    product.setMarginPercent(item.getMarginPercent() != null ? item.getMarginPercent() : 0.0);
                    product.setUnit(item.getUnit());
                    product.setQuantityConverter(item.getQuantityConverter() != null ? item.getQuantityConverter() : 1.0);
                    
                    // Kopiuj accessoryType (dla ACCESSORY)
                    if (item.getCategory() == ProductCategory.ACCESSORY) {
                        product.setAccessoryType(item.getAccessoryType());
                    }
                    
                    // Obsługa opcji grupy - najpierw ustaw ze snapshotu, potem nadpisz z Input jeśli istnieje
                    product.setIsMainOption(item.getIsMainOption());
                    String groupKey = item.getManufacturer() + "|" + item.getGroupName();
                    if (groupOptionMap.containsKey(groupKey)) {
                        Boolean groupOption = groupOptionMap.get(groupKey);
                        product.setIsMainOption(groupOption);
                        logger.debug("📌 Ustawiono opcję dla grupy {} / {}: {}", 
                                   item.getManufacturer(), item.getGroupName(), groupOption);
                    }
                    
                    // ⚠️ WAŻNE: Oblicz quantity na podstawie Input z formularza
                    double calculatedQuantity = 0.0;
                    if (item.getMapperName() != null) {
                        String mapperKey = item.getMapperName().toLowerCase().trim();
                        Double inputQuantity = formInputQuantityMap.get(mapperKey);
                        
                        if (inputQuantity != null && inputQuantity > 0) {
                            double quantityConverter = product.getQuantityConverter() != null ? product.getQuantityConverter() : 1.0;
                            
                            // Oblicz quantity produktu: inputQuantity * quantityConverter
                            calculatedQuantity = priceCalculationService.calculateProductQuantity(
                                inputQuantity,
                                quantityConverter
                            );
                            
                            logger.debug("📊 Obliczono quantity dla produktu {} ({}): inputQuantity={} * quantityConverter={} = {}",
                                       item.getProductId(), item.getName(),
                                       inputQuantity, quantityConverter, calculatedQuantity);
                        }
                    }
                    
                    // Sprawdź czy jest override dla tego produktu
                    Input override = priceOverrideMap.get(item.getProductId());
                    Double originalSellingPrice = item.getSellingPrice();
                    Double originalQuantity = calculatedQuantity;
                    
                    // Obsługa override'u sellingPrice
                    // Zawsze ustaw originalSellingPrice na cenę ze snapshotu (sugerowana cena)
                    product.setOriginalSellingPrice(originalSellingPrice);
                    
                    if (override != null && override.getManualSellingPrice() != null) {
                        Double manualSellingPrice = override.getManualSellingPrice();
                        // Porównaj z ceną ze snapshotu
                        if (Math.abs(manualSellingPrice - (originalSellingPrice != null ? originalSellingPrice : 0.0)) > 0.01) {
                            // Ceny są różne - użyj ręcznej ceny
                            product.setSellingPrice(manualSellingPrice);
                            product.setIsManualPrice(true);
                        } else {
                            // Ceny są takie same - użyj ceny ze snapshotu
                            product.setSellingPrice(originalSellingPrice);
                            product.setIsManualPrice(false);
                        }
                    } else {
                        // Brak override'u - użyj ceny ze snapshotu
                        product.setSellingPrice(originalSellingPrice);
                        product.setIsManualPrice(false);
                    }
                    
                    // Obsługa override'u purchasePrice
                    Double originalPurchasePrice = item.getPurchasePrice();
                    if (override != null && override.getManualPurchasePrice() != null) {
                        Double manualPurchasePrice = override.getManualPurchasePrice();
                        // Porównaj z ceną zakupu ze snapshotu
                        if (Math.abs(manualPurchasePrice - (originalPurchasePrice != null ? originalPurchasePrice : 0.0)) > 0.01) {
                            // Ceny są różne - użyj ręcznej ceny zakupu
                            product.setPurchasePrice(manualPurchasePrice);
                            product.setIsManualPurchasePrice(true);
                            product.setOriginalPurchasePrice(originalPurchasePrice);
                        } else {
                            // Ceny są takie same - użyj ceny zakupu ze snapshotu
                            product.setPurchasePrice(originalPurchasePrice);
                            product.setIsManualPurchasePrice(false);
                        }
                    } else {
                        // Brak override'u - użyj ceny zakupu ze snapshotu
                        product.setPurchasePrice(originalPurchasePrice);
                        product.setIsManualPurchasePrice(false);
                    }
                    
                    // Obsługa override'u quantity
                    if (override != null && override.getManualQuantity() != null) {
                        Double manualQuantity = override.getManualQuantity();
                        // Porównaj z obliczoną quantity
                        if (Math.abs(manualQuantity - originalQuantity) > 0.01) {
                            // Ilości są różne - użyj ręcznej ilości
                            product.setQuantity(manualQuantity);
                            product.setIsManualQuantity(true);
                            product.setOriginalQuantity(originalQuantity);
                        } else {
                            // Ilości są takie same - użyj obliczonej
                            product.setQuantity(originalQuantity);
                            product.setIsManualQuantity(false);
                        }
                    } else {
                        // Brak override'u - użyj obliczonej quantity
                        product.setQuantity(originalQuantity);
                        product.setIsManualQuantity(false);
                    }
                    
                    return product;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            logger.error("Błąd pobierania produktów ze snapshotu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } */

    /**
     * TODO: Przepisać na nowy model - używa ProjectProduct zamiast PriceListSnapshot
     * Wypełnij ilości produktów na podstawie inputów - używa snapshotu projektu
     * POST /api/projects/{projectId}/fill-quantities?category=TILE
     * ⚠️ WAŻNE: Używa produktów ze snapshotu projektu, nie z aktualnego cennika!
     */
    // @PostMapping("/{projectId}/fill-quantities")
    /* ZAKOMENTOWANE - używa starego modelu PriceListSnapshot
    public ResponseEntity<List<Product>> fillQuantitiesFromSnapshot(
            @PathVariable Long projectId,
            @RequestBody List<Input> inputList,
            @RequestParam ProductCategory category) {
        
        logger.info("fillQuantitiesFromSnapshot - projekt ID: {}, kategoria: {}", projectId, category);
        logger.debug("Otrzymano inputów: {}", inputList.size());
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            // Upewnij się że snapshotDate jest ustawione
            if (project.getSnapshotDate() == null) {
                logger.warn("Projekt {} nie ma snapshotDate", projectId);
                return ResponseEntity.badRequest().build();
            }
            
            // Znajdź snapshot dla daty projektu i kategorii
            Optional<pl.koszela.nowoczesnebud.Model.PriceListSnapshot> snapshotOpt = 
                priceListSnapshotService.findSnapshotForDate(project.getSnapshotDate(), category);
            
            if (!snapshotOpt.isPresent()) {
                logger.warn("Brak snapshotu dla projektu {} kategorii {} daty {}", 
                           projectId, category, project.getSnapshotDate());
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            pl.koszela.nowoczesnebud.Model.PriceListSnapshot snapshot = snapshotOpt.get();
            List<PriceListSnapshotItem> snapshotItems = priceListSnapshotService.getSnapshotItems(snapshot.getId());
            
            // Stwórz mapę inputów (mapperName -> Input) dla szybkiego wyszukiwania
            // ⚠️ WAŻNE: Użyj lowercase dla case-insensitive matching
            Map<String, Input> inputMap = new HashMap<>();
            for (Input input : inputList) {
                if (input.getMapperName() != null) {
                    String key = input.getMapperName().toLowerCase().trim();
                    inputMap.put(key, input);
                }
            }
            
            logger.info("📋 Utworzono mapę {} inputów z formularza: {}", inputMap.size(), 
                inputMap.entrySet().stream()
                    .map(e -> String.format("%s=%s", e.getKey(), e.getValue().getQuantity()))
                    .collect(Collectors.joining(", ")));
            
            logger.info("📦 Liczba produktów w snapshotcie dla kategorii {}: {}", category, snapshotItems.size());
            
            // ⚠️ WAŻNE: Wszystkie Input są teraz z formularza (usunęliśmy pola produktowe)
            // Użyj Input z request body (najnowsze wartości z formularza)
            // + zachowaj istniejące Input z bazy (jeśli request body nie ma wszystkich)
            List<Input> formInputsFromRequest = inputList; // Wszystkie Input są z formularza

            // Zachowaj istniejące Input z bazy (na wypadek gdyby request body nie miał wszystkich)
            List<Input> formInputsFromDb = project.getInputs() != null ? project.getInputs() : new ArrayList<>();
            
            // Połącz Input z request body (priorytet) + Input z bazy (fallback)
            Map<String, Input> formInputsMap = new HashMap<>();
            // Najpierw dodaj z bazy
            for (Input input : formInputsFromDb) {
                if (input.getMapperName() != null) {
                    formInputsMap.put(input.getMapperName().toLowerCase().trim(), input);
                }
            }
            // Potem nadpisz wartościami z request body (priorytet)
            for (Input input : formInputsFromRequest) {
                if (input.getMapperName() != null) {
                    formInputsMap.put(input.getMapperName().toLowerCase().trim(), input);
                }
            }
            
            List<Input> formInputs = new ArrayList<>(formInputsMap.values());
            logger.info("📝 Używam {} Input z formularza ({} z request, {} z bazy, {} po połączeniu)", 
                       formInputs.size(), formInputsFromRequest.size(), formInputsFromDb.size(), formInputs.size());
            
            // ⚠️ WAŻNE: NIE tworzymy Input produktów w bazie - produkty są w snapshotach
            // Konwertuj PriceListSnapshotItem na Product i wypełnij ilości (bez zapisywania do bazy)
            List<Product> products = new ArrayList<>();
            
            for (PriceListSnapshotItem item : snapshotItems) {
                Product product = new Product();
                product.setId(item.getProductId());
                product.setName(item.getName());
                product.setManufacturer(item.getManufacturer());
                product.setGroupName(item.getGroupName());
                product.setCategory(item.getCategory());
                product.setMapperName(item.getMapperName());
                product.setRetailPrice(item.getRetailPrice());
                product.setPurchasePrice(item.getPurchasePrice());
                product.setSellingPrice(item.getSellingPrice());
                product.setBasicDiscount(item.getBasicDiscount() != null ? item.getBasicDiscount() : 0);
                product.setPromotionDiscount(item.getPromotionDiscount() != null ? item.getPromotionDiscount() : 0);
                product.setAdditionalDiscount(item.getAdditionalDiscount() != null ? item.getAdditionalDiscount() : 0);
                product.setSkontoDiscount(item.getSkontoDiscount() != null ? item.getSkontoDiscount() : 0);
                product.setMarginPercent(item.getMarginPercent() != null ? item.getMarginPercent() : 0.0);
                product.setIsMainOption(item.getIsMainOption());
                product.setUnit(item.getUnit());
                product.setQuantityConverter(item.getQuantityConverter() != null ? item.getQuantityConverter() : 1.0);
                
                // ⚠️ WAŻNE: Wypełnij ilość na podstawie Input z formularza
                double calculatedQuantity = 0.0;
                if (item.getMapperName() != null && item.getProductId() != null) {
                    // ⚠️ WAŻNE: Użyj lowercase dla case-insensitive matching
                    String mapperKey = item.getMapperName().toLowerCase().trim();
                    Input formInput = inputMap.get(mapperKey);
                    
                    if (formInput != null) {
                        logger.debug("Znaleziono Input dla produktu {} (mapperName: {}): quantity={}", 
                                   item.getProductId(), item.getMapperName(), formInput.getQuantity());
                        
                        if (formInput.getQuantity() != null && formInput.getQuantity() > 0) {
                            double quantityConverter = product.getQuantityConverter() != null ? product.getQuantityConverter() : 1.0;
                            
                            // ⚠️ WAŻNE: Fallback - jeśli snapshot nie ma quantityConverter, pobierz z aktualnego produktu
                            if ((quantityConverter == 1.0 || item.getQuantityConverter() == null) && item.getProductId() != null) {
                                Optional<pl.koszela.nowoczesnebud.Model.Product> currentProductOpt = 
                                    productRepository.findById(item.getProductId());
                                if (currentProductOpt.isPresent()) {
                                    pl.koszela.nowoczesnebud.Model.Product currentProduct = currentProductOpt.get();
                                    if (currentProduct.getQuantityConverter() != null && currentProduct.getQuantityConverter() != 1.0) {
                                        quantityConverter = currentProduct.getQuantityConverter();
                                        logger.info("📦 Używam quantityConverter z aktualnego produktu {}: {}", 
                                                   item.getProductId(), quantityConverter);
                                    }
                                }
                            }
                            
                            logger.info("🔢 Obliczam ilość dla produktu {} ({}): inputQuantity={} * quantityConverter={} = {}", 
                                       item.getProductId(), item.getName(),
                                       formInput.getQuantity(), quantityConverter, 
                                       formInput.getQuantity() * quantityConverter);
                            
                            // Oblicz ilość produktu (z uwzględnieniem quantityConverter ze snapshotu)
                            calculatedQuantity = priceCalculationService.calculateProductQuantity(
                                formInput.getQuantity(), 
                                quantityConverter
                            );
                            
                            logger.debug("Obliczona ilość dla produktu {}: {}", item.getProductId(), calculatedQuantity);
                            
                            // Ustaw cenę sprzedaży = cena katalogowa ze snapshotu
                            if (product.getRetailPrice() > 0.00) {
                                product.setSellingPrice(product.getRetailPrice());
                            }
                        } else {
                            logger.debug("Input quantity jest null lub 0 dla produktu {}", item.getProductId());
                        }
                    } else {
                        logger.debug("Brak Input dla produktu {} (mapperName: '{}', szukany klucz: '{}')", 
                                   item.getProductId(), item.getMapperName(), mapperKey);
                    }
                } else {
                    if (item.getMapperName() == null) {
                        logger.debug("Produkt {} nie ma mapperName", item.getProductId());
                    }
                }
                product.setQuantity(calculatedQuantity);
                
                // ⚠️ WAŻNE: NIE tworzymy Input produktów w bazie - produkty są pobierane ze snapshotów
                // quantity i sellingPrice są tylko w Product DTO, nie w Input w bazie
                logger.debug("✅ Obliczono quantity dla produktu {} ({}): quantity={}, sellingPrice={}", 
                           item.getProductId(), item.getName(), calculatedQuantity, product.getSellingPrice());
                
                products.add(product);
            }
            
            // ⚠️ WAŻNE: NIE zapisujemy inputów do bazy - użytkownik musi kliknąć "Zapisz projekt"
            // Endpoint tylko oblicza i zwraca produkty z quantity dla frontendu
            logger.info("✅ Obliczono produkty - NIE zapisano do bazy (użytkownik musi kliknąć 'Zapisz projekt')");
            
            // Policz ile produktów ma quantity > 0
            long productsWithQuantity = products.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .count();
            
            logger.info("✅ Zwracam {} produktów ze snapshotu: {} z quantity > 0", 
                       products.size(), productsWithQuantity);
            
            // Loguj produkty z quantity > 0 dla diagnostyki
            products.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .limit(5)
                .forEach(p -> logger.info("  📊 {} ({}): quantity={}", 
                    p.getName(), p.getMapperName(), p.getQuantity()));
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            logger.error("Błąd wypełniania ilości ze snapshotu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } */

    /**
     * Generuje PDF oferty na podstawie projektu
     * @param id ID projektu
     * @param templateId ID szablonu (opcjonalne - jeśli null, użyje domyślnego lub starego CreateOffer)
     */
    @PostMapping("/{id}/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId) {
        try {
            Project project = projectService.getProjectById(id);
            logger.info("Generowanie PDF dla projektu ID: {} (szablon ID: {})", 
                project.getId(), templateId);
            
            byte[] pdfBytes;
            
            // Jeśli podano templateId, użyj nowego systemu szablonów
            if (templateId != null) {
                pdfBytes = offerPdfService.generatePdfFromTemplate(project, templateId);
            } else {
                // Spróbuj użyć domyślnego szablonu
                try {
                    logger.info("Brak templateId - próba użycia domyślnego szablonu");
                    pdfBytes = offerPdfService.generatePdfFromTemplate(project, null);
                } catch (IllegalStateException e) {
                    // Jeśli nie ma domyślnego szablonu, użyj starego systemu jako fallback
                    logger.warn("Brak domyślnego szablonu - używanie starego systemu (CreateOffer): {}", e.getMessage());
                    createOffer.createOffer(project);
                    
                    // Odczytaj plik PDF
                    Path pdfPath = Paths.get("src/main/resources/templates/CommercialOffer.pdf");
                    pdfBytes = Files.readAllBytes(pdfPath);
                }
            }
            
            // Zwróć PDF jako response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "Oferta_" + (project.getClient() != null ? project.getClient().getSurname() : "Projekt") + "_" + 
                              LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            headers.setContentDispositionFormData("filename", filename);
            
            logger.info("PDF wygenerowany pomyślnie dla projektu {}", project.getId());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            logger.error("Błąd podczas generowania PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Błąd podczas generowania PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


