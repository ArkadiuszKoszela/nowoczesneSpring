package pl.koszela.nowoczesnebud.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.koszela.nowoczesnebud.DTO.BusinessStatusRequest;
import pl.koszela.nowoczesnebud.Repository.BusinessStatusRepository;
import pl.koszela.nowoczesnebud.Service.ClientWorkflowService;

@Component
public class BusinessStatusInitializer implements CommandLineRunner {

    private final BusinessStatusRepository businessStatusRepository;
    private final ClientWorkflowService clientWorkflowService;

    public BusinessStatusInitializer(BusinessStatusRepository businessStatusRepository,
                                     ClientWorkflowService clientWorkflowService) {
        this.businessStatusRepository = businessStatusRepository;
        this.clientWorkflowService = clientWorkflowService;
    }

    @Override
    public void run(String... args) {
        if (businessStatusRepository.count() > 0) {
            return;
        }

        createDefault("Nowy projekt", "#3B82F6", true, false, false);
        createDefault("Zakwalifikowany", "#6366F1", true, false, false);
        createDefault("Wycena w toku", "#0EA5E9", true, false, false);
        createDefault("Oferta gotowa", "#F59E0B", true, false, false);
        createDefault("Omowione", "#A855F7", true, false, false);
        createDefault("Negocjacje", "#EC4899", true, false, false);
        createDefault("Oczekuje na decyzje", "#F97316", true, false, false);
        createDefault("Wygrana", "#22C55E", false, false, true);
        createDefault("Przegrana", "#EF4444", false, true, true);
    }

    private void createDefault(String name,
                               String color,
                               boolean requiresNextTask,
                               boolean requiresLossReason,
                               boolean terminal) {
        BusinessStatusRequest request = new BusinessStatusRequest();
        request.setName(name);
        request.setColor(color);
        request.setRequiresNextTask(requiresNextTask);
        request.setRequiresLossReason(requiresLossReason);
        request.setTerminal(terminal);
        request.setActive(true);
        clientWorkflowService.createStatus(request);
    }
}

