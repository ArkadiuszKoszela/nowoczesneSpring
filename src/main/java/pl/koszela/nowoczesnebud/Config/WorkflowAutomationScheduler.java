package pl.koszela.nowoczesnebud.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.koszela.nowoczesnebud.Service.ClientWorkflowService;

@Component
public class WorkflowAutomationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowAutomationScheduler.class);

    private final ClientWorkflowService clientWorkflowService;

    public WorkflowAutomationScheduler(ClientWorkflowService clientWorkflowService) {
        this.clientWorkflowService = clientWorkflowService;
    }

    // Co 15 minut twórz alerty dla przeterminowanych otwartych zadań.
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void processOverdueTasks() {
        try {
            int created = clientWorkflowService.createOverdueAlerts();
            if (created > 0) {
                logger.info("Workflow automation: utworzono {} alertow dla zaleglych zadan", created);
            }
        } catch (Exception ex) {
            logger.error("Workflow automation scheduler failed: {}", ex.getMessage(), ex);
        }
    }
}

