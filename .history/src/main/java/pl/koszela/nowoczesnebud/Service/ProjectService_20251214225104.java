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
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        long methodStartTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] ProjectService.save - START | projectId: {}", 
                   project.getId() != null ? project.getId() : "nowy");
        
        // Jeśli projekt ma ID - aktualizuj istniejący
        if (project.getId() != null) {
            long findStartTime = System.currentTimeMillis();
            Project existingProject = projectRepository.findById(project.getId())
                .orElseThrow(() -> new RuntimeException("Project not found: " + project.getId()));
            long findEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findById - {}ms", findEndTime - findStartTime);
            
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
            
            long processInputsStartTime = System.currentTimeMillis();
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
                
                long processInputsEndTime = System.currentTimeMillis();
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Przetwarzanie {} Input: {}ms", 
                           inputsToProcess.size(), processInputsEndTime - processInputsStartTime);
            } else {
                logger.warn("⚠️ Brak Input do zapisania - inputsToProcess jest puste");
            }
            
            // Zapisz projekt
            long saveStartTime = System.currentTimeMillis();
            Project savedProject = projectRepository.save(existingProject);
            long saveEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Save: save - {}ms", saveEndTime - saveStartTime);
            
            long methodEndTime = System.currentTimeMillis();
            long totalDuration = methodEndTime - methodStartTime;
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] ProjectService.save - END | projectId: {} | czas całkowity: {}ms", 
                       savedProject.getId(), totalDuration);
            
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
        long saveStartTime = System.currentTimeMillis();
        Project savedProject = projectRepository.save(project);
        long saveEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Save: save (nowy projekt) - {}ms", saveEndTime - saveStartTime);
        
        // Zwróć projekt z załadowanymi inputami (używając JOIN FETCH)
        long fetchStartTime = System.currentTimeMillis();
        Project result = projectRepository.findByIdWithClientAndInputs(savedProject.getId())
            .orElse(savedProject);
        long fetchEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findByIdWithClientAndInputs - {}ms", fetchEndTime - fetchStartTime);
        
        long methodEndTime = System.currentTimeMillis();
        long totalDuration = methodEndTime - methodStartTime;
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] ProjectService.save - END | projectId: {} | czas całkowity: {}ms", 
                   savedProject.getId(), totalDuration);
        
        return result;
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
        long methodStartTime = System.currentTimeMillis();
        int productsCount = request.getProducts() != null ? request.getProducts().size() : 0;
        int productGroupsCount = request.getProductGroups() != null ? request.getProductGroups().size() : 0;
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] ProjectService.saveProjectData - START | projectId: {} | products: {} | productGroups: {}", 
                   projectId, productsCount, productGroupsCount);
        
        long findProjectStartTime = System.currentTimeMillis();
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        long findProjectEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findById - {}ms", findProjectEndTime - findProjectStartTime);
        
        // 1. Aktualizuj globalne rabaty
        long updateMarginsStartTime = System.currentTimeMillis();
        project.setTilesMargin(request.getTilesMargin());
        project.setTilesDiscount(request.getTilesDiscount());
        project.setGuttersMargin(request.getGuttersMargin());
        project.setGuttersDiscount(request.getGuttersDiscount());
        project.setAccessoriesMargin(request.getAccessoriesMargin());
        project.setAccessoriesDiscount(request.getAccessoriesDiscount());
        long updateMarginsEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Aktualizacja rabatów globalnych: {}ms", updateMarginsEndTime - updateMarginsStartTime);
        
        // 2. NOWE: Przenieś draft changes do ProjectProduct
        long findDraftChangesStartTime = System.currentTimeMillis();
        List<ProjectDraftChange> allDraftChanges = projectDraftChangeRepository.findByProjectId(projectId);
        long findDraftChangesEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findByProjectId (draft changes) - {} draft changes w {}ms", 
                   allDraftChanges.size(), findDraftChangesEndTime - findDraftChangesStartTime);
        
        // ⚡ WAŻNE: Usuń stare ProjectProduct jeśli nie ma draft changes
        // Projekt może mieć tylko jeden zestaw ProjectProduct (z aktualnych draft changes)
        // Jeśli nie ma draft changes, to nie powinno być żadnych ProjectProduct
        if (allDraftChanges.isEmpty()) {
            long deleteExistingProductsStartTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Brak draft changes - usuwanie wszystkich ProjectProduct dla projektu");
            project.getProjectProducts().clear(); // Usuń z kolekcji
            projectProductRepository.deleteByProjectId(projectId); // Usuń z bazy
            entityManager.flush(); // Zapisuje usunięcie do bazy
            long deleteExistingProductsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Usunięto wszystkie ProjectProduct (brak draft changes) - {}ms", 
                       deleteExistingProductsEndTime - deleteExistingProductsStartTime);
        } else {
            long transferDraftChangesStartTime = System.currentTimeMillis();
            
            // ⚡ WAŻNE: Usuń WSZYSTKIE istniejące ProjectProduct dla tego projektu
            // Projekt może mieć tylko jeden zestaw ProjectProduct (z aktualnych draft changes)
            // Nie może być wielu zestawów zapisanych dla jednego projektu
            long deleteExistingProductsStartTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Usuwanie starych ProjectProduct dla projektu");
            project.getProjectProducts().clear(); // Usuń z kolekcji
            projectProductRepository.deleteByProjectId(projectId); // Usuń z bazy
            entityManager.flush(); // Zapisuje usunięcie do bazy
            long deleteExistingProductsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Usunięto stare ProjectProduct - {}ms", 
                       deleteExistingProductsEndTime - deleteExistingProductsStartTime);
            
            // ⚡ OPTYMALIZACJA: Użyj JDBC batch insert zamiast Hibernate ORM dla dużej liczby rekordów
            // Hibernate ORM jest bardzo wolny dla 8685 rekordów (timeout), batch insert jest znacznie szybszy
            long batchInsertStartTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PRZED batch insert - {} rekordów do zapisania", allDraftChanges.size());
            batchInsertProjectProducts(projectId, allDraftChanges);
            long batchInsertEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Batch insert ProjectProduct do bazy - {}ms (zapisano {} rekordów)", 
                       batchInsertEndTime - batchInsertStartTime, allDraftChanges.size());
            
            // ⚠️ WAŻNE: NIE używamy entityManager.refresh(project) - może być wolne dla 8685 rekordów
            // ProjectProduct są już zapisane w bazie przez batch insert
            // Jeśli potrzebujemy ProjectProduct w dalszych operacjach, załadujemy je z bazy
            
            // 2a.1. Przenieś opcje grup z draft changes do ProjectProductGroup (PRZED usunięciem draft changes!)
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PRZED sekcją opcji grup - allDraftChanges.size() = {}", allDraftChanges.size());
            
            // Sprawdź, ile draft changes ma opcje grup
            long draftChangesWithOptions = allDraftChanges.stream()
                .filter(dc -> dc.getDraftIsMainOption() != null && dc.getDraftIsMainOption() != GroupOption.NONE)
                .count();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Draft changes z opcjami grup (draftIsMainOption != null && != NONE): {}", draftChangesWithOptions);
            
            long draftChangesWithOptionsAndProductId = allDraftChanges.stream()
                .filter(dc -> dc.getDraftIsMainOption() != null && dc.getDraftIsMainOption() != GroupOption.NONE)
                .filter(dc -> dc.getProductId() != null && dc.getProductId() > 0)
                .count();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Draft changes z opcjami grup i productId > 0: {}", draftChangesWithOptionsAndProductId);
            
            long draftChangesWithOptionsAndProductIdZero = allDraftChanges.stream()
                .filter(dc -> dc.getDraftIsMainOption() != null && dc.getDraftIsMainOption() != GroupOption.NONE)
                .filter(dc -> dc.getProductId() != null && dc.getProductId() == 0)
                .count();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Draft changes z opcjami grup i productId = 0: {}", draftChangesWithOptionsAndProductIdZero);
            
            try {
                long transferGroupsStartTime = System.currentTimeMillis();
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] START: Przenoszenie opcji grup z draft changes");
            
            // ⚡ OPTYMALIZACJA: Pobierz wszystkie produkty naraz zamiast N+1 zapytań
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PRZED filtrowaniem productIds");
            long loadProductsStartTime = System.currentTimeMillis();
            Set<Long> productIds = allDraftChanges.stream()
                .filter(dc -> dc.getDraftIsMainOption() != null && dc.getDraftIsMainOption() != GroupOption.NONE)
                .filter(dc -> dc.getProductId() != null && dc.getProductId() > 0)
                .map(ProjectDraftChange::getProductId)
                .collect(Collectors.toSet());
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PO filtrowaniu productIds - znaleziono {} produktów", productIds.size());
            
            Map<Long, Product> productsMap = new HashMap<>();
            if (!productIds.isEmpty()) {
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PRZED findAllById - {} produktów", productIds.size());
                List<Product> products = productRepository.findAllById(productIds);
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PO findAllById - pobrano {} produktów", products.size());
                for (Product p : products) {
                    productsMap.put(p.getId(), p);
                }
            }
            long loadProductsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findAllById (produkty dla opcji grup) - {} produktów w {}ms", 
                       productsMap.size(), loadProductsEndTime - loadProductsStartTime);
            
            // ⚡ OPTYMALIZACJA: Pobierz wszystkie produkty dla kategorii (dla productId = 0) raz zamiast setki razy
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PRZED filtrowaniem kategorii dla productId=0");
            long loadProductsByCategoryStartTime = System.currentTimeMillis();
            Set<String> categories = allDraftChanges.stream()
                .filter(dc -> dc.getDraftIsMainOption() != null && dc.getDraftIsMainOption() != GroupOption.NONE)
                .filter(dc -> dc.getProductId() != null && dc.getProductId() == 0)
                .map(ProjectDraftChange::getCategory)
                .collect(Collectors.toSet());
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PO filtrowaniu kategorii - znaleziono {} kategorii", categories.size());
            
            Map<String, List<Product>> productsByCategoryMap = new HashMap<>();
            int totalProductsLoaded = 0;
            for (String categoryStr : categories) {
                try {
                    logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Pobieranie produktów dla kategorii: {}", categoryStr);
                    long categoryStartTime = System.currentTimeMillis();
                    ProductCategory category = ProductCategory.valueOf(categoryStr);
                    List<Product> products = productRepository.findByCategory(category);
                    long categoryEndTime = System.currentTimeMillis();
                    totalProductsLoaded += products.size();
                    logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Pobrano {} produktów dla kategorii {} w {}ms", 
                               products.size(), categoryStr, categoryEndTime - categoryStartTime);
                    productsByCategoryMap.put(categoryStr, products);
                } catch (IllegalArgumentException e) {
                    logger.warn("    Nieprawidłowa kategoria: {}", categoryStr);
                }
            }
            long loadProductsByCategoryEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findByCategory (dla productId=0) - {} kategorii, {} produktów łącznie w {}ms", 
                       categories.size(), totalProductsLoaded, loadProductsByCategoryEndTime - loadProductsByCategoryStartTime);
            
            // ⚡ OPTYMALIZACJA: Utwórz mapę draft changes po productId przed pętlą (dla productId = 0)
            long buildDraftMapForGroupsStartTime = System.currentTimeMillis();
            Map<String, ProjectDraftChange> draftChangesByProductIdForGroups = new HashMap<>();
            for (ProjectDraftChange dc : allDraftChanges) {
                if (dc.getProductId() != null && dc.getDraftIsMainOption() != null) {
                    String key = dc.getProductId() + "_" + dc.getCategory();
                    draftChangesByProductIdForGroups.put(key, dc);
                }
            }
            long buildDraftMapForGroupsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Budowanie mapy draft changes po productId (dla opcji grup) - {}ms", 
                       buildDraftMapForGroupsEndTime - buildDraftMapForGroupsStartTime);
            
            // ⚡ OPTYMALIZACJA: Utwórz mapę grup (manufacturer + groupName) dla każdej kategorii (dla productId = 0)
            // To pozwoli uniknąć wielokrotnego przeszukiwania produktów dla każdego draft change z productId = 0
            long buildCategoryGroupsMapStartTime = System.currentTimeMillis();
            Map<String, Map<String, String[]>> categoryGroupsMap = new HashMap<>(); // Map<category, Map<groupKey, [manufacturer, groupName]>>
            for (Map.Entry<String, List<Product>> entry : productsByCategoryMap.entrySet()) {
                String category = entry.getKey();
                List<Product> products = entry.getValue();
                Map<String, String[]> groupsForCategory = new HashMap<>();
                
                for (Product p : products) {
                    if (p.getManufacturer() != null && p.getGroupName() != null) {
                        String key = p.getId() + "_" + category;
                        ProjectDraftChange groupDraft = draftChangesByProductIdForGroups.get(key);
                        if (groupDraft != null && groupDraft.getDraftIsMainOption() != null) {
                            String groupKey = p.getManufacturer() + "_" + p.getGroupName() + "_" + category;
                            if (!groupsForCategory.containsKey(groupKey)) {
                                groupsForCategory.put(groupKey, new String[]{p.getManufacturer(), p.getGroupName()});
                            }
                        }
                    }
                }
                categoryGroupsMap.put(category, groupsForCategory);
            }
            long buildCategoryGroupsMapEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Budowanie mapy grup dla kategorii (dla productId=0) - {}ms", 
                       buildCategoryGroupsMapEndTime - buildCategoryGroupsMapStartTime);
            
            // Grupuj draft changes po manufacturer + groupName (pobierane z Product przez productId)
            // ⚠️ WAŻNE: Obsługuj productId = 0 (z importu) - wtedy szukaj produktów po manufacturer i groupName
            Map<String, ProjectDraftChange> groupOptionsMap = new java.util.HashMap<>();
            int processedCount = 0;
            int productIdGreaterThanZeroCount = 0;
            int productIdEqualsZeroCount = 0;
            
            long loopGroupsStartTime = System.currentTimeMillis();
            for (ProjectDraftChange draft : allDraftChanges) {
                if (draft.getDraftIsMainOption() != null && draft.getDraftIsMainOption() != GroupOption.NONE) {
                    processedCount++;
                    String groupKey = null;
                    String manufacturer = null;
                    String groupName = null;
                    
                    if (draft.getProductId() != null && draft.getProductId() > 0) {
                        productIdGreaterThanZeroCount++;
                        // Normalny przypadek: productId > 0 - użyj HashMap zamiast findById
                        Product product = productsMap.get(draft.getProductId());
                        if (product != null) {
                            manufacturer = product.getManufacturer();
                            groupName = product.getGroupName();
                            if (manufacturer != null && groupName != null) {
                                groupKey = manufacturer + "_" + groupName + "_" + draft.getCategory();
                            }
                        }
                    } else if (draft.getProductId() != null && draft.getProductId() == 0) {
                        productIdEqualsZeroCount++;
                        // ⚡ OPTYMALIZACJA: Użyj wcześniej zbudowanej mapy grup dla kategorii
                        Map<String, String[]> groupsForCategory = categoryGroupsMap.get(draft.getCategory());
                        if (groupsForCategory != null && !groupsForCategory.isEmpty()) {
                            // Użyj pierwszej znalezionej grupy dla tej kategorii
                            // (wszystkie produkty w grupie mają tę samą opcję)
                            Map.Entry<String, String[]> firstGroup = groupsForCategory.entrySet().iterator().next();
                            groupKey = firstGroup.getKey();
                            String[] manufacturerAndGroup = firstGroup.getValue();
                            if (manufacturerAndGroup != null && manufacturerAndGroup.length == 2) {
                                manufacturer = manufacturerAndGroup[0];
                                groupName = manufacturerAndGroup[1];
                            }
                        }
                    }
                    
                    // Zapisz opcję grupy (użyj pierwszej znalezionej dla danej grupy)
                    if (groupKey != null && !groupOptionsMap.containsKey(groupKey)) {
                        groupOptionsMap.put(groupKey, draft);
                    }
                }
            }
            long loopGroupsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Pętla przetwarzania opcji grup - {} rekordów w {}ms (productId>0: {}, productId=0: {})", 
                       processedCount, loopGroupsEndTime - loopGroupsStartTime, productIdGreaterThanZeroCount, productIdEqualsZeroCount);
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Utworzono {} unikalnych grup produktowych", groupOptionsMap.size());
            
            // Utwórz ProjectProductGroup z opcji grup
            long createGroupsStartTime = System.currentTimeMillis();
            if (!groupOptionsMap.isEmpty()) {
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Tworzenie {} ProjectProductGroup", groupOptionsMap.size());
                
                // ⚡ OPTYMALIZACJA: Utwórz mapę draft changes po productId dla szybkiego wyszukiwania
                long buildDraftMapStartTime = System.currentTimeMillis();
                Map<String, ProjectDraftChange> draftChangesByProductId = new HashMap<>();
                for (ProjectDraftChange dc : allDraftChanges) {
                    if (dc.getProductId() != null && dc.getDraftIsMainOption() != null) {
                        String key = dc.getProductId() + "_" + dc.getCategory();
                        draftChangesByProductId.put(key, dc);
                    }
                }
                long buildDraftMapEndTime = System.currentTimeMillis();
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Budowanie mapy draft changes po productId - {}ms", 
                           buildDraftMapEndTime - buildDraftMapStartTime);
                
                int createdGroupsCount = 0;
                for (Map.Entry<String, ProjectDraftChange> entry : groupOptionsMap.entrySet()) {
                    ProjectDraftChange draft = entry.getValue();
                    String manufacturer = null;
                    String groupName = null;
                    
                    if (draft.getProductId() != null && draft.getProductId() > 0) {
                        // Normalny przypadek: productId > 0 - użyj HashMap zamiast findById
                        Product product = productsMap.get(draft.getProductId());
                        if (product != null) {
                            manufacturer = product.getManufacturer();
                            groupName = product.getGroupName();
                        }
                    } else if (draft.getProductId() != null && draft.getProductId() == 0) {
                        // ⚡ OPTYMALIZACJA: Użyj wcześniej załadowanych produktów zamiast findByCategory
                        List<Product> productsInGroup = productsByCategoryMap.get(draft.getCategory());
                        if (productsInGroup != null) {
                            for (Product p : productsInGroup) {
                                if (p.getManufacturer() != null && p.getGroupName() != null) {
                                    // ⚡ OPTYMALIZACJA: Użyj HashMap zamiast stream().filter()
                                    String key = p.getId() + "_" + draft.getCategory();
                                    ProjectDraftChange groupDraft = draftChangesByProductId.get(key);
                                    if (groupDraft != null && groupDraft.getDraftIsMainOption() != null) {
                                        manufacturer = p.getManufacturer();
                                        groupName = p.getGroupName();
                                        break;
                                    }
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
                        createdGroupsCount++;
                        logger.debug("    Utworzono ProjectProductGroup: {} - {} (isMainOption: {})", 
                                   manufacturer, groupName, draft.getDraftIsMainOption());
                    }
                }
                long createGroupsEndTime = System.currentTimeMillis();
                logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Tworzenie {} ProjectProductGroup - {}ms", 
                           createdGroupsCount, createGroupsEndTime - createGroupsStartTime);
            }
            long transferGroupsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Przenoszenie opcji grup z draft changes - {}ms", 
                       transferGroupsEndTime - transferGroupsStartTime);
            } catch (Exception e) {
                logger.error("⏱️ [PERFORMANCE] [Zapisz projekt] BŁĄD w sekcji opcji grup: {}", e.getMessage(), e);
                throw e;
            }
            
            // Usuń draft changes po przeniesieniu (zarówno do ProjectProduct jak i ProjectProductGroup)
            long deleteDraftChangesStartTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PRZED deleteByProjectId - {} rekordów do usunięcia", allDraftChanges.size());
            // ⚡ OPTYMALIZACJA: Użyj natywnego SQL przez EntityManager dla lepszej wydajności
            int deletedCount = entityManager.createNativeQuery("DELETE FROM project_draft_changes_ws WHERE project_id = :projectId")
                    .setParameter("projectId", projectId)
                    .executeUpdate();
            entityManager.flush();
            // ⚠️ WAŻNE: NIE wywołuj entityManager.clear() tutaj - project.getInputs() potrzebuje aktywnej sesji!
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] PO deleteByProjectId - usunięto {} rekordów", deletedCount);
            long deleteDraftChangesEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Delete: deleteByProjectId (draft changes) - {}ms", 
                       deleteDraftChangesEndTime - deleteDraftChangesStartTime);
            
            long transferDraftChangesEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Przenoszenie draft changes do ProjectProduct - {}ms", 
                       transferDraftChangesEndTime - transferDraftChangesStartTime);
        }
        
        // 2b. NOWE: Przenieś draft inputs do Input
        long findDraftInputsStartTime = System.currentTimeMillis();
        List<ProjectDraftInput> allDraftInputs = projectDraftInputRepository.findByProjectId(projectId);
        long findDraftInputsEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Query: findByProjectId (draft inputs) - {} draft inputs w {}ms", 
                   allDraftInputs.size(), findDraftInputsEndTime - findDraftInputsStartTime);
        
        if (!allDraftInputs.isEmpty()) {
            long transferDraftInputsStartTime = System.currentTimeMillis();
            
            // ⚠️ WAŻNE: Załaduj project ponownie jeśli został odłączony (dla bezpieczeństwa)
            // project.getInputs() wymaga aktywnej sesji Hibernate (lazy loading)
            if (!entityManager.contains(project)) {
                project = entityManager.find(Project.class, projectId);
            }
            
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
            long deleteDraftInputsStartTime = System.currentTimeMillis();
            projectDraftInputRepository.deleteByProjectId(projectId);
            long deleteDraftInputsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Delete: deleteByProjectId (draft inputs) - {}ms", 
                       deleteDraftInputsEndTime - deleteDraftInputsStartTime);
            
            long transferDraftInputsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Przenoszenie draft inputs do Input - {}ms", 
                       transferDraftInputsEndTime - transferDraftInputsStartTime);
        }
        
        // 3. ⚠️ WAŻNE: NIE nadpisuj produktów z request - draft changes mają priorytet!
        long processRequestProductsStartTime = System.currentTimeMillis();
        // Produkty z request są używane tylko do dodatkowych aktualizacji (np. productGroups)
        // Ale wartości z draft changes (skopiowane w sekcji 2) mają najwyższy priorytet
        // Jeśli request zawiera produkty, które nie są w draft changes, dodaj je
        // Ale NIE nadpisuj produktów, które już zostały skopiowane z draft changes
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            logger.info("  Sprawdzanie {} produktów z request (dodanie tylko tych, które nie są w draft changes)", request.getProducts().size());
            
            // ⚡ OPTYMALIZACJA: Utwórz mapę produktów z draft changes (zamiast project.getProjectProducts())
            // project.getProjectProducts() jest puste po batch insert, więc używamy allDraftChanges
            Map<String, ProjectDraftChange> draftProductsMap = allDraftChanges.stream()
                .collect(Collectors.toMap(
                    dc -> dc.getProductId() + "_" + dc.getCategory(),
                    dc -> dc,
                    (existing, replacement) -> existing // Jeśli duplikat, użyj istniejącego
                ));
            
            // ⚡ OPTYMALIZACJA: Dodaj produkty z request, które nie są w draft changes, używając batch insert
            List<SaveProjectProductDTO> productsToAdd = new ArrayList<>();
            for (SaveProjectProductDTO dto : request.getProducts()) {
                String key = dto.getProductId() + "_" + dto.getCategory();
                if (!draftProductsMap.containsKey(key)) {
                    productsToAdd.add(dto);
                    logger.debug("    Produkt z request (nie był w draft changes): productId={}, category={}", dto.getProductId(), dto.getCategory());
                } else {
                    logger.debug("    Pomiń produkt z request (już jest w draft changes): productId={}, category={}", dto.getProductId(), dto.getCategory());
                }
            }
            
            // Jeśli są produkty do dodania, użyj batch insert (ale to rzadki przypadek)
            if (!productsToAdd.isEmpty()) {
                logger.info("  Dodawanie {} produktów z request (nie były w draft changes)", productsToAdd.size());
                // Dla małej liczby produktów z request, możemy użyć Hibernate (szybkie dla < 100)
                // Dla większej liczby, użyjemy batch insert
                if (productsToAdd.size() > 100) {
                    // TODO: Jeśli będzie potrzeba, dodaj batch insert dla produktów z request
                    logger.warn("  ⚠️ Wiele produktów z request ({}), ale batch insert nie jest zaimplementowany", productsToAdd.size());
                }
                for (SaveProjectProductDTO dto : productsToAdd) {
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
                }
            }
            long processRequestProductsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Przetwarzanie produktów z request - {}ms", 
                       processRequestProductsEndTime - processRequestProductsStartTime);
        }
        
        // 4. Usuń stare ProjectProductGroup
        long clearGroupsStartTime = System.currentTimeMillis();
        
        // ⚠️ WAŻNE: Załaduj project ponownie jeśli został odłączony (dla bezpieczeństwa)
        // project.getProjectProductGroups() wymaga aktywnej sesji Hibernate (lazy loading)
        if (!entityManager.contains(project)) {
            project = entityManager.find(Project.class, projectId);
        }
        
        project.getProjectProductGroups().clear();
        entityManager.flush(); // Wymuś usunięcie przed dodaniem nowych
        long clearGroupsEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Usunięcie starych ProjectProductGroup + flush - {}ms", 
                   clearGroupsEndTime - clearGroupsStartTime);
        
        // 5. Dodaj nowe ProjectProductGroup z request
        long addProductGroupsStartTime = System.currentTimeMillis();
        if (request.getProductGroups() != null && !request.getProductGroups().isEmpty()) {
            for (SaveProjectProductGroupDTO dto : request.getProductGroups()) {
                ProjectProductGroup ppg = new ProjectProductGroup();
                ppg.setProject(project);
                ppg.setCategory(dto.getCategory());
                ppg.setManufacturer(dto.getManufacturer());
                ppg.setGroupName(dto.getGroupName());
                ppg.setIsMainOption(dto.getIsMainOption());
                
                project.getProjectProductGroups().add(ppg);
            }
            long addProductGroupsEndTime = System.currentTimeMillis();
            logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] Dodanie {} grup produktowych - {}ms", 
                       request.getProductGroups().size(), addProductGroupsEndTime - addProductGroupsStartTime);
        }
        
        // 6. Zapisz projekt z nowymi danymi
        long finalSaveStartTime = System.currentTimeMillis();
        projectRepository.save(project);
        long finalSaveEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] DB Save: save (final) - {}ms", finalSaveEndTime - finalSaveStartTime);
        
        long methodEndTime = System.currentTimeMillis();
        long totalDuration = methodEndTime - methodStartTime;
        logger.info("⏱️ [PERFORMANCE] [Zapisz projekt] ProjectService.saveProjectData - END | projectId: {} | czas całkowity: {}ms", 
                   projectId, totalDuration);
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
        int totalChanges = request.getChanges() != null ? request.getChanges().size() : 0;
        logger.info("⏱️ [PERFORMANCE] saveDraftChanges - START | projectId: {} | kategoria: {} | zmian: {}", 
                   projectId, request.getCategory(), totalChanges);
        
        if (request.getChanges() == null || request.getChanges().isEmpty()) {
            logger.info("⏱️ [PERFORMANCE] saveDraftChanges - END | Brak zmian do zapisania | czas: 0ms");
            return;
        }
        
        // ⚡ OPTYMALIZACJA: Sprawdź, czy to tylko zmiana quantity (dla "Przelicz produkty")
        // "Przelicz produkty" zmienia głównie quantity, więc możemy użyć szybszego UPDATE zamiast UPSERT
        // Frontend wysyła wszystkie pola (retailPrice, purchasePrice, etc.), ale jeśli categoryMargin i 
        // categoryDiscount są null, a wszystkie zmiany mają quantity != null, to prawdopodobnie to "Przelicz produkty"
        boolean isOnlyQuantityChange = request.getCategoryMargin() == null && 
                                       request.getCategoryDiscount() == null &&
                                       request.getChanges().stream().allMatch(change -> 
                                           change.getDraftQuantity() != null
                                       ) &&
                                       // Dodatkowo: sprawdź, czy większość zmian ma tylko quantity (opcjonalne sprawdzenie)
                                       // Jeśli mniej niż 10% zmian ma inne pola (retailPrice, purchasePrice, etc.), 
                                       // to prawdopodobnie to tylko zmiana quantity
                                       request.getChanges().stream()
                                           .filter(change -> {
                                               // Sprawdź, czy są inne zmiany oprócz quantity
                                               boolean hasOtherChanges = 
                                                   (change.getDraftRetailPrice() != null && change.getDraftRetailPrice() != 0) ||
                                                   (change.getDraftPurchasePrice() != null && change.getDraftPurchasePrice() != 0) ||
                                                   (change.getDraftSellingPrice() != null && change.getDraftSellingPrice() != 0) ||
                                                   (change.getDraftMarginPercent() != null && change.getDraftMarginPercent() != 0) ||
                                                   (change.getDraftDiscountPercent() != null && change.getDraftDiscountPercent() != 0) ||
                                                   (change.getDraftIsMainOption() != null);
                                               return hasOtherChanges;
                                           })
                                           .count() < request.getChanges().size() * 0.1; // Mniej niż 10% ma inne zmiany
        
        if (isOnlyQuantityChange) {
            // ⚡ OPTYMALIZACJA: UPDATE tylko quantity - znacznie szybsze!
            logger.info("⏱️ [PERFORMANCE] Wykryto tylko zmiany quantity - używam UPDATE zamiast UPSERT");
            updateQuantitiesOnly(projectId, request);
            return;
        }
        
        // ⚡ WAŻNE: Usuń stare draft changes dla tej kategorii przed zapisem nowych
        // To zapewni, że w project_draft_changes_ws będą tylko rekordy dla produktów z aktualnego cennika
        // (nie będzie rekordów dla produktów, które już nie są w cenniku)
        long deleteOldDraftsStartTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Usuwanie starych draft changes dla kategorii {} przed zapisem nowych", request.getCategory());
        int deletedCount = entityManager.createNativeQuery("DELETE FROM project_draft_changes_ws WHERE project_id = :projectId AND category = :category")
                .setParameter("projectId", projectId)
                .setParameter("category", request.getCategory())
                .executeUpdate();
        entityManager.flush();
        long deleteOldDraftsEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Usunięto {} starych draft changes dla kategorii {} - {}ms", 
                   deletedCount, request.getCategory(), deleteOldDraftsEndTime - deleteOldDraftsStartTime);
        
        // ⚡ OPTYMALIZACJA: UPSERT zamiast DELETE + INSERT dla innych zmian
        // Po usunięciu starych rekordów, UPSERT będzie tylko INSERT (szybsze)
        logger.info("⏱️ [PERFORMANCE] Używam UPSERT zamiast DELETE + INSERT");
        upsertDraftChanges(projectId, request);
    }
    
    /**
     * ⚡ OPTYMALIZACJA: UPDATE tylko quantity dla produktów (używane przez "Przelicz produkty")
     * Używa INSERT ... ON DUPLICATE KEY UPDATE, żeby móc tworzyć nowe rekordy jeśli nie istnieją
     * Znacznie szybsze niż DELETE + INSERT, bo nie usuwa wszystkich rekordów
     */
    private void updateQuantitiesOnly(Long projectId, SaveDraftChangesRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] UPDATE QUANTITIES ONLY - START | projectId: {} | kategoria: {} | zmian: {}", 
                   projectId, request.getCategory(), request.getChanges().size());
        
        // ⚡ WAŻNE: Usuń stare draft changes dla tej kategorii przed zapisem nowych
        // To zapewni, że nie będzie duplikatów
        long deleteOldDraftsStartTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Usuwanie starych draft changes dla kategorii {} przed zapisem nowych", request.getCategory());
        int deletedCount = entityManager.createNativeQuery("DELETE FROM project_draft_changes_ws WHERE project_id = :projectId AND category = :category")
                .setParameter("projectId", projectId)
                .setParameter("category", request.getCategory())
                .executeUpdate();
        entityManager.flush();
        long deleteOldDraftsEndTime = System.currentTimeMillis();
        logger.info("⏱️ [PERFORMANCE] Usunięto {} starych draft changes dla kategorii {} - {}ms", 
                   deletedCount, request.getCategory(), deleteOldDraftsEndTime - deleteOldDraftsStartTime);
        
        // ⚡ Używamy INSERT ... ON DUPLICATE KEY UPDATE zamiast zwykłego UPDATE
        // To pozwala tworzyć nowe rekordy jeśli nie istnieją (dla "Przelicz produkty" na nowych produktach)
        // ⚠️ WAŻNE: Używamy tego samego SQL co upsertDraftChanges, ale ustawiamy tylko draft_quantity
        // Inne pola pozostają NULL (dla INSERT) lub bez zmian (dla UPDATE - używamy nazwy kolumny zamiast VALUES())
        // MySQL używa UNIQUE constraint uk_draft_changes_project_product_category do wykrycia duplikatów
        String sql = "INSERT INTO project_draft_changes_ws " +
                    "(project_id, product_id, category, draft_retail_price, draft_purchase_price, " +
                    "draft_selling_price, draft_quantity, draft_selected, draft_margin_percent, " +
                    "draft_discount_percent, price_change_source, draft_is_main_option, " +
                    "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "draft_quantity = VALUES(draft_quantity), " +
                    "updated_at = VALUES(updated_at), " +
                    "draft_retail_price = COALESCE(VALUES(draft_retail_price), draft_retail_price), " +
                    "draft_purchase_price = COALESCE(VALUES(draft_purchase_price), draft_purchase_price), " +
                    "draft_selling_price = COALESCE(VALUES(draft_selling_price), draft_selling_price), " +
                    "draft_selected = COALESCE(VALUES(draft_selected), draft_selected), " +
                    "draft_margin_percent = COALESCE(VALUES(draft_margin_percent), draft_margin_percent), " +
                    "draft_discount_percent = COALESCE(VALUES(draft_discount_percent), draft_discount_percent), " +
                    "price_change_source = COALESCE(VALUES(price_change_source), price_change_source), " +
                    "draft_is_main_option = COALESCE(VALUES(draft_is_main_option), draft_is_main_option)";
        
        // ⚡ OPTYMALIZACJA: Większy batch size dla UPDATE (2000) - UPDATE jest szybszy niż UPSERT
        int batchSize = 2000;
        int totalChanges = request.getChanges().size();
        int totalBatches = (int)Math.ceil((double)totalChanges / batchSize);
        
        final long[] totalPrepareTime = {0};
        final long[] totalSaveTime = {0};
        
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalChanges);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchPrepareStart = System.currentTimeMillis();
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            DraftChangeDTO dto = request.getChanges().get(i);
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            
                            int paramIndex = 1;
                            pstmt.setLong(paramIndex++, projectId);
                            pstmt.setObject(paramIndex++, dto.getProductId());
                            pstmt.setString(paramIndex++, request.getCategory());
                            // ⚠️ WAŻNE: Ustawiamy tylko draft_quantity, inne pola są NULL
                            // W przypadku UPDATE (duplikat), tylko draft_quantity zostanie zaktualizowane
                            pstmt.setObject(paramIndex++, null);  // draft_retail_price
                            pstmt.setObject(paramIndex++, null);  // draft_purchase_price
                            pstmt.setObject(paramIndex++, null);  // draft_selling_price
                            pstmt.setObject(paramIndex++, dto.getDraftQuantity());  // draft_quantity - JEDYNE pole które ustawiamy
                            pstmt.setObject(paramIndex++, null);  // draft_selected
                            pstmt.setObject(paramIndex++, null);  // draft_margin_percent
                            pstmt.setObject(paramIndex++, null);  // draft_discount_percent
                            pstmt.setString(paramIndex++, null);  // price_change_source
                            pstmt.setString(paramIndex++, null);  // draft_is_main_option
                            pstmt.setTimestamp(paramIndex++, now);  // created_at
                            pstmt.setTimestamp(paramIndex++, now);  // updated_at
                            
                            pstmt.addBatch();
                        }
                        
                        long batchPrepareEnd = System.currentTimeMillis();
                        long batchPrepareTime = batchPrepareEnd - batchPrepareStart;
                        totalPrepareTime[0] += batchPrepareTime;
                        
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} przygotowany | rekordów: {} | czas przygotowania: {}ms", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchPrepareTime);
                        
                        long batchSaveStart = System.currentTimeMillis();
                        int[] results = pstmt.executeBatch();
                        long batchSaveEnd = System.currentTimeMillis();
                        long batchSaveTime = batchSaveEnd - batchSaveStart;
                        totalSaveTime[0] += batchSaveTime;
                        
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} zapisany (INSERT/UPDATE quantity) | rekordów: {} | czas zapisu: {}ms | przetworzonych: {}", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchSaveTime, results.length);
                        
                        entityManager.flush();
                    }
                } catch (SQLException e) {
                    logger.error("❌ [PERFORMANCE] Błąd podczas UPDATE quantities: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas UPDATE quantities", e);
                }
            }
        });
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] UPDATE QUANTITIES ONLY - END | rekordów: {} | batchy: {} | czas całkowity: {}ms | prepare: {}ms | save: {}ms", 
                   totalChanges, totalBatches, duration, totalPrepareTime[0], totalSaveTime[0]);
    }
    
    /**
     * ⚡ OPTYMALIZACJA: UPSERT draft changes (UPDATE jeśli istnieje, INSERT jeśli nie)
     * Znacznie szybsze niż DELETE + INSERT, bo nie usuwa wszystkich rekordów przed wstawieniem
     * 
     * UWAGA: Wymaga UNIQUE constraint na (project_id, product_id, category) lub używa istniejącego indeksu
     * Jeśli nie ma UNIQUE constraint, MySQL może nie wykryć duplikatów poprawnie.
     * W takim przypadku użyjemy INSERT ... ON DUPLICATE KEY UPDATE z indeksem.
     */
    private void upsertDraftChanges(Long projectId, SaveDraftChangesRequest request) {
        long startTime = System.currentTimeMillis();
        int totalChanges = request.getChanges().size();
        logger.info("⏱️ [PERFORMANCE] UPSERT DRAFT CHANGES - START | projectId: {} | kategoria: {} | zmian: {}", 
                   projectId, request.getCategory(), totalChanges);
        
        // MySQL UPSERT: INSERT ... ON DUPLICATE KEY UPDATE
        // Używa indeksu (project_id, product_id, category) do wykrycia duplikatów
        // Jeśli nie ma UNIQUE constraint, MySQL użyje pierwszego dostępnego UNIQUE lub PRIMARY KEY
        String sql = "INSERT INTO project_draft_changes_ws " +
                    "(project_id, product_id, category, draft_retail_price, draft_purchase_price, " +
                    "draft_selling_price, draft_quantity, draft_selected, draft_margin_percent, " +
                    "draft_discount_percent, price_change_source, draft_is_main_option, " +
                    "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "draft_retail_price = VALUES(draft_retail_price), " +
                    "draft_purchase_price = VALUES(draft_purchase_price), " +
                    "draft_selling_price = VALUES(draft_selling_price), " +
                    "draft_quantity = VALUES(draft_quantity), " +
                    "draft_selected = VALUES(draft_selected), " +
                    "draft_margin_percent = VALUES(draft_margin_percent), " +
                    "draft_discount_percent = VALUES(draft_discount_percent), " +
                    "price_change_source = VALUES(price_change_source), " +
                    "draft_is_main_option = VALUES(draft_is_main_option), " +
                    "updated_at = VALUES(updated_at)";
        
        int batchSize = 1000;
        int totalBatches = (int)Math.ceil((double)totalChanges / batchSize);
        
        // ⚡ Używamy final array aby móc modyfikować wartości w doWork()
        final long[] totalPrepareTime = {0};
        final long[] totalSaveTime = {0};
        
        // ⚡ WAŻNE: Używamy Hibernate Session.doWork() - działa zarówno z H2 jak i MySQL
        // To zapewnia, że connection jest w tej samej transakcji Spring
        // dataSource.getConnection() tworzy nowe połączenie poza transakcją, co może powodować timeouty
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalChanges);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchPrepareStart = System.currentTimeMillis();
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            DraftChangeDTO dto = request.getChanges().get(i);
                            
                            int paramIndex = 1;
                            pstmt.setLong(paramIndex++, projectId);
                            pstmt.setObject(paramIndex++, dto.getProductId());
                            pstmt.setString(paramIndex++, dto.getCategory());
                            pstmt.setObject(paramIndex++, dto.getDraftRetailPrice());
                            pstmt.setObject(paramIndex++, dto.getDraftPurchasePrice());
                            pstmt.setObject(paramIndex++, dto.getDraftSellingPrice());
                            pstmt.setObject(paramIndex++, dto.getDraftQuantity());
                            pstmt.setObject(paramIndex++, dto.getDraftSelected());
                            pstmt.setObject(paramIndex++, dto.getDraftMarginPercent());
                            pstmt.setObject(paramIndex++, dto.getDraftDiscountPercent());
                            pstmt.setString(paramIndex++, dto.getPriceChangeSource());
                            
                            String draftIsMainOption = dto.getDraftIsMainOption() != null 
                                ? dto.getDraftIsMainOption().name() 
                                : GroupOption.NONE.name();
                            pstmt.setString(paramIndex++, draftIsMainOption);
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        long batchPrepareEnd = System.currentTimeMillis();
                        long batchPrepareTime = batchPrepareEnd - batchPrepareStart;
                        totalPrepareTime[0] += batchPrepareTime;
                        
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} przygotowany | rekordów: {} | czas przygotowania: {}ms", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchPrepareTime);
                        
                        long batchSaveStart = System.currentTimeMillis();
                        int[] results = pstmt.executeBatch();
                        long batchSaveEnd = System.currentTimeMillis();
                        long batchSaveTime = batchSaveEnd - batchSaveStart;
                        totalSaveTime[0] += batchSaveTime;
                        
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} zapisany (UPSERT) | rekordów: {} | czas zapisu: {}ms | przetworzonych: {}", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchSaveTime, results.length);
                        
                        // ⚡ WAŻNE: Flush po każdym batchu, aby zmniejszyć ryzyko timeoutu
                        // Ale NIE commit - transakcja Spring zrobi commit na końcu
                        entityManager.flush();
                    }
                } catch (SQLException e) {
                    logger.error("❌ [PERFORMANCE] Błąd podczas UPSERT draft changes: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas UPSERT draft changes", e);
                }
            }
        });
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] UPSERT DRAFT CHANGES - END | rekordów: {} | batchy: {} | czas całkowity: {}ms | prepare: {}ms | save: {}ms", 
                   totalChanges, totalBatches, duration, totalPrepareTime[0], totalSaveTime[0]);
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
    
    /**
     * ⚡ OPTYMALIZACJA: Batch insert ProjectProduct (JDBC batch insert zamiast Hibernate ORM)
     * Znacznie szybsze niż Hibernate ORM dla dużej liczby rekordów (8685+)
     * 
     * @param projectId ID projektu
     * @param draftChanges Lista draft changes do przeniesienia do ProjectProduct
     */
    private void batchInsertProjectProducts(Long projectId, List<ProjectDraftChange> draftChanges) {
        long startTime = System.currentTimeMillis();
        int totalChanges = draftChanges.size();
        logger.info("⏱️ [PERFORMANCE] BATCH INSERT PROJECT PRODUCTS - START | projectId: {} | rekordów: {}", 
                   projectId, totalChanges);
        
        String sql = "INSERT INTO project_products " +
                    "(project_id, product_id, category, saved_retail_price, saved_purchase_price, " +
                    "saved_selling_price, saved_quantity, price_change_source, saved_margin_percent, " +
                    "saved_discount_percent, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 1000;
        int totalBatches = (int)Math.ceil((double)totalChanges / batchSize);
        
        // ⚡ Używamy final array aby móc modyfikować wartości w doWork()
        final long[] totalPrepareTime = {0};
        final long[] totalSaveTime = {0};
        
        // ⚡ WAŻNE: Używamy Hibernate Session.doWork() - działa zarówno z H2 jak i MySQL
        Session session = entityManager.unwrap(Session.class);
        
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                        int startIndex = batchIndex * batchSize;
                        int endIndex = Math.min(startIndex + batchSize, totalChanges);
                        int recordsInBatch = endIndex - startIndex;
                        
                        long batchPrepareStart = System.currentTimeMillis();
                        
                        for (int i = startIndex; i < endIndex; i++) {
                            ProjectDraftChange draft = draftChanges.get(i);
                            
                            // Konwertuj String category z draft na ProductCategory enum
                            ProductCategory category;
                            try {
                                category = ProductCategory.valueOf(draft.getCategory());
                            } catch (IllegalArgumentException e) {
                                logger.warn("    Nieprawidłowa kategoria w draft: {}", draft.getCategory());
                                continue; // Pomiń ten draft change
                            }
                            
                            // Konwertuj priceChangeSource
                            String priceChangeSourceStr = null;
                            if (draft.getPriceChangeSource() != null && !draft.getPriceChangeSource().isEmpty()) {
                                try {
                                    PriceChangeSource.valueOf(draft.getPriceChangeSource());
                                    priceChangeSourceStr = draft.getPriceChangeSource();
                                } catch (IllegalArgumentException e) {
                                    logger.warn("    Nieprawidłowe priceChangeSource w draft: {}", draft.getPriceChangeSource());
                                }
                            }
                            
                            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                            
                            int paramIndex = 1;
                            pstmt.setLong(paramIndex++, projectId);
                            pstmt.setObject(paramIndex++, draft.getProductId());
                            pstmt.setString(paramIndex++, category.name());
                            pstmt.setObject(paramIndex++, draft.getDraftRetailPrice());
                            pstmt.setObject(paramIndex++, draft.getDraftPurchasePrice());
                            pstmt.setObject(paramIndex++, draft.getDraftSellingPrice());
                            pstmt.setObject(paramIndex++, draft.getDraftQuantity());
                            pstmt.setString(paramIndex++, priceChangeSourceStr);
                            pstmt.setObject(paramIndex++, draft.getDraftMarginPercent());
                            pstmt.setObject(paramIndex++, draft.getDraftDiscountPercent());
                            pstmt.setTimestamp(paramIndex++, now);
                            pstmt.setTimestamp(paramIndex++, now);
                            
                            pstmt.addBatch();
                        }
                        
                        long batchPrepareEnd = System.currentTimeMillis();
                        long batchPrepareTime = batchPrepareEnd - batchPrepareStart;
                        totalPrepareTime[0] += batchPrepareTime;
                        
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} przygotowany | rekordów: {} | czas przygotowania: {}ms", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchPrepareTime);
                        
                        long batchSaveStart = System.currentTimeMillis();
                        int[] results = pstmt.executeBatch();
                        long batchSaveEnd = System.currentTimeMillis();
                        long batchSaveTime = batchSaveEnd - batchSaveStart;
                        totalSaveTime[0] += batchSaveTime;
                        
                        logger.info("⏱️ [PERFORMANCE] Batch {}/{} zapisany (INSERT ProjectProduct) | rekordów: {} | czas zapisu: {}ms | przetworzonych: {}", 
                                   batchIndex + 1, totalBatches, recordsInBatch, batchSaveTime, results.length);
                        
                        entityManager.flush();
                    }
                } catch (SQLException e) {
                    logger.error("❌ [PERFORMANCE] Błąd podczas batch insert ProjectProduct: {}", e.getMessage(), e);
                    throw new RuntimeException("Błąd podczas batch insert ProjectProduct", e);
                }
            }
        });
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("⏱️ [PERFORMANCE] BATCH INSERT PROJECT PRODUCTS - END | rekordów: {} | batchy: {} | czas całkowity: {}ms | prepare: {}ms | save: {}ms", 
                   totalChanges, totalBatches, duration, totalPrepareTime[0], totalSaveTime[0]);
    }
}

