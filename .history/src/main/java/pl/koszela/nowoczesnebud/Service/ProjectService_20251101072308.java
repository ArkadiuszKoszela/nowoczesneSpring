package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Model.Project;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Repository.ProjectRepository;
import pl.koszela.nowoczesnebud.Repository.UserRepository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serwis do zarządzania projektami
 * ZAWSZE tworzy nowy projekt (jeśli brak ID), lub aktualizuje istniejący
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final InputService inputService;

    public ProjectService(ProjectRepository projectRepository, 
                         UserRepository userRepository,
                         InputService inputService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.inputService = inputService;
    }

    /**
     * Zapisuje projekt - zawsze tworzy nowy (jeśli brak ID) lub aktualizuje istniejący
     */
    @Transactional
    public Project save(Project project) {
        // Jeśli projekt ma ID - aktualizuj istniejący
        if (project.getId() != null) {
            Project existingProject = projectRepository.findById(project.getId())
                .orElseThrow(() -> new RuntimeException("Project not found: " + project.getId()));
            
            // Aktualizuj dane
            existingProject.setProjectName(project.getProjectName());
            existingProject.setStatus(project.getStatus());
            existingProject.setClient(project.getClient());
            
            // Usuń stare inputy i dodaj nowe
            existingProject.getInputs().clear();
            if (project.getInputs() != null) {
                for (Input input : project.getInputs()) {
                    input.setProject(existingProject);
                    // Normalizuj quantity: null → 0.0
                    if (input.getQuantity() == null) {
                        input.setQuantity(0.0);
                    }
                    existingProject.getInputs().add(input);
                }
            }
            
            return projectRepository.save(existingProject);
        }
        
        // Nowy projekt - upewnij się że client jest zapisany w bazie
        User client = project.getClient();
        if (client != null && (client.getId() == null || client.getId() == 0)) {
            // Zapisz nowego klienta
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
        
        // Zapisz inputy z przypisaniem do projektu
        // Normalizuj quantity: null → 0.0 dla wszystkich inputów
        if (project.getInputs() != null && !project.getInputs().isEmpty()) {
            for (Input input : project.getInputs()) {
                input.setProject(project);
                // Upewnij się że quantity nie jest null (normalizuj do 0.0)
                if (input.getQuantity() == null) {
                    input.setQuantity(0.0);
                }
            }
        }
        
        return projectRepository.save(project);
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
     */
    public Project getProjectById(Long id) {
        return projectRepository.findByIdWithClientAndInputs(id)
            .orElseThrow(() -> new RuntimeException("Project not found: " + id));
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

