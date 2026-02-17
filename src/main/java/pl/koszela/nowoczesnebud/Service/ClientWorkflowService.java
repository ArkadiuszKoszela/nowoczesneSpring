package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.DTO.*;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientWorkflowService {

    private final BusinessStatusRepository businessStatusRepository;
    private final ClientStatusHistoryRepository clientStatusHistoryRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;

    public ClientWorkflowService(BusinessStatusRepository businessStatusRepository,
                                 ClientStatusHistoryRepository clientStatusHistoryRepository,
                                 ProjectTaskRepository projectTaskRepository,
                                 ProjectRepository projectRepository,
                                 AppUserRepository appUserRepository) {
        this.businessStatusRepository = businessStatusRepository;
        this.clientStatusHistoryRepository = clientStatusHistoryRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<BusinessStatusResponse> getStatusCatalog(boolean includeInactive) {
        List<BusinessStatus> statuses = includeInactive
                ? businessStatusRepository.findAllByOrderBySortOrderAscIdAsc()
                : businessStatusRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        return statuses.stream().map(this::mapStatus).collect(Collectors.toList());
    }

    @Transactional
    public BusinessStatusResponse createStatus(BusinessStatusRequest request) {
        BusinessStatus status = new BusinessStatus();
        status.setName(request.getName().trim());
        status.setCode(buildUniqueCode(request.getName()));
        status.setColor(resolveColor(request.getColor()));
        status.setActive(Boolean.TRUE.equals(request.getActive()));
        status.setRequiresNextTask(Boolean.TRUE.equals(request.getRequiresNextTask()));
        status.setRequiresLossReason(Boolean.TRUE.equals(request.getRequiresLossReason()));
        status.setTerminal(Boolean.TRUE.equals(request.getTerminal()));
        status.setSortOrder(nextSortOrder());
        return mapStatus(businessStatusRepository.save(status));
    }

    @Transactional
    public BusinessStatusResponse updateStatus(Long statusId, BusinessStatusRequest request) {
        BusinessStatus status = businessStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status nie istnieje"));

        String newName = request.getName().trim();
        status.setName(newName);
        status.setColor(resolveColor(request.getColor()));
        status.setActive(Boolean.TRUE.equals(request.getActive()));
        status.setRequiresNextTask(Boolean.TRUE.equals(request.getRequiresNextTask()));
        status.setRequiresLossReason(Boolean.TRUE.equals(request.getRequiresLossReason()));
        status.setTerminal(Boolean.TRUE.equals(request.getTerminal()));
        return mapStatus(businessStatusRepository.save(status));
    }

    @Transactional
    public void deleteStatus(Long statusId) {
        BusinessStatus status = businessStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status nie istnieje"));

        if (clientStatusHistoryRepository.existsByToStatusId(statusId)) {
            status.setActive(false);
            businessStatusRepository.save(status);
            return;
        }
        businessStatusRepository.delete(status);
    }

    @Transactional
    public void reorderStatuses(ReorderBusinessStatusRequest request) {
        List<Long> orderedIds = request.getOrderedStatusIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("Lista statusów do sortowania nie może być pusta");
        }

        Map<Long, BusinessStatus> statusesById = businessStatusRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(BusinessStatus::getId, item -> item));

        if (statusesById.size() != orderedIds.size()) {
            throw new IllegalArgumentException("Nie wszystkie statusy istnieją");
        }

        int order = 1;
        for (Long statusId : orderedIds) {
            BusinessStatus status = statusesById.get(statusId);
            status.setSortOrder(order++);
        }
        businessStatusRepository.saveAll(statusesById.values());
    }

    @Transactional
    public ClientStatusHistoryResponse changeProjectStatus(Long projectId, ChangeClientStatusRequest request, Long actorUserId) {
        Project project = projectRepository.findByIdWithClient(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nie istnieje"));

        BusinessStatus targetStatus = businessStatusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new IllegalArgumentException("Status nie istnieje"));
        if (!Boolean.TRUE.equals(targetStatus.getActive())) {
            throw new IllegalArgumentException("Nie można ustawić nieaktywnego statusu");
        }

        String note = trimToNull(request.getNote());
        validateGateRules(targetStatus, note);

        ClientStatusHistory last = clientStatusHistoryRepository.findTopByProjectIdAndToStatusIsNotNullOrderByCreatedAtDescIdDesc(projectId)
                .orElse(null);

        ClientStatusHistory history = new ClientStatusHistory();
        history.setProject(project);
        history.setClient(project.getClient());
        history.setFromStatus(last != null ? last.getToStatus() : null);
        history.setToStatus(targetStatus);
        history.setSource(StatusHistorySource.MANUAL);
        history.setChangedByUserId(actorUserId);
        history.setNote(note);
        ClientStatusHistory saved = clientStatusHistoryRepository.save(history);

        if (trimToNull(request.getNextTaskTitle()) != null) {
            createTaskInternal(project, request.getNextTaskTitle(), null,
                    request.getNextTaskDueAt(), request.getNextTaskType(), request.getNextTaskPriority(), false);
        } else {
            ensureNextTaskIfRequired(project, targetStatus);
        }

        applyStatusAutomations(project, targetStatus);

        return mapHistory(saved);
    }

    public List<ClientStatusHistoryResponse> getProjectStatusHistory(Long projectId) {
        return clientStatusHistoryRepository.findByProjectIdOrderByCreatedAtDescIdDesc(projectId).stream()
                .map(this::mapHistory)
                .collect(Collectors.toList());
    }

    public List<ClientPipelineItemResponse> getPipeline() {
        List<Project> projects = projectRepository.findAllWithClient();
        List<ClientPipelineItemResponse> response = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Project project : projects) {
            ClientStatusHistory current = clientStatusHistoryRepository
                    .findTopByProjectIdAndToStatusIsNotNullOrderByCreatedAtDescIdDesc(project.getId())
                    .orElse(null);

            List<ProjectTask> tasks = projectTaskRepository.findByProjectIdOrderByDueAtAscCreatedAtDesc(project.getId());
            ProjectTask nextOpenTask = tasks.stream()
                    .filter(t -> t.getStatus() == ProjectTaskStatus.OPEN)
                    .findFirst()
                    .orElse(null);
            boolean atRisk = nextOpenTask != null && nextOpenTask.getDueAt() != null && nextOpenTask.getDueAt().isBefore(now);

            String clientName = ((project.getClient().getName() != null ? project.getClient().getName() : "") + " "
                    + (project.getClient().getSurname() != null ? project.getClient().getSurname() : "")).trim();

            response.add(new ClientPipelineItemResponse(
                    project.getId(),
                    project.getClient().getId(),
                    clientName,
                    project.getClient().getEmail(),
                    project.getClient().getTelephoneNumber(),
                    current != null && current.getToStatus() != null ? current.getToStatus().getId() : null,
                    current != null && current.getToStatus() != null ? current.getToStatus().getName() : null,
                    current != null && current.getToStatus() != null ? current.getToStatus().getColor() : null,
                    current != null ? current.getCreatedAt() : null,
                    nextOpenTask != null ? nextOpenTask.getId() : null,
                    nextOpenTask != null ? nextOpenTask.getTitle() : null,
                    nextOpenTask != null ? nextOpenTask.getDueAt() : null,
                    nextOpenTask != null ? nextOpenTask.getStatus().name() : null,
                    atRisk
            ));
        }

        response.sort(Comparator.comparing(
                ClientPipelineItemResponse::getStatusUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return response;
    }

    @Transactional
    public ProjectTaskResponse createTask(Long projectId, CreateProjectTaskRequest request) {
        Project project = projectRepository.findByIdWithClient(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nie istnieje"));
        ProjectTask task = createTaskInternal(project, request.getTitle(), request.getNote(), request.getDueAt(),
                request.getType(), request.getPriority(), Boolean.TRUE.equals(request.getAutoCreated()));
        return mapTask(task);
    }

    public List<ProjectTaskResponse> getProjectTasks(Long projectId) {
        return projectTaskRepository.findByProjectIdOrderByDueAtAscCreatedAtDesc(projectId).stream()
                .map(this::mapTask)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectTaskResponse markTaskDone(Long taskId) {
        ProjectTask task = projectTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Zadanie nie istnieje"));
        task.setStatus(ProjectTaskStatus.DONE);
        task.setCompletedAt(LocalDateTime.now());
        return mapTask(projectTaskRepository.save(task));
    }

    @Transactional
    public void appendCalendarHistory(Long projectId,
                                      Long clientId,
                                      String calendarEventId,
                                      String summary,
                                      LocalDateTime eventAt,
                                      String status,
                                      String calendarDescription,
                                      Long actorUserId) {
        if (projectId == null || clientId == null) {
            return;
        }

        Optional<Project> projectOpt = projectRepository.findByIdWithClient(projectId);
        if (projectOpt.isEmpty()) {
            return;
        }

        Project project = projectOpt.get();
        if (project.getClient() == null || !Objects.equals(project.getClient().getId(), clientId)) {
            return;
        }

        ClientStatusHistory history = new ClientStatusHistory();
        history.setProject(project);
        history.setClient(project.getClient());
        history.setFromStatus(null);
        history.setToStatus(null);
        history.setSource(StatusHistorySource.GOOGLE_CALENDAR);
        history.setChangedByUserId(actorUserId);
        history.setCalendarEventId(trimToNull(calendarEventId));
        history.setCalendarSummary(trimToNull(summary));
        history.setEventAt(eventAt);
        String extractedDescription = extractUserCalendarDescription(calendarDescription);
        history.setNote(trimToNull(extractedDescription != null ? extractedDescription : ("Google Calendar: " + (status == null ? "updated" : status))));
        clientStatusHistoryRepository.save(history);
    }

    public Optional<Long> resolveCurrentActorUserId(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByUsername(username).map(AppUser::getId);
    }

    @Transactional
    public int createOverdueAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<ProjectTask> overdueTasks = projectTaskRepository.findByStatusAndDueAtBefore(ProjectTaskStatus.OPEN, now);
        int created = 0;

        for (ProjectTask task : overdueTasks) {
            if (task.getProject() == null || task.getProject().getId() == null) {
                continue;
            }
            String alertTitle = "ALERT: zalegle zadanie - " + task.getTitle();
            boolean exists = hasOpenTaskWithExactTitle(task.getProject().getId(), alertTitle);
            if (exists) {
                continue;
            }
            createTaskInternal(
                    task.getProject(),
                    alertTitle,
                    "Zadanie automatyczne dla przeterminowanego kroku.",
                    LocalDateTime.now().plusHours(1),
                    ProjectTaskType.FOLLOW_UP,
                    ProjectTaskPriority.HIGH,
                    true
            );
            created++;
        }

        return created;
    }

    private void validateGateRules(BusinessStatus targetStatus, String note) {
        if (Boolean.TRUE.equals(targetStatus.getRequiresLossReason()) && note == null) {
            throw new IllegalArgumentException("Ten status wymaga podania notatki/powodu");
        }
    }

    private void ensureNextTaskIfRequired(Project project, BusinessStatus targetStatus) {
        if (!Boolean.TRUE.equals(targetStatus.getRequiresNextTask())) {
            return;
        }

        boolean hasOpenFutureTask = projectTaskRepository.existsByProjectIdAndStatusAndDueAtAfter(
                project.getId(), ProjectTaskStatus.OPEN, LocalDateTime.now());
        if (hasOpenFutureTask) {
            return;
        }

        // Zamiast blokować handlowca wyjątkiem, twórz automatyczny "next step".
        // Zachowuje to regułę procesu (zawsze jest kolejny krok), a UX jest płynny.
        createTaskInternal(
                project,
                "Follow-up po statusie: " + targetStatus.getName(),
                "Zadanie utworzone automatycznie, bo status wymaga kolejnego kroku.",
                LocalDateTime.now().plusDays(1),
                ProjectTaskType.FOLLOW_UP,
                ProjectTaskPriority.MEDIUM,
                true
        );
    }

    private void applyStatusAutomations(Project project, BusinessStatus targetStatus) {
        String code = targetStatus.getCode() != null ? targetStatus.getCode().toUpperCase(Locale.ROOT) : "";
        if (code.isBlank()) {
            return;
        }

        switch (code) {
            case "NOWY_PROJEKT":
                createAutoTaskIfMissing(
                        project,
                        "Telefon w 24h",
                        "Pierwszy kontakt po rejestracji projektu.",
                        LocalDateTime.now().plusHours(24),
                        ProjectTaskType.CALL,
                        ProjectTaskPriority.HIGH
                );
                break;
            case "OFERTA_GOTOWA":
                createAutoTaskIfMissing(
                        project,
                        "Umow rozmowe ofertowa",
                        "Omowienie gotowej oferty z klientem.",
                        LocalDateTime.now().plusDays(1),
                        ProjectTaskType.OFFER_MEETING,
                        ProjectTaskPriority.HIGH
                );
                break;
            case "OCZEKUJE_NA_DECYZJE":
                createAutoTaskIfMissing(
                        project,
                        "Follow-up D+2",
                        "Przypomnienie po 2 dniach od oczekiwania na decyzje.",
                        LocalDateTime.now().plusDays(2),
                        ProjectTaskType.FOLLOW_UP,
                        ProjectTaskPriority.MEDIUM
                );
                createAutoTaskIfMissing(
                        project,
                        "Follow-up D+5",
                        "Zapytanie o decyzje po 5 dniach.",
                        LocalDateTime.now().plusDays(5),
                        ProjectTaskType.FOLLOW_UP,
                        ProjectTaskPriority.MEDIUM
                );
                createAutoTaskIfMissing(
                        project,
                        "Follow-up D+10",
                        "Dodatkowe wsparcie: realizacje/referencje.",
                        LocalDateTime.now().plusDays(10),
                        ProjectTaskType.FOLLOW_UP,
                        ProjectTaskPriority.MEDIUM
                );
                createAutoTaskIfMissing(
                        project,
                        "Follow-up D+14",
                        "Decyzja koncowa: archiwizacja lub negocjacje.",
                        LocalDateTime.now().plusDays(14),
                        ProjectTaskType.FOLLOW_UP,
                        ProjectTaskPriority.HIGH
                );
                break;
            case "WYGRANA":
                createAutoTaskIfMissing(
                        project,
                        "Potwierdz logistyke",
                        "Potwierdz termin i logistyke realizacji.",
                        LocalDateTime.now().plusDays(1),
                        ProjectTaskType.LOGISTICS,
                        ProjectTaskPriority.HIGH
                );
                createAutoTaskIfMissing(
                        project,
                        "Telefon satysfakcyjny +7",
                        "Telefon po montazu.",
                        LocalDateTime.now().plusDays(7),
                        ProjectTaskType.AFTER_SALES,
                        ProjectTaskPriority.MEDIUM
                );
                createAutoTaskIfMissing(
                        project,
                        "Prosba o opinie +30",
                        "Prosba o opinie po realizacji.",
                        LocalDateTime.now().plusDays(30),
                        ProjectTaskType.AFTER_SALES,
                        ProjectTaskPriority.MEDIUM
                );
                createAutoTaskIfMissing(
                        project,
                        "Zapytanie o polecenie +45",
                        "Proaktywny follow-up o polecenia.",
                        LocalDateTime.now().plusDays(45),
                        ProjectTaskType.AFTER_SALES,
                        ProjectTaskPriority.MEDIUM
                );
                break;
            default:
                break;
        }
    }

    private void createAutoTaskIfMissing(Project project,
                                         String title,
                                         String note,
                                         LocalDateTime dueAt,
                                         ProjectTaskType type,
                                         ProjectTaskPriority priority) {
        if (hasOpenTaskWithExactTitle(project.getId(), title)) {
            return;
        }
        createTaskInternal(project, title, note, dueAt, type, priority, true);
    }

    private boolean hasOpenTaskWithExactTitle(Long projectId, String title) {
        if (projectId == null || title == null) {
            return false;
        }
        List<ProjectTask> openTasks = projectTaskRepository.findByProjectIdAndStatusOrderByDueAtAscCreatedAtDesc(
                projectId,
                ProjectTaskStatus.OPEN
        );
        return openTasks.stream().anyMatch(task -> title.equalsIgnoreCase(task.getTitle()));
    }

    private ProjectTask createTaskInternal(Project project,
                                           String title,
                                           String note,
                                           LocalDateTime dueAt,
                                           ProjectTaskType type,
                                           ProjectTaskPriority priority,
                                           boolean autoCreated) {
        String normalizedTitle = trimToNull(title);
        if (normalizedTitle == null) {
            throw new IllegalArgumentException("Tytuł zadania jest wymagany");
        }
        if (dueAt == null) {
            throw new IllegalArgumentException("Termin zadania jest wymagany");
        }

        ProjectTask task = new ProjectTask();
        task.setProject(project);
        task.setClient(project.getClient());
        task.setTitle(normalizedTitle);
        task.setNote(trimToNull(note));
        task.setDueAt(dueAt);
        task.setType(type != null ? type : ProjectTaskType.OTHER);
        task.setPriority(priority != null ? priority : ProjectTaskPriority.MEDIUM);
        task.setStatus(ProjectTaskStatus.OPEN);
        task.setAutoCreated(autoCreated);
        return projectTaskRepository.save(task);
    }

    private BusinessStatusResponse mapStatus(BusinessStatus status) {
        return new BusinessStatusResponse(
                status.getId(),
                status.getName(),
                status.getCode(),
                status.getColor(),
                status.getSortOrder(),
                status.getActive(),
                status.getRequiresNextTask(),
                status.getRequiresLossReason(),
                status.getTerminal()
        );
    }

    private ClientStatusHistoryResponse mapHistory(ClientStatusHistory history) {
        return new ClientStatusHistoryResponse(
                history.getId(),
                history.getProject() != null ? history.getProject().getId() : null,
                history.getClient() != null ? history.getClient().getId() : null,
                history.getFromStatus() != null ? history.getFromStatus().getId() : null,
                history.getFromStatus() != null ? history.getFromStatus().getName() : null,
                history.getToStatus() != null ? history.getToStatus().getId() : null,
                history.getToStatus() != null ? history.getToStatus().getName() : null,
                history.getNote(),
                history.getSource() != null ? history.getSource().name() : null,
                history.getCalendarEventId(),
                history.getCalendarSummary(),
                history.getEventAt(),
                history.getCreatedAt()
        );
    }

    private ProjectTaskResponse mapTask(ProjectTask task) {
        return new ProjectTaskResponse(
                task.getId(),
                task.getProject() != null ? task.getProject().getId() : null,
                task.getClient() != null ? task.getClient().getId() : null,
                task.getTitle(),
                task.getNote(),
                task.getType() != null ? task.getType().name() : null,
                task.getStatus() != null ? task.getStatus().name() : null,
                task.getPriority() != null ? task.getPriority().name() : null,
                task.getDueAt(),
                task.getAutoCreated(),
                task.getCompletedAt(),
                task.getCreatedAt()
        );
    }

    private Integer nextSortOrder() {
        return businessStatusRepository.findTopByOrderBySortOrderDescIdDesc()
                .map(item -> item.getSortOrder() + 1)
                .orElse(1);
    }

    private String buildUniqueCode(String name) {
        String normalized = normalizeCode(name);
        String candidate = normalized;
        int i = 2;
        while (businessStatusRepository.existsByCodeIgnoreCase(candidate)) {
            candidate = normalized + "_" + i++;
        }
        return candidate;
    }

    private String normalizeCode(String value) {
        String ascii = value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('Ą', 'A')
                .replace('Ć', 'C')
                .replace('Ę', 'E')
                .replace('Ł', 'L')
                .replace('Ń', 'N')
                .replace('Ó', 'O')
                .replace('Ś', 'S')
                .replace('Ź', 'Z')
                .replace('Ż', 'Z');
        ascii = ascii.replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (ascii.isBlank()) {
            return "STATUS";
        }
        return ascii;
    }

    private String resolveColor(String color) {
        String normalized = trimToNull(color);
        if (normalized == null) {
            return "#64748B";
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractUserCalendarDescription(String description) {
        String normalized = trimToNull(description);
        if (normalized == null) {
            return null;
        }
        // Usuń automatyczny blok techniczny z danymi klienta, zostawiając tylko notatkę użytkownika.
        String withoutClientBlock = normalized.replaceAll("\\n?-{10,}\\s*\\nDANE KLIENTA[\\s\\S]*$", "").trim();
        return withoutClientBlock.isEmpty() ? null : withoutClientBlock;
    }
}

