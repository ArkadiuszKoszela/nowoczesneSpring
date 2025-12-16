package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.DTO.*;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.ProductRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectDraftChangeRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectDraftInputRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectProductGroupRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectProductRepository;
import pl.koszela.nowoczesnebud.Repository.ProjectRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serwis do zarządzania projektami
 * ZAWSZE tworzy nowy projekt (jeśli brak ID), lub aktualizuje istniejący
 * 
 * Zapisane ceny i ilości produktów są w ProjectProduct (nie w snapshotach)
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectProductRepository projectProductRepository;
    private final ProjectProductGroupRepository projectProductGroupRepository;
    private final ProductRepository productRepository;
    private final ProjectDraftChangeRepository projectDraftChangeRepository;
    private final ProjectDraftInputRepository projectDraftInputRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ProjectService(ProjectRepository projectRepository, 
                         UserRepository userRepository,
                         ProjectProductRepository projectProductRepository,
                         ProjectProductGroupRepository projectProductGroupRepository,
                         ProductRepository productRepository,
                         ProjectDraftChangeRepository projectDraftChangeRepository,
                         ProjectDraftInputRepository projectDraftInputRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectProductRepository = projectProductRepository;
        this.projectProductGroupRepository = projectProductGroupRepository;
        this.productRepository = productRepository;
        this.projectDraftChangeRepository = projectDraftChangeRepository;
        this.projectDraftInputRepository = projectDraftInputRepository;
    }

    /**
     * Zapisuje projekt (przeciążona metoda bez formInputsToSave)
     * Zawsze tworzy nowy (jeśli brak ID) lub aktualizuje istniejący
     * Zapisuje Input z formularza + ProjectProduct (zapisane ceny) + ProjectProductGroup (opcje grup)
     */
    @Transactional
    public Project save(Project project) {
        return save(project, null);
    }
    
    /**
     * Zapisuje projekt z opcjonalną listą Input z formularza
     * Jeśli formInputsToSave != null, używa jej zamiast project.getInputs()
     */
    @Transactional
    public Project save(Project project, List<Input> formInputsToSave) {
        logger.info("💾 Zapisywanie projektu ID: {}", project.getId() != null ? project.getId() : "nowy");
        
        // Jeśli projekt ma ID - aktualizuj istniejący
        if (project.getId() != null) {
            logger.info("  Aktualizacja istniejącego projektu ID: {}", project.getId());
            Project existingProject = projectRepository.findById(project.getId())
                .orElseThrow(() -> new RuntimeException("Project not found: " + project.getId()));
            
            // Aktualizuj dane projektu
            existingProject.setStatus(project.getStatus());
            existingProject.setClient(project.getClient());
            
            // Aktualizuj rabaty globalne dla zakładek
            existingProject.setTilesMargin(project.getTilesMargin());
            existingProject.setTilesDiscount(project.getTilesDiscount());
            existingProject.setGuttersMargin(project.getGuttersMargin());
            existingProject.setGuttersDiscount(project.getGuttersDiscount());
            existingProject.setAccessoriesMargin(project.getAccessoriesMargin());
            existingProject.setAccessoriesDiscount(project.getAccessoriesDiscount());
            
            // ⚠️ WAŻNE: Modyfikuj istniejącą kolekcję zamiast tworzenia nowej referencji
            // orphanRemoval=true wymaga abyśmy modyfikowali istniejącą kolekcję, nie zastępowali jej
            
            // Wyczyść istniejące Input (orphanRemoval usunie je z bazy)
            existingProject.getInputs().clear();
            
            // ⚠️ WAŻNE: Przesyłamy WSZYSTKIE Input z formularza
            // Użyj formInputsToSave jeśli jest podana, w przeciwnym razie użyj project.getInputs()
            List<Input> inputsToProcess = formInputsToSave != null ? formInputsToSave :
                (project.getInputs() != null ? project.getInputs() : new ArrayList<>());
            
            if (formInputsToSave != null) {
                logger.debug("📥 Używam {} Input przekazanych bezpośrednio", inputsToProcess.size());
            } else if (project.getInputs() != null) {
                logger.debug("📥 Otrzymano {} Input w request", project.getInputs().size());
            }
            
            if (!inputsToProcess.isEmpty()) {
                for (Input input : inputsToProcess) {
                    // ⚠️ WAŻNE: Wyczyść ID - zawsze tworzymy NOWE Input przy zapisie
                    input.setId(null);
                    
                    // Przypisz do projektu
                    input.setProject(existingProject);
                    
                    // Normalizuj quantity dla Input z formularza: null → 0.0
                    if (input.getQuantity() == null) {
                        input.setQuantity(0.0);
                    }
                    
                    logger.debug("  📝 Input z formularza: '{}' (mapperName: '{}', quantity: {})", 
                               input.getName(), input.getMapperName(), input.getQuantity());
                    
                    // ⚠️ WAŻNE: Dodaj do istniejącej kolekcji (nie tworz nowej referencji)
                    existingProject.getInputs().add(input);
                }
                
                logger.info("💾 Zapisywanie projektu: {} Input z formularza", inputsToProcess.size());
            } else {
                logger.warn("⚠️ Brak Input do zapisania - inputsToProcess jest puste");
            }
            
            // Zapisz projekt
            Project savedProject = projectRepository.save(existingProject);
            logger.info("✅ Zaktualizowano projekt ID: {}", savedProject.getId());
            
            // ⚠️ WAŻNE: Nie wywołuj findByIdWithClientAndInputs() jeśli nie jest potrzebne
            // To może powodować StaleStateException jeśli Input zostały już usunięte przez orphanRemoval
            // Zwróć zapisany projekt bezpośrednio - Input są już w kolekcji savedProject.getInputs()
            return savedProject;
        }
        
        // Nowy projekt - upewnij się że client jest zapisany w bazie
        User client = project.getClient();
        if (client != null && client.getId() == 0) {
            logger.info("  Zapisywanie nowego klienta");
            client = userRepository.save(client);
            project.setClient(client);
        }
        
        // ⚠️ WAŻNE: OneToOne - sprawdź czy klient już ma projekt
        // Jeśli tak, zaktualizuj istniejący zamiast tworzyć nowy
        if (client != null && client.getId() != 0) {
            Optional<Project> existingProjectForClient = projectRepository.findByClientId(client.getId());
            if (existingProjectForClient.isPresent()) {
                logger.info("  Klient już ma projekt (ID: {}), aktualizuję istniejący zamiast tworzyć nowy", 
                           existingProjectForClient.get().getId());
                // Ustaw ID istniejącego projektu, aby zaktualizować zamiast tworzyć nowy
                project.setId(existingProjectForClient.get().getId());
                // Przejdź do logiki aktualizacji (powyżej)
                return save(project, formInputsToSave);
            }
        }
        
        // Ustaw status domyślny jeśli nie ma
        if (project.getStatus() == null) {
            project.setStatus(Project.ProjectStatus.DRAFT);
        }
        
        // Inicjalizuj rabaty jeśli null
        if (project.getTilesMargin() == null) project.setTilesMargin(0.0);
        if (project.getTilesDiscount() == null) project.setTilesDiscount(0.0);
        if (project.getGuttersMargin() == null) project.setGuttersMargin(0.0);
        if (project.getGuttersDiscount() == null) project.setGuttersDiscount(0.0);
        if (project.getAccessoriesMargin() == null) project.setAccessoriesMargin(0.0);
        if (project.getAccessoriesDiscount() == null) project.setAccessoriesDiscount(0.0);
        
            // ⚠️ WAŻNE: Wszystkie Input są teraz z formularza
            if (project.getInputs() != null && !project.getInputs().isEmpty()) {
                logger.info("  Przetwarzanie {} Input z formularza", project.getInputs().size());
                
                for (Input input : project.getInputs()) {
                    // ⚠️ WAŻNE: Wyczyść ID - zawsze tworzymy NOWE Input przy zapisie
                    input.setId(null);
                    
                    input.setProject(project);
                    
                    // Normalizuj quantity dla Input z formularza: null → 0.0
                    if (input.getQuantity() == null) {
                        input.setQuantity(0.0);
                    }
                    
                    logger.debug("  📝 Input z formularza: '{}' (mapperName: '{}', quantity: {})", 
                               input.getName(), input.getMapperName(), input.getQuantity());
                }
            }
        
        // Zapisz projekt
        Project savedProject = projectRepository.save(project);
        logger.info("✅ Utworzono nowy projekt ID: {}", savedProject.getId());
        
        // Zwróć projekt z załadowanymi inputami (używając JOIN FETCH)
        return projectRepository.findByIdWithClientAndInputs(savedProject.getId())
            .orElse(savedProject);
    }

    /**
     * Pobiera wszystkie projekty z załadowanymi relacjami
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAllWithClient();
    }

    /**
     * Pobiera projekt dla danego klienta (OneToOne - jeden klient ma jeden projekt)
     */
    public Optional<Project> getProjectByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    /**
     * Pobiera projekt po ID z załadowanym klientem i inputami
     */
    public Project getProjectById(Long id) {
        return projectRepository.findByIdWithClientAndInputs(id)
            .orElseThrow(() -> new RuntimeException("Project not found: " + id));
    }

    /**
     * Aktualizuje dane klienta (User)
     */
    @Transactional
    public User updateClient(User client) {
        if (client == null || client.getId() == 0L) {
            throw new IllegalArgumentException("Klient musi mieć ID");
        }
        
        logger.info("🔄 Aktualizacja klienta ID: {}", client.getId());
        
        Optional<User> existingUserOpt = userRepository.findById(client.getId());
        if (!existingUserOpt.isPresent()) {
            throw new RuntimeException("Klient nie istnieje: " + client.getId());
        }
        
        User existingUser = existingUserOpt.get();
        
        // Aktualizuj dane klienta
        existingUser.setName(client.getName());
        existingUser.setSurname(client.getSurname());
        existingUser.setEmail(client.getEmail());
        existingUser.setTelephoneNumber(client.getTelephoneNumber());
        
        // Aktualizuj adres jeśli jest podany
        if (client.getAddress() != null) {
            if (existingUser.getAddress() == null) {
                existingUser.setAddress(client.getAddress());
            } else {
                // Aktualizuj istniejący adres
                existingUser.getAddress().setAddress(client.getAddress().getAddress());
                existingUser.getAddress().setLongitude(client.getAddress().getLongitude());
                existingUser.getAddress().setLatitude(client.getAddress().getLatitude());
                existingUser.getAddress().setZoom(client.getAddress().getZoom());
            }
        }
        
        existingUser.setDateOfMeeting(client.getDateOfMeeting());
        
        User savedUser = userRepository.save(existingUser);
        
        // ⚠️ WAŻNE: Wymuś flush i clear cache, aby projekty odświeżyły dane klienta
        // To zapewni, że getAllProjects() zwróci projekty z zaktualizowanymi danymi klienta
        userRepository.flush();
        entityManager.clear(); // Wyczyść cache JPA, aby wymusić odświeżenie danych klienta w projektach
        
        logger.info("✅ Zaktualizowano klienta ID: {}", savedUser.getId());
        logger.info("📋 Zaktualizowane dane: imię={}, nazwisko={}, email={}", 
                   savedUser.getName(), savedUser.getSurname(), savedUser.getEmail());
        
        return savedUser;
    }

    /**
     * Pobiera wszystkich klientów (User)
     */
    public List<User> getAllClients() {
        logger.info("📋 Pobieranie wszystkich klientów");
        List<User> clients = userRepository.findAll();
        logger.info("✅ Znaleziono {} klientów", clients.size());
        return clients;
    }

    /**
     * Usuwa klienta (User) wraz z wszystkimi jego projektami i powiązanymi danymi
     * Usuwa również draft changes i draft inputs, które nie są automatycznie usuwane przez cascade
     */
    @Transactional
    public void deleteUser(Long userId) {
        logger.info("🗑️ Usuwanie klienta ID: {}", userId);
        
        // Sprawdź czy klient istnieje
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            logger.warn("⚠️ Klient ID {} nie istnieje", userId);
            throw new RuntimeException("Klient nie istnieje: " + userId);
        }
        
        User user = userOpt.get();
        
        // Znajdź projekt klienta (OneToOne - jeden klient ma jeden projekt)
        Optional<Project> userProjectOpt = projectRepository.findByClientId(userId);
        
        // Usuń projekt klienta jeśli istnieje
        if (userProjectOpt.isPresent()) {
            Project project = userProjectOpt.get();
            Long projectId = project.getId();
            
            logger.info("  Znaleziono projekt ID {} dla klienta, usuwanie powiązanych danych...", projectId);
            
            // ⚠️ WAŻNE: Usuń draft changes (nie są automatycznie usuwane przez cascade, bo nie mają relacji JPA)
            List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectId(projectId);
            if (!draftChanges.isEmpty()) {
                logger.info("  Usuwanie {} draft changes...", draftChanges.size());
                projectDraftChangeRepository.deleteByProjectId(projectId);
                logger.info("  ✓ Draft changes usunięte");
            }
            
            // ⚠️ WAŻNE: Usuń draft inputs (nie są automatycznie usuwane przez cascade, bo nie mają relacji JPA)
            List<ProjectDraftInput> draftInputs = projectDraftInputRepository.findByProjectId(projectId);
            if (!draftInputs.isEmpty()) {
                logger.info("  Usuwanie {} draft inputs...", draftInputs.size());
                projectDraftInputRepository.deleteByProjectId(projectId);
                logger.info("  ✓ Draft inputs usunięte");
            }
            
            // Usuń projekt (to automatycznie usunie też Input, ProjectProduct, ProjectProductGroup przez cascade)
            projectRepository.delete(project);
            logger.info("  ✓ Projekt usunięty (wraz z Input, ProjectProduct, ProjectProductGroup)");
        } else {
            logger.info("  Klient nie ma projektu");
        }
        
        // Usuń klienta
        userRepository.delete(user);
        logger.info("✅ Klient ID {} został usunięty wraz z wszystkimi powiązanymi danymi", userId);
    }

    /**
     * Usuwa projekt
     */
    @Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    /**
     * Zapisuje dane produktów i grup dla projektu
     * Wywołane podczas kliknięcia "Zapisz projekt" w frontendzie
     * 
     * NOWA LOGIKA (z Draft Changes):
     * 1. Przenieś wszystkie draft changes do ProjectProduct
     * 2. Usuń draft changes
     * 3. Zapisz dane z request (jeśli są)
     */
    @Transactional
    public void saveProjectData(Long projectId, SaveProjectDataRequest request) {
        logger.info("💾 Zapisywanie danych projektu ID: {}", projectId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        // 1. Aktualizuj globalne rabaty
        project.setTilesMargin(request.getTilesMargin());
        project.setTilesDiscount(request.getTilesDiscount());
        project.setGuttersMargin(request.getGuttersMargin());
        project.setGuttersDiscount(request.getGuttersDiscount());
        project.setAccessoriesMargin(request.getAccessoriesMargin());
        project.setAccessoriesDiscount(request.getAccessoriesDiscount());
        
        // 2. NOWE: Przenieś draft changes do ProjectProduct
        List<ProjectDraftChange> allDraftChanges = projectDraftChangeRepository.findByProjectId(projectId);
        if (!allDraftChanges.isEmpty()) {
            logger.info("  Przenoszenie {} draft changes do ProjectProduct", allDraftChanges.size());
            
            // Pobierz istniejące ProjectProduct jako mapę
            // ⚠️ Obsługa duplikatów: jeśli są duplikaty (productId + category), wybierz najnowszy (większe id)
            Map<String, ProjectProduct> existingProductsMap = project.getProjectProducts().stream()
                .collect(Collectors.toMap(
                    pp -> pp.getProductId() + "_" + pp.getCategory(), 
                    pp -> pp,
                    (existing, replacement) -> {
                        // Jeśli istnieje duplikat, wybierz ten z większym id (nowszy)
                        if (replacement.getId() != null && existing.getId() != null) {
                            return replacement.getId() > existing.getId() ? replacement : existing;
                        }
                        return replacement; // Jeśli brak id, użyj nowego
                    }
                ));
            
            for (ProjectDraftChange draft : allDraftChanges) {
                String key = draft.getProductId() + "_" + draft.getCategory();
                ProjectProduct pp = existingProductsMap.get(key);
                
                if (pp == null) {
                    // Stwórz nowy ProjectProduct
                    pp = new ProjectProduct();
                    pp.setProject(project);
                    pp.setProductId(draft.getProductId());
                    
                    // Konwertuj String category z draft na ProductCategory enum
                    try {
                        pp.setCategory(ProductCategory.valueOf(draft.getCategory()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("    Nieprawidłowa kategoria w draft: {}", draft.getCategory());
                        continue; // Pomiń ten draft change
                    }
                    
                    project.getProjectProducts().add(pp);
                    logger.debug("    Utworzono nowy ProjectProduct dla produktu ID: {}", draft.getProductId());
                } else {
                    logger.debug("    Aktualizacja istniejącego ProjectProduct dla produktu ID: {}", draft.getProductId());
                }
                
                // ⚠️ WAŻNE: Przenieś dane z draft do ProjectProduct - BEZWARUNKOWE KOPIOWANIE
                // Kopiuj dokładnie wartości z draft changes, nawet jeśli są null
                // To zapewnia, że ręczne zmiany nadpisują wartości z marży/rabatu
                pp.setSavedRetailPrice(draft.getDraftRetailPrice());
                pp.setSavedPurchasePrice(draft.getDraftPurchasePrice());
                pp.setSavedSellingPrice(draft.getDraftSellingPrice());
                pp.setSavedQuantity(draft.getDraftQuantity());
                pp.setSavedMarginPercent(draft.getDraftMarginPercent());
                pp.setSavedDiscountPercent(draft.getDraftDiscountPercent());
                if (draft.getPriceChangeSource() != null && !draft.getPriceChangeSource().isEmpty()) {
                    try {
                        pp.setPriceChangeSource(PriceChangeSource.valueOf(draft.getPriceChangeSource()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("    Nieprawidłowe priceChangeSource w draft: {}", draft.getPriceChangeSource());
                    }
                }
            }
            
            // Usuń draft changes po przeniesieniu
            projectDraftChangeRepository.deleteByProjectId(projectId);
            logger.info("  ✓ Draft changes przeniesione i usunięte");
            
            // 2a.1. Przenieś opcje grup z draft changes do ProjectProductGroup
            // Grupuj draft changes po manufacturer + groupName (pobierane z Product przez productId)
            // ⚠️ WAŻNE: Obsługuj productId = 0 (z importu) - wtedy szukaj produktów po manufacturer i groupName
            Map<String, ProjectDraftChange> groupOptionsMap = new java.util.HashMap<>();
            for (ProjectDraftChange draft : allDraftChanges) {
                if (draft.getDraftIsMainOption() != null && draft.getDraftIsMainOption() != GroupOption.NONE) {
                    String groupKey = null;
                    String manufacturer = null;
                    String groupName = null;
                    
                    if (draft.getProductId() != null && draft.getProductId() > 0) {
                        // Normalny przypadek: productId > 0 - pobierz Product
                        Optional<Product> productOpt = productRepository.findById(draft.getProductId());
                        if (productOpt.isPresent()) {
                            Product product = productOpt.get();
                            manufacturer = product.getManufacturer();
                            groupName = product.getGroupName();
                            if (manufacturer != null && groupName != null) {
                                groupKey = manufacturer + "_" + groupName + "_" + draft.getCategory();
                            }
                        }
                    } else if (draft.getProductId() != null && draft.getProductId() == 0) {
                        // ⚠️ WAŻNE: productId = 0 oznacza import - znajdź wszystkie produkty z danym manufacturer i groupName
                        // (ale nie mamy tych pól w draft changes, więc musimy je znaleźć przez Product)
                        // W tym przypadku opcje grup są już zapisane dla wszystkich produktów w grupie,
                        // więc możemy użyć pierwszego produktu z tej grupy
                        List<Product> productsInGroup = productRepository.findByCategory(ProductCategory.valueOf(draft.getCategory()));
                        for (Product p : productsInGroup) {
                            if (p.getManufacturer() != null && p.getGroupName() != null) {
                                // Sprawdź czy ten produkt ma draft change z draftIsMainOption
                                // (wszystkie produkty w grupie mają tę samą opcję)
                                Optional<ProjectDraftChange> groupDraftOpt = allDraftChanges.stream()
                                    .filter(dc -> dc.getProductId() != null && dc.getProductId().equals(p.getId()) &&
                                                  dc.getDraftIsMainOption() != null &&
                                                  dc.getCategory().equals(draft.getCategory()))
                                    .findFirst();
                                if (groupDraftOpt.isPresent()) {
                                    manufacturer = p.getManufacturer();
                                    groupName = p.getGroupName();
                                    if (manufacturer != null && groupName != null) {
                                        groupKey = manufacturer + "_" + groupName + "_" + draft.getCategory();
                                    }
                                    break; // Znaleziono - użyj tego produktu
                                }
                            }
                        }
                    }
                    
                    // Zapisz opcję grupy (użyj pierwszej znalezionej dla danej grupy)
                    if (groupKey != null && !groupOptionsMap.containsKey(groupKey)) {
                        groupOptionsMap.put(groupKey, draft);
                    }
                }
            }
            
            // Utwórz ProjectProductGroup z opcji grup
            if (!groupOptionsMap.isEmpty()) {
                logger.info("  Przenoszenie {} opcji grup z draft changes do ProjectProductGroup", groupOptionsMap.size());
                for (Map.Entry<String, ProjectDraftChange> entry : groupOptionsMap.entrySet()) {
                    ProjectDraftChange draft = entry.getValue();
                    String manufacturer = null;
                    String groupName = null;
                    
                    if (draft.getProductId() != null && draft.getProductId() > 0) {
                        // Normalny przypadek: productId > 0
                        Optional<Product> productOpt = productRepository.findById(draft.getProductId());
                        if (productOpt.isPresent()) {
                            Product product = productOpt.get();
                            manufacturer = product.getManufacturer();
                            groupName = product.getGroupName();
                        }
                    } else if (draft.getProductId() != null && draft.getProductId() == 0) {
                        // ⚠️ WAŻNE: productId = 0 - znajdź pierwszy produkt z tej grupy
                        List<Product> productsInGroup = productRepository.findByCategory(ProductCategory.valueOf(draft.getCategory()));
                        for (Product p : productsInGroup) {
                            if (p.getManufacturer() != null && p.getGroupName() != null) {
                                Optional<ProjectDraftChange> groupDraftOpt = allDraftChanges.stream()
                                    .filter(dc -> dc.getProductId() != null && dc.getProductId().equals(p.getId()) &&
                                                  dc.getDraftIsMainOption() != null &&
                                                  dc.getCategory().equals(draft.getCategory()))
                                    .findFirst();
                                if (groupDraftOpt.isPresent()) {
                                    manufacturer = p.getManufacturer();
                                    groupName = p.getGroupName();
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (manufacturer != null && groupName != null) {
                        ProjectProductGroup ppg = new ProjectProductGroup();
                        ppg.setProject(project);
                        ppg.setCategory(ProductCategory.valueOf(draft.getCategory()));
                        ppg.setManufacturer(manufacturer);
                        ppg.setGroupName(groupName);
                        ppg.setIsMainOption(draft.getDraftIsMainOption());
                        
                        project.getProjectProductGroups().add(ppg);
                        logger.debug("    Utworzono ProjectProductGroup: {} - {} (isMainOption: {})", 
                                   manufacturer, groupName, draft.getDraftIsMainOption());
                    }
                }
                logger.info("  ✓ Opcje grup przeniesione z draft changes");
            }
        }
        
        // 2b. NOWE: Przenieś draft inputs do Input
        List<ProjectDraftInput> allDraftInputs = projectDraftInputRepository.findByProjectId(projectId);
        if (!allDraftInputs.isEmpty()) {
            logger.info("  Przenoszenie {} draft inputs do Input", allDraftInputs.size());
            
            // Wyczyść istniejące Input (orphanRemoval usunie je z bazy)
            project.getInputs().clear();
            
            // Przenieś draft inputs do Input
            for (ProjectDraftInput draftInput : allDraftInputs) {
                Input input = new Input();
                input.setProject(project);
                input.setMapperName(draftInput.getMapperName());
                input.setName(draftInput.getName());
                input.setQuantity(draftInput.getQuantity());
                
                project.getInputs().add(input);
            }
            
            // Usuń draft inputs po przeniesieniu
            projectDraftInputRepository.deleteByProjectId(projectId);
            logger.info("  ✓ Draft inputs przeniesione i usunięte");
        }
        
        // 3. ⚠️ WAŻNE: NIE nadpisuj produktów z request - draft changes mają priorytet!
        // Produkty z request są używane tylko do dodatkowych aktualizacji (np. productGroups)
        // Ale wartości z draft changes (skopiowane w sekcji 2) mają najwyższy priorytet
        // Jeśli request zawiera produkty, które nie są w draft changes, dodaj je
        // Ale NIE nadpisuj produktów, które już zostały skopiowane z draft changes
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            logger.info("  Sprawdzanie {} produktów z request (dodanie tylko tych, które nie są w draft changes)", request.getProducts().size());
            
            // Utwórz mapę produktów już skopiowanych z draft changes
            Map<String, ProjectProduct> draftProductsMap = project.getProjectProducts().stream()
                .collect(Collectors.toMap(
                    pp -> pp.getProductId() + "_" + pp.getCategory(),
                    pp -> pp
                ));
            
            // Dodaj tylko produkty, które nie są w draft changes
            for (SaveProjectProductDTO dto : request.getProducts()) {
                String key = dto.getProductId() + "_" + dto.getCategory();
                if (!draftProductsMap.containsKey(key)) {
                    // Produkt nie jest w draft changes - dodaj z request
                    ProjectProduct pp = new ProjectProduct();
                    pp.setProject(project);
                    pp.setProductId(dto.getProductId());
                    pp.setCategory(dto.getCategory());
                    pp.setSavedRetailPrice(dto.getSavedRetailPrice());
                    pp.setSavedPurchasePrice(dto.getSavedPurchasePrice());
                    pp.setSavedSellingPrice(dto.getSavedSellingPrice());
                    pp.setSavedQuantity(dto.getSavedQuantity());
                    pp.setPriceChangeSource(dto.getPriceChangeSource());
                    pp.setSavedMarginPercent(dto.getSavedMarginPercent());
                    pp.setSavedDiscountPercent(dto.getSavedDiscountPercent());
                    
                    project.getProjectProducts().add(pp);
                    logger.debug("    Dodano produkt z request (nie był w draft changes): productId={}, category={}", dto.getProductId(), dto.getCategory());
                } else {
                    logger.debug("    Pomiń produkt z request (już jest w draft changes): productId={}, category={}", dto.getProductId(), dto.getCategory());
                }
            }
        }
        
        // 4. Usuń stare ProjectProductGroup
        project.getProjectProductGroups().clear();
        entityManager.flush(); // Wymuś usunięcie przed dodaniem nowych
        
        // 5. Dodaj nowe ProjectProductGroup z request
        if (request.getProductGroups() != null && !request.getProductGroups().isEmpty()) {
            logger.info("  Zapisywanie {} grup produktowych", request.getProductGroups().size());
            for (SaveProjectProductGroupDTO dto : request.getProductGroups()) {
                ProjectProductGroup ppg = new ProjectProductGroup();
                ppg.setProject(project);
                ppg.setCategory(dto.getCategory());
                ppg.setManufacturer(dto.getManufacturer());
                ppg.setGroupName(dto.getGroupName());
                ppg.setIsMainOption(dto.getIsMainOption());
                
                project.getProjectProductGroups().add(ppg);
            }
        }
        
        // 6. Zapisz projekt z nowymi danymi
        projectRepository.save(project);
        
        logger.info("✅ Zapisano dane projektu ID: {}", projectId);
    }
    
    /**
     * Pobiera zapisane dane produktów dla projektu
     * Zwraca listę ProjectProductDTO (zapisane ceny i ilości)
     */
    public List<ProjectProductDTO> getProjectProducts(Long projectId, ProductCategory category) {
        logger.info("📋 Pobieranie produktów projektu ID: {}, kategoria: {}", projectId, category);
        
        List<ProjectProduct> projectProducts = projectProductRepository.findByProjectIdAndCategory(projectId, category);
        
        return projectProducts.stream()
            .map(pp -> new ProjectProductDTO(
                pp.getProductId(),
                pp.getCategory(),
                pp.getSavedRetailPrice(),
                pp.getSavedPurchasePrice(),
                pp.getSavedSellingPrice(),
                pp.getSavedQuantity(),
                pp.getPriceChangeSource(),
                pp.getSavedMarginPercent(),
                pp.getSavedDiscountPercent()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Pobiera zapisane opcje grup produktowych dla projektu
     */
    public List<ProjectProductGroupDTO> getProjectProductGroups(Long projectId, ProductCategory category) {
        logger.info("📋 Pobieranie grup produktowych projektu ID: {}, kategoria: {}", projectId, category);
        
        List<ProjectProductGroup> groups = projectProductGroupRepository.findByProjectIdAndCategory(projectId, category);
        
        return groups.stream()
            .map(ppg -> new ProjectProductGroupDTO(
                ppg.getCategory(),
                ppg.getManufacturer(),
                ppg.getGroupName(),
                ppg.getIsMainOption()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Porównuje aktualne ceny z cennika z zapisanymi cenami w projekcie + draft changes
     * Zwraca listę ProductComparisonDTO (Stara vs Nowa cena)
     * Używane w zakładkach Dachówki/Rynny/Akcesoria
     * 
     * LOGIKA:
     * - "Stara cena" = zapisane dane z ProjectProduct (ostatni stan po "Zapisz projekt")
     * - "Nowa cena" = draft changes (jeśli istnieją) lub aktualne ceny z cennika
     */
    public List<ProductComparisonDTO> getProductComparison(Long projectId, ProductCategory category) {
        // 1. Pobierz wszystkie produkty z aktualnego cennika
        List<Product> currentProducts = productRepository.findByCategory(category);
        
        // 2. Pobierz zapisane dane z ProjectProduct (ostatni zapisany stan)
        List<ProjectProduct> savedProducts = projectProductRepository.findByProjectIdAndCategory(projectId, category);
        // ⚠️ Obsługa duplikatów: jeśli są duplikaty productId, wybierz najnowszy (większe id)
        Map<Long, ProjectProduct> savedProductsMap = savedProducts.stream()
            .collect(Collectors.toMap(
                ProjectProduct::getProductId, 
                pp -> pp,
                (existing, replacement) -> {
                    // Jeśli istnieje duplikat, wybierz ten z większym id (nowszy)
                    if (replacement.getId() != null && existing.getId() != null) {
                        return replacement.getId() > existing.getId() ? replacement : existing;
                    }
                    return replacement; // Jeśli brak id, użyj nowego
                }
            ));
        logger.info("  Znaleziono {} zapisanych produktów ({} unikalnych)", savedProducts.size(), savedProductsMap.size());
        
        // 3. Pobierz draft changes (tymczasowe, niezapisane zmiany)
        List<ProjectDraftChange> draftChanges = projectDraftChangeRepository.findByProjectIdAndCategory(projectId, category.name());
        // ⚠️ WAŻNE: Obsługa duplikatów - jeśli są duplikaty productId, wybierz najnowszy (większe id)
        // To zapobiega błędom "Duplicate key" gdy w bazie są duplikaty draft changes dla tego samego produktu
        Map<Long, ProjectDraftChange> draftChangesMap = draftChanges.stream()
            .collect(Collectors.toMap(
                ProjectDraftChange::getProductId, 
                dc -> dc,
                (existing, replacement) -> {
                    // Jeśli istnieje duplikat, wybierz ten z większym id (nowszy)
                    if (replacement.getId() != null && existing.getId() != null) {
                        return replacement.getId() > existing.getId() ? replacement : existing;
                    }
                    return replacement; // Jeśli brak id, użyj nowego
                }
            ));
        logger.info("  Znaleziono {} draft changes ({} unikalnych)", draftChanges.size(), draftChangesMap.size());
        
        // ⚠️ WAŻNE: Pobierz marżę/rabat kategorii z draft changes (wszystkie produkty mają tę samą wartość)
        // Używane do przywrócenia marży/rabatu w UI po odświeżeniu strony
        Double categoryDraftMargin = null;
        Double categoryDraftDiscount = null;
        if (!draftChanges.isEmpty()) {
            // Weź marżę/rabat z pierwszego draft change (wszystkie mają tę samą wartość)
            ProjectDraftChange firstDraft = draftChanges.get(0);
            categoryDraftMargin = firstDraft.getDraftMarginPercent();
            categoryDraftDiscount = firstDraft.getDraftDiscountPercent();
            logger.info("  Marża/rabat kategorii z draft: marża={}, rabat={}", categoryDraftMargin, categoryDraftDiscount);
        }
        
        // 3a. Pobierz opcje grup z ProjectProductGroup (zapisane opcje)
        List<ProjectProductGroup> productGroups = projectProductGroupRepository.findByProjectIdAndCategory(projectId, category);
        
        // ⚠️ WAŻNE: Mapuj opcje grup po manufacturer + groupName (klucz: "manufacturer_groupName")
        Map<String, GroupOption> savedGroupOptionsMap = productGroups.stream()
            .filter(ppg -> ppg.getIsMainOption() != null && ppg.getIsMainOption() != GroupOption.NONE)
            .collect(Collectors.toMap(
                ppg -> ppg.getManufacturer() + "_" + ppg.getGroupName(),
                ProjectProductGroup::getIsMainOption,
                (existing, replacement) -> replacement // Jeśli duplikat, użyj nowszego
            ));
        logger.info("  Znaleziono {} opcji grup (zapisane)", savedGroupOptionsMap.size());
        
        // 4. Połącz cennik + saved + draft i utwórz DTO
        List<ProductComparisonDTO> comparison = new ArrayList<>();
        
        for (Product current : currentProducts) {
            ProductComparisonDTO dto = new ProductComparisonDTO();
            
            // Podstawowe dane produktu
            dto.setProductId(current.getId());
            dto.setName(current.getName());
            dto.setManufacturer(current.getManufacturer());
            dto.setGroupName(current.getGroupName());
            dto.setCategory(current.getCategory());
            dto.setUnit(current.getUnit());
            dto.setQuantityConverter(current.getQuantityConverter());
            dto.setMapperName(current.getMapperName());
            
            // Typ akcesorium (tylko dla ACCESSORY)
            if (category == ProductCategory.ACCESSORY) {
                dto.setAccessoryType(current.getAccessoryType());
            }
            
            // Aktualne ceny z cennika
            dto.setCurrentRetailPrice(current.getRetailPrice());
            dto.setCurrentPurchasePrice(current.getPurchasePrice());
            
            // ⚠️ WAŻNE: Dla Akcesoriów, jeśli currentSellingPrice jest null, użyj currentPurchasePrice jako domyślnej
            // To zapewni, że zysk będzie poprawny (0 zamiast ujemnego)
            if (category == ProductCategory.ACCESSORY && current.getSellingPrice() == null) {
                dto.setCurrentSellingPrice(current.getPurchasePrice());
            } else {
                dto.setCurrentSellingPrice(current.getSellingPrice());
            }
            
            // Rabat z cennika
            dto.setDiscount(current.getDiscount());
            dto.setDiscountCalculationMethod(current.getDiscountCalculationMethod());
            dto.setMarginPercent(current.getMarginPercent());
            
            // ZAPISANE CENY (z ProjectProduct - ostatni zapisany stan)
            // To będzie "Stara cena" w UI
            ProjectProduct saved = savedProductsMap.get(current.getId());
            if (saved != null) {
                dto.setSavedRetailPrice(saved.getSavedRetailPrice());
                dto.setSavedPurchasePrice(saved.getSavedPurchasePrice());
                dto.setSavedSellingPrice(saved.getSavedSellingPrice());
                dto.setSavedQuantity(saved.getSavedQuantity());
                dto.setPriceChangeSource(saved.getPriceChangeSource());
                dto.setSavedMarginPercent(saved.getSavedMarginPercent());
                dto.setSavedDiscountPercent(saved.getSavedDiscountPercent());
                
                // Flagi zmian (porównaj zapisane vs aktualne)
                dto.setPriceChanged(!pricesEqual(saved.getSavedSellingPrice(), current.getSellingPrice()));
                dto.setQuantityChanged(saved.getSavedQuantity() != null && saved.getSavedQuantity() > 0);
            } else {
                // Brak zapisanych danych - to może być nowy produkt lub pierwsza edycja projektu
                dto.setSavedRetailPrice(null);
                dto.setSavedPurchasePrice(null);
                dto.setSavedSellingPrice(null);
                dto.setSavedQuantity(null);
                dto.setPriceChangeSource(PriceChangeSource.AUTO);
                dto.setPriceChanged(false);
                dto.setQuantityChanged(false);
            }
            
            // DRAFT CHANGES (tymczasowe, niezapisane zmiany)
            // To będzie "Nowa cena" w UI (jeśli draft istnieje)
            ProjectDraftChange draft = draftChangesMap.get(current.getId());
            if (draft != null) {
                dto.setDraftRetailPrice(draft.getDraftRetailPrice());
                dto.setDraftPurchasePrice(draft.getDraftPurchasePrice());
                dto.setDraftSellingPrice(draft.getDraftSellingPrice());
                dto.setDraftQuantity(draft.getDraftQuantity());
                dto.setDraftSelected(draft.getDraftSelected()); // ⚠️ WAŻNE: Odczytaj stan checkboxa dla akcesoriów
                dto.setDraftMarginPercent(draft.getDraftMarginPercent());
                dto.setDraftDiscountPercent(draft.getDraftDiscountPercent());
                
                // ⚠️ WAŻNE: Jeśli draft istnieje, to priceChangeSource z draftu ma priorytet
                // (użytkownik mógł zmienić źródło zmiany ceny)
                if (draft.getPriceChangeSource() != null && !draft.getPriceChangeSource().isEmpty()) {
                    try {
                        dto.setPriceChangeSource(PriceChangeSource.valueOf(draft.getPriceChangeSource()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("  Nieprawidłowe priceChangeSource w draft: {}", draft.getPriceChangeSource());
                    }
                }
            } else {
                // Brak draft changes - użyj null (frontend użyje currentPrice jako "Nowa cena")
                dto.setDraftRetailPrice(null);
                dto.setDraftPurchasePrice(null);
                dto.setDraftSellingPrice(null);
                dto.setDraftQuantity(null);
                dto.setDraftSelected(null);
                dto.setDraftMarginPercent(null);
                dto.setDraftDiscountPercent(null);
            }
            
            // ⚠️ WAŻNE: Ustaw marżę/rabat kategorii z draft changes (dla wszystkich produktów)
            dto.setCategoryDraftMarginPercent(categoryDraftMargin);
            dto.setCategoryDraftDiscountPercent(categoryDraftDiscount);
            
            // ⚠️ WAŻNE: Ustaw isMainOption z priorytetami:
            // 1. draftIsMainOption z draft changes (najwyższy priorytet - tymczasowe, niezapisane)
            //    - Jeśli draft istnieje i wszystkie inne pola są null, to to jest tylko aktualizacja opcji grupy
            //    - W takim przypadku użyj draftIsMainOption (może być NONE - "Nie wybrano")
            // 2. isMainOption z ProjectProductGroup (zapisane opcje)
            // 3. NONE (domyślnie - "Nie wybrano")
            GroupOption isMainOption = GroupOption.NONE;
            if (draft != null) {
                // Sprawdź, czy to jest tylko aktualizacja opcji grupy (wszystkie inne pola są null)
                boolean isOnlyGroupOptionUpdate = draft.getDraftRetailPrice() == null && 
                                                  draft.getDraftPurchasePrice() == null && 
                                                  draft.getDraftSellingPrice() == null && 
                                                  draft.getDraftQuantity() == null && 
                                                  draft.getDraftSelected() == null &&
                                                  draft.getDraftMarginPercent() == null &&
                                                  draft.getDraftDiscountPercent() == null;
                
                if (isOnlyGroupOptionUpdate) {
                    // To jest tylko aktualizacja opcji grupy - użyj draftIsMainOption (może być NONE - "Nie wybrano")
                    isMainOption = draft.getDraftIsMainOption() != null ? draft.getDraftIsMainOption() : GroupOption.NONE;
                } else if (draft.getDraftIsMainOption() != null && draft.getDraftIsMainOption() != GroupOption.NONE) {
                    // Jeśli są inne pola, użyj draftIsMainOption tylko jeśli nie jest NONE
                    isMainOption = draft.getDraftIsMainOption();
                }
            }
            
            // Priorytet 2: ProjectProductGroup (tylko jeśli nie ma draft changes z opcjami grup)
            if (isMainOption == GroupOption.NONE && current.getManufacturer() != null && current.getGroupName() != null) {
                String groupKey = current.getManufacturer() + "_" + current.getGroupName();
                GroupOption savedOption = savedGroupOptionsMap.get(groupKey);
                if (savedOption != null) {
                    isMainOption = savedOption;
                }
            }
            
            dto.setIsMainOption(isMainOption);
            
            comparison.add(dto);
        }
        
        return comparison;
    }
    
    /**
     * Pomocnicza metoda - porównuje ceny z tolerancją na błędy zaokrągleń
     */
    private boolean pricesEqual(Double price1, Double price2) {
        if (price1 == null && price2 == null) return true;
        if (price1 == null || price2 == null) return false;
        return Math.abs(price1 - price2) < 0.01;
    }

    // ==================== DRAFT CHANGES ====================
    
    /**
     * Zapisuje tymczasowe zmiany (draft changes) do bazy danych
     * Te zmiany są zapisywane w tabeli project_draft_changes_ws (workset)
     * i nie są jeszcze finalnie zapisane w project_products
     * 
     * @param projectId ID projektu
     * @param request Request zawierający listę draft changes
     */
    @Transactional
    public void saveDraftChanges(Long projectId, SaveDraftChangesRequest request) {
        // ⏱️ PERFORMANCE LOG: Start zapisu draft changes
        long saveStartTime = System.currentTimeMillis();
        int totalChanges = request.getChanges() != null ? request.getChanges().size() : 0;
        logger.info("⏱️ [PERFORMANCE] saveDraftChanges - START | projectId: {} | kategoria: {} | zmian: {}", 
                   projectId, request.getCategory(), totalChanges);
        
        if (request.getChanges() == null || request.getChanges().isEmpty()) {
            logger.info("⏱️ [PERFORMANCE] saveDraftChanges - END | Brak zmian do zapisania | czas: 0ms");
            return;
        }
        
        // 1. Usuń WSZYSTKIE stare draft changes dla tej kategorii
        long deleteStartTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] DELETE - START | projectId: {} | category: {}", projectId, request.getCategory());
        projectDraftChangeRepository.deleteByProjectIdAndCategory(projectId, request.getCategory());
        long deleteEndTime = System.currentTimeMillis();
        long deleteDuration = deleteEndTime - deleteStartTime;
        logger.info("⏱️ [PERFORMANCE] DELETE - END | czas: {}ms", deleteDuration);
        
        // 2. BATCH INSERT z EntityManager - optymalizacja dla nowych encji (bez ID)
        // Problem: saveAll() może nie używać batchowania dla nowych encji na niektórych bazach danych
        // Rozwiązanie: Użyj EntityManager.persist() w batchach z flush() i clear()
        long insertStartTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] BATCH INSERT - START | rekordów: {}", totalChanges);
        int batchSize = 1000; // Zgodne z hibernate.jdbc.batch_size
        int batchCount = 0;
        long prepareTime = 0;
        long persistTime = 0;
        long flushTime = 0;
        long lastBatchEndTime = insertStartTime;
        
        for (int i = 0; i < totalChanges; i++) {
            // Przygotowanie encji
            long prepareStart = System.currentTimeMillis();
            DraftChangeDTO dto = request.getChanges().get(i);
            ProjectDraftChange draft = new ProjectDraftChange();
            draft.setProjectId(projectId);
            draft.setProductId(dto.getProductId());
            draft.setCategory(dto.getCategory());
            
            // Ustaw wszystkie pola (zapisujemy pełny stan - nie sprawdzamy czy są null)
            draft.setDraftRetailPrice(dto.getDraftRetailPrice());
            draft.setDraftPurchasePrice(dto.getDraftPurchasePrice());
            draft.setDraftSellingPrice(dto.getDraftSellingPrice());
            draft.setDraftQuantity(dto.getDraftQuantity());
            draft.setDraftSelected(dto.getDraftSelected());
            draft.setDraftMarginPercent(dto.getDraftMarginPercent());
            draft.setDraftDiscountPercent(dto.getDraftDiscountPercent());
            if (dto.getPriceChangeSource() != null && !dto.getPriceChangeSource().isEmpty()) {
                draft.setPriceChangeSource(dto.getPriceChangeSource());
            }
            draft.setDraftIsMainOption(dto.getDraftIsMainOption());
            long prepareEnd = System.currentTimeMillis();
            long prepareDuration = prepareEnd - prepareStart;
            prepareTime += prepareDuration;
            
            // Persist encję (dodaje do kontekstu persistence)
            long persistStart = System.currentTimeMillis();
            entityManager.persist(draft);
            long persistEnd = System.currentTimeMillis();
            long persistDuration = persistEnd - persistStart;
            persistTime += persistDuration;
            
            // Loguj szczegóły dla pierwszych 10 rekordów i co 1000 rekordów, aby zobaczyć czy persist() jest wolne
            if (i < 10 || (i + 1) % 1000 == 0) {
                logger.debug("⏱️ [PERFORMANCE] Rekord {}/{} | prepare: {}ms | persist: {}ms", 
                           i + 1, totalChanges, prepareDuration, persistDuration);
            }
            
            // Loguj co 100 rekordów, aby zobaczyć postęp
            if ((i + 1) % 100 == 0) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastBatch = currentTime - lastBatchEndTime;
                long avgPersistTime = persistTime / (i + 1);
                logger.info("⏱️ [PERFORMANCE] Postęp: {}/{} rekordów | czas od ostatniego batcha: {}ms | prepare: {}ms | persist: {}ms | średni persist/rekord: {}ms", 
                           i + 1, totalChanges, timeSinceLastBatch, prepareTime, persistTime, avgPersistTime);
            }
            
            // Flush i clear co batchSize encji, aby wymusić batch INSERT i zwolnić pamięć
            if ((i + 1) % batchSize == 0 || (i + 1) == totalChanges) {
                long flushStart = System.currentTimeMillis();
                long timeSinceLastBatch = flushStart - lastBatchEndTime;
                logger.info("⏱️ [PERFORMANCE] Przed flush: Batch {}/{} | rekordów w batchu: {} | czas od ostatniego batcha: {}ms | prepare: {}ms | persist: {}ms", 
                           batchCount + 1, (int)Math.ceil((double)totalChanges / batchSize), 
                           (i + 1) % batchSize == 0 ? batchSize : ((i + 1) % batchSize),
                           timeSinceLastBatch, prepareTime, persistTime);
                
                entityManager.flush(); // Wymusza wykonanie INSERT-ów w batchu
                entityManager.clear(); // Czyści kontekst persistence, zwalnia pamięć
                
                long flushEnd = System.currentTimeMillis();
                long flushDuration = flushEnd - flushStart;
                flushTime += flushDuration;
                batchCount++;
                int recordsInBatch = (i + 1) % batchSize == 0 ? batchSize : ((i + 1) % batchSize);
                long batchTotalTime = flushEnd - lastBatchEndTime;
                lastBatchEndTime = flushEnd;
                
                logger.info("⏱️ [PERFORMANCE] Batch {}/{} zapisany | rekordów w batchu: {} | czas batcha: {}ms [prepare: {}ms, persist: {}ms, flush: {}ms]", 
                           batchCount, (int)Math.ceil((double)totalChanges / batchSize), recordsInBatch, 
                           batchTotalTime, prepareTime, persistTime, flushDuration);
                
                // Resetuj liczniki dla następnego batcha
                prepareTime = 0;
                persistTime = 0;
            }
        }
        
        long insertEndTime = System.currentTimeMillis();
        long insertDuration = insertEndTime - insertStartTime;
        logger.info("⏱️ [PERFORMANCE] BATCH INSERT - END | rekordów: {} | batchy: {} | czas całkowity: {}ms | prepare: {}ms | persist: {}ms | flush: {}ms", 
                   totalChanges, batchCount, insertDuration, prepareTime, persistTime, flushTime);
        
        // ⏱️ PERFORMANCE LOG: Koniec zapisu
        long saveEndTime = System.currentTimeMillis();
        long totalDuration = saveEndTime - saveStartTime;
        logger.info("⏱️ [PERFORMANCE] saveDraftChanges - END | projectId: {} | kategoria: {} | zmian: {} | czas całkowity: {}ms [DELETE: {}ms, INSERT: {}ms]", 
                   projectId, request.getCategory(), totalChanges, totalDuration, deleteDuration, insertDuration);
    }
    
    /**
     * Zapisuje pojedynczą zmianę draft dla produktu (UPSERT - update jeśli istnieje, insert jeśli nie)
     * Używane do szybkiej aktualizacji pojedynczego produktu (np. zmiana wariantu oferty)
     * 
     * @param projectId ID projektu
     * @param dto Draft change do zapisania
     */
    @Transactional
    public void saveSingleDraftChange(Long projectId, DraftChangeDTO dto) {
        long startTime = System.currentTimeMillis();
        logger.info("⚡ [PERFORMANCE] saveSingleDraftChange - START | projectId: {} | productId: {} | category: {}", 
                   projectId, dto.getProductId(), dto.getCategory());
        
        // UPSERT: znajdź istniejący draft change dla tego produktu
        long findStartTime = System.currentTimeMillis();
        Optional<ProjectDraftChange> existingOpt = projectDraftChangeRepository
            .findByProjectIdAndProductIdAndCategory(projectId, dto.getProductId(), dto.getCategory());
        long findEndTime = System.currentTimeMillis();
        long findDuration = findEndTime - findStartTime;
        
        ProjectDraftChange draft;
        boolean isUpdate = false;
        if (existingOpt.isPresent()) {
            // UPDATE istniejącego
            draft = existingOpt.get();
            isUpdate = true;
            logger.debug("⚡ [PERFORMANCE] Znaleziono istniejący draft change (ID: {}) | czas wyszukiwania: {}ms", draft.getId(), findDuration);
        } else {
            // INSERT nowego
            draft = new ProjectDraftChange();
            draft.setProjectId(projectId);
            draft.setProductId(dto.getProductId());
            draft.setCategory(dto.getCategory());
            logger.debug("⚡ [PERFORMANCE] Tworzenie nowego draft change | czas wyszukiwania: {}ms", findDuration);
        }
        
        // Ustaw wszystkie pola (pełny stan)
        long setFieldsStartTime = System.currentTimeMillis();
        draft.setDraftRetailPrice(dto.getDraftRetailPrice());
        draft.setDraftPurchasePrice(dto.getDraftPurchasePrice());
        draft.setDraftSellingPrice(dto.getDraftSellingPrice());
        draft.setDraftQuantity(dto.getDraftQuantity());
        draft.setDraftSelected(dto.getDraftSelected());
        draft.setDraftMarginPercent(dto.getDraftMarginPercent());
        draft.setDraftDiscountPercent(dto.getDraftDiscountPercent());
        if (dto.getPriceChangeSource() != null && !dto.getPriceChangeSource().isEmpty()) {
            draft.setPriceChangeSource(dto.getPriceChangeSource());
        }
        draft.setDraftIsMainOption(dto.getDraftIsMainOption());
        long setFieldsEndTime = System.currentTimeMillis();
        long setFieldsDuration = setFieldsEndTime - setFieldsStartTime;
        
        // Zapisz (save działa jako INSERT lub UPDATE w zależności od tego czy encja ma ID)
        long saveStartTime = System.currentTimeMillis();
        projectDraftChangeRepository.save(draft);
        long saveEndTime = System.currentTimeMillis();
        long saveDuration = saveEndTime - saveStartTime;
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        logger.info("⚡ [PERFORMANCE] saveSingleDraftChange - END | projectId: {} | productId: {} | operacja: {} | czas całkowity: {}ms [find: {}ms, setFields: {}ms, save: {}ms]", 
                   projectId, dto.getProductId(), isUpdate ? "UPDATE" : "INSERT", totalDuration, findDuration, setFieldsDuration, saveDuration);
    }
    
    /**
     * Pobiera draft changes dla projektu (opcjonalnie filtrowane po kategorii)
     * 
     * @param projectId ID projektu
     * @param category Opcjonalna kategoria (TILE, GUTTER, ACCESSORY)
     * @return Lista draft changes
     */
    public List<DraftChangeDTO> getDraftChanges(Long projectId, String category) {
        logger.info("📥 Pobieranie draft changes dla projektu ID: {}, kategoria: {}", projectId, category);
        
        List<ProjectDraftChange> drafts;
        if (category != null && !category.isEmpty()) {
            drafts = projectDraftChangeRepository.findByProjectIdAndCategory(projectId, category);
        } else {
            drafts = projectDraftChangeRepository.findByProjectId(projectId);
        }
        
        List<DraftChangeDTO> result = drafts.stream().map(draft -> {
            DraftChangeDTO dto = new DraftChangeDTO();
            dto.setProductId(draft.getProductId());
            dto.setCategory(draft.getCategory());
            dto.setDraftRetailPrice(draft.getDraftRetailPrice());
            dto.setDraftPurchasePrice(draft.getDraftPurchasePrice());
            dto.setDraftSellingPrice(draft.getDraftSellingPrice());
            dto.setDraftQuantity(draft.getDraftQuantity());
            dto.setDraftSelected(draft.getDraftSelected()); // ⚠️ WAŻNE: Odczytaj stan checkboxa dla akcesoriów
            dto.setDraftMarginPercent(draft.getDraftMarginPercent());
            dto.setDraftDiscountPercent(draft.getDraftDiscountPercent());
            dto.setPriceChangeSource(draft.getPriceChangeSource());
            
            // Opcja dla grupy produktowej (draft)
            // ⚠️ WAŻNE: manufacturer i groupName są pobierane z Product przez productId
            dto.setDraftIsMainOption(draft.getDraftIsMainOption());
            
            return dto;
        }).collect(Collectors.toList());
        
        logger.info("✅ Znaleziono {} draft changes", result.size());
        return result;
    }
    
    /**
     * Usuwa wszystkie draft changes i draft inputs dla projektu
     * Używane np. przy kliknięciu "Cofnij zmiany" lub po zapisaniu projektu
     * 
     * @param projectId ID projektu
     */
    @Transactional
    public void clearDraftChanges(Long projectId) {
        logger.info("🗑️ Usuwanie draft changes i draft inputs dla projektu ID: {}", projectId);
        projectDraftChangeRepository.deleteByProjectId(projectId);
        projectDraftInputRepository.deleteByProjectId(projectId);
        logger.info("✅ Draft changes i draft inputs usunięte");
    }
    
    // ==================== DRAFT INPUTS ====================
    
    /**
     * Zapisuje draft inputs (tymczasowe Input z formularza)
     * Wywoływane po każdej zmianie w formularzu "Wprowadź dane"
     * 
     * @param projectId ID projektu
     * @param request Request zawierający draft inputs
     */
    @Transactional
    public void saveDraftInputs(Long projectId, SaveDraftInputsRequest request) {
        logger.info("💾 Zapisywanie draft inputs dla projektu ID: {}", projectId);
        
        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            logger.info("  Brak inputs do zapisania");
            return;
        }
        
        // Usuń istniejące draft inputs dla projektu
        projectDraftInputRepository.deleteByProjectId(projectId);
        
        for (DraftInputDTO dto : request.getInputs()) {
            ProjectDraftInput draftInput = new ProjectDraftInput();
            draftInput.setProjectId(projectId);
            draftInput.setMapperName(dto.getMapperName());
            draftInput.setName(dto.getName());
            draftInput.setQuantity(dto.getQuantity());
            
            projectDraftInputRepository.save(draftInput);
        }
        
        logger.info("✅ Zapisano {} draft inputs", request.getInputs().size());
    }
    
    /**
     * Pobiera draft inputs dla projektu
     * 
     * @param projectId ID projektu
     * @return Lista draft inputs
     */
    public List<DraftInputDTO> getDraftInputs(Long projectId) {
        logger.info("📥 Pobieranie draft inputs dla projektu ID: {}", projectId);
        
        List<ProjectDraftInput> drafts = projectDraftInputRepository.findByProjectId(projectId);
        
        List<DraftInputDTO> result = drafts.stream().map(draft -> {
            DraftInputDTO dto = new DraftInputDTO();
            dto.setMapperName(draft.getMapperName());
            dto.setName(draft.getName());
            dto.setQuantity(draft.getQuantity());
            return dto;
        }).collect(Collectors.toList());
        
        logger.info("✅ Znaleziono {} draft inputs", result.size());
        return result;
    }
    
    /**
     * Usuwa wszystkie draft inputs dla projektu
     * Używane np. przy kliknięciu "Cofnij zmiany" lub po zapisaniu projektu
     * 
     * @param projectId ID projektu
     */
    @Transactional
    public void clearDraftInputs(Long projectId) {
        logger.info("🗑️ Usuwanie draft inputs dla projektu ID: {}", projectId);
        projectDraftInputRepository.deleteByProjectId(projectId);
        logger.info("✅ Draft inputs usunięte");
    }
}

