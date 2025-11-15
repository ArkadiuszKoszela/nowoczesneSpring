package pl.koszela.nowoczesnebud.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.PriceListSnapshot;
import pl.koszela.nowoczesnebud.Model.ProductCategory;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.ProjectRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;
import pl.koszela.nowoczesnebud.Service.PriceListSnapshotService;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serwis do zarządzania projektami
 * ZAWSZE tworzy nowy projekt (jeśli brak ID), lub aktualizuje istniejący
 * 
 * WAŻNE: Zapisuje SNAPSHOTY produktów do Input, aby każdy projekt miał swoją własną kopię danych
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PriceListSnapshotService priceListSnapshotService;

    public ProjectService(ProjectRepository projectRepository, 
                         UserRepository userRepository,
                         PriceListSnapshotService priceListSnapshotService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.priceListSnapshotService = priceListSnapshotService;
    }

    /**
     * Zapisuje projekt (przeciążona metoda bez formInputsToSave)
     * Zawsze tworzy nowy (jeśli brak ID) lub aktualizuje istniejący
     * Zapisuje TYLKO Input z formularza (category == null), Input produktów są w snapshotach
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
        logger.info("💾 Zapisywanie projektu: {}", project.getProjectName());
        
        // Jeśli projekt ma ID - aktualizuj istniejący
        if (project.getId() != null) {
            logger.info("  Aktualizacja istniejącego projektu ID: {}", project.getId());
            Project existingProject = projectRepository.findById(project.getId())
                .orElseThrow(() -> new RuntimeException("Project not found: " + project.getId()));
            
            // Aktualizuj dane projektu
            existingProject.setProjectName(project.getProjectName());
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
            
            // ⚠️ WAŻNE: Przesyłamy WSZYSTKIE Input (formularza + price-override + group-option)
            // Użyj formInputsToSave jeśli jest podana (z fillQuantitiesFromSnapshot),
            // w przeciwnym razie użyj project.getInputs()
            List<Input> inputsToProcess = formInputsToSave != null ? formInputsToSave :
                (project.getInputs() != null ? project.getInputs() : new ArrayList<>());
            
            // Policz typy Input dla debugowania
            int formInputsCount = 0;
            int priceOverridesCount = 0;
            int groupOptionsCount = 0;
            
            if (formInputsToSave != null) {
                logger.debug("📥 Używam {} Input przekazanych bezpośrednio (z fillQuantitiesFromSnapshot)", 
                           inputsToProcess.size());
            } else if (project.getInputs() != null) {
                logger.debug("📥 Otrzymano {} Input w request", project.getInputs().size());
            }
            
            if (!inputsToProcess.isEmpty()) {
                for (Input input : inputsToProcess) {
                    // ⚠️ WAŻNE: Wyczyść ID - zawsze tworzymy NOWE Input przy zapisie
                    input.setId(null);
                    
                    // Przypisz do projektu
                    input.setProject(existingProject);
                    
                    // Klasyfikuj typ Input
                    if (input.getProductId() != null) {
                        priceOverridesCount++;
                        logger.debug("  📝 Price override: productId={}, manualQuantity={}, manualSellingPrice={}", 
                                   input.getProductId(), input.getManualQuantity(), input.getManualSellingPrice());
                    } else if (input.getGroupManufacturer() != null) {
                        groupOptionsCount++;
                        logger.debug("  📝 Group option: {} / {} → {}", 
                                   input.getGroupManufacturer(), input.getGroupName(), input.getIsMainOption());
                    } else if (input.getMapperName() != null) {
                        formInputsCount++;
                        logger.debug("  📝 Form input: '{}' (mapperName: '{}', quantity: {})", 
                                   input.getName(), input.getMapperName(), input.getQuantity());
                    }
                    
                    // Normalizuj quantity dla Input z formularza: null → 0.0
                    if (input.getQuantity() == null) {
                        input.setQuantity(0.0);
                    }
                    
                    // ⚠️ WAŻNE: Dodaj do istniejącej kolekcji (nie tworz nowej referencji)
                    existingProject.getInputs().add(input);
                }
                
                logger.info("💾 Zapisywanie projektu: {} Input (formularza: {}, price-override: {}, group-option: {})", 
                           inputsToProcess.size(), formInputsCount, priceOverridesCount, groupOptionsCount);
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
        
        // Jeśli brak nazwy projektu - wygeneruj automatycznie
        if (project.getProjectName() == null || project.getProjectName().trim().isEmpty()) {
            project.setProjectName(generateDefaultProjectName(client));
        }
        
        // Ustaw status domyślny jeśli nie ma
        if (project.getStatus() == null) {
            project.setStatus(Project.ProjectStatus.DRAFT);
        }
        
        // ⚠️ WAŻNE: Ustaw snapshotDate jeśli nie ma (domyślnie = createdAt lub teraz)
        if (project.getSnapshotDate() == null) {
            project.setSnapshotDate(project.getCreatedAt() != null ? project.getCreatedAt() : LocalDateTime.now());
            logger.info("  Ustawiono snapshotDate: {}", project.getSnapshotDate());
        }
        
        // Inicjalizuj rabaty jeśli null
        if (project.getTilesMargin() == null) project.setTilesMargin(0.0);
        if (project.getTilesDiscount() == null) project.setTilesDiscount(0.0);
        if (project.getGuttersMargin() == null) project.setGuttersMargin(0.0);
        if (project.getGuttersDiscount() == null) project.setGuttersDiscount(0.0);
        if (project.getAccessoriesMargin() == null) project.setAccessoriesMargin(0.0);
        if (project.getAccessoriesDiscount() == null) project.setAccessoriesDiscount(0.0);
        
        // ⚠️ WAŻNE: Upewnij się że snapshoty istnieją dla kategorii projektu (fallback)
        ensureSnapshotsForProject(project);
        
            // ⚠️ WAŻNE: Wszystkie Input są teraz z formularza (usunęliśmy pola produktowe)
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
     * Pobiera wszystkie projekty dla danego klienta
     */
    public List<Project> getProjectsByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    /**
     * Pobiera projekt po ID z załadowanym klientem i inputami
     * ⚠️ WAŻNE: Projekt używa snapshotu cennika - nie modyfikujemy Input podczas odczytu
     */
    public Project getProjectById(Long id) {
        Project project = projectRepository.findByIdWithClientAndInputs(id)
            .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        
        // Upewnij się że snapshotDate jest ustawione (backward compatibility)
        if (project.getSnapshotDate() == null) {
            project.setSnapshotDate(project.getCreatedAt() != null ? project.getCreatedAt() : LocalDateTime.now());
        }
        
        return project;
    }
    
    /**
     * Upewnij się że snapshoty istnieją dla kategorii projektu (fallback)
     * Jeśli snapshot nie istnieje dla daty projektu, utwórz go z aktualnego stanu cennika
     */
    private void ensureSnapshotsForProject(Project project) {
        LocalDateTime snapshotDate = project.getSnapshotDate();
        if (snapshotDate == null) {
            return;
        }
        
        // Sprawdź snapshoty dla wszystkich kategorii
        for (ProductCategory category : ProductCategory.values()) {
            Optional<PriceListSnapshot> snapshotOpt = priceListSnapshotService.findSnapshotForDate(snapshotDate, category);
            if (!snapshotOpt.isPresent()) {
                // Brak snapshotu - utwórz z aktualnego stanu cennika (fallback)
                logger.info("  ⚠️ Brak snapshotu dla kategorii {} i daty {} - tworzę fallback snapshot", category, snapshotDate);
                try {
                    priceListSnapshotService.createSnapshotForDate(snapshotDate, category);
                    logger.info("  ✅ Utworzono fallback snapshot dla kategorii {}", category);
                } catch (Exception e) {
                    logger.error("  ❌ Błąd tworzenia fallback snapshotu dla kategorii {}: {}", category, e.getMessage(), e);
                }
            }
        }
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
        logger.info("✅ Zaktualizowano klienta ID: {}", savedUser.getId());
        
        return savedUser;
    }

    /**
     * Usuwa klienta (User) wraz z wszystkimi jego projektami
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
        
        // Znajdź wszystkie projekty klienta
        List<Project> userProjects = projectRepository.findByClientId(userId);
        logger.info("  Znaleziono {} projektów dla klienta", userProjects.size());
        
        // Usuń wszystkie projekty klienta (to automatycznie usunie też Input przez cascade)
        if (!userProjects.isEmpty()) {
            logger.info("  Usuwanie {} projektów klienta...", userProjects.size());
            projectRepository.deleteAll(userProjects);
            logger.info("  ✓ Projekty usunięte");
        }
        
        // Usuń klienta
        userRepository.delete(user);
        logger.info("✅ Klient ID {} został usunięty", userId);
    }

    /**
     * Usuwa projekt
     */
    @Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    /**
     * Generuje domyślną nazwę projektu
     */
    private String generateDefaultProjectName(User client) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        if (client != null && client.getName() != null) {
            return "Projekt - " + client.getName() + " - " + date;
        }
        return "Projekt - " + date;
    }
}

