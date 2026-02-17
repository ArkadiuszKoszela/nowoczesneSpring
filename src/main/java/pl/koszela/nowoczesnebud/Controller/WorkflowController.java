package pl.koszela.nowoczesnebud.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.DTO.*;
import pl.koszela.nowoczesnebud.Service.ClientWorkflowService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@Validated
public class WorkflowController {

    private final ClientWorkflowService clientWorkflowService;

    public WorkflowController(ClientWorkflowService clientWorkflowService) {
        this.clientWorkflowService = clientWorkflowService;
    }

    @GetMapping("/status-catalog")
    public ResponseEntity<List<BusinessStatusResponse>> getStatusCatalog(
            @RequestParam(defaultValue = "true") boolean includeInactive) {
        return ResponseEntity.ok(clientWorkflowService.getStatusCatalog(includeInactive));
    }

    @PostMapping("/status-catalog")
    public ResponseEntity<BusinessStatusResponse> createStatus(@Valid @RequestBody BusinessStatusRequest request) {
        return ResponseEntity.ok(clientWorkflowService.createStatus(request));
    }

    @PutMapping("/status-catalog/{statusId}")
    public ResponseEntity<BusinessStatusResponse> updateStatus(@PathVariable Long statusId,
                                                               @Valid @RequestBody BusinessStatusRequest request) {
        return ResponseEntity.ok(clientWorkflowService.updateStatus(statusId, request));
    }

    @DeleteMapping("/status-catalog/{statusId}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long statusId) {
        clientWorkflowService.deleteStatus(statusId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/status-catalog/reorder")
    public ResponseEntity<Void> reorderStatuses(@Valid @RequestBody ReorderBusinessStatusRequest request) {
        clientWorkflowService.reorderStatuses(request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/projects/{projectId}/status")
    public ResponseEntity<ClientStatusHistoryResponse> changeProjectStatus(@PathVariable Long projectId,
                                                                           @Valid @RequestBody ChangeClientStatusRequest request) {
        Long actorUserId = resolveCurrentActorUserId();
        return ResponseEntity.ok(clientWorkflowService.changeProjectStatus(projectId, request, actorUserId));
    }

    @GetMapping("/projects/{projectId}/status-history")
    public ResponseEntity<List<ClientStatusHistoryResponse>> getProjectStatusHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(clientWorkflowService.getProjectStatusHistory(projectId));
    }

    @GetMapping("/clients/pipeline")
    public ResponseEntity<List<ClientPipelineItemResponse>> getPipeline() {
        return ResponseEntity.ok(clientWorkflowService.getPipeline());
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ProjectTaskResponse> createTask(@PathVariable Long projectId,
                                                          @Valid @RequestBody CreateProjectTaskRequest request) {
        return ResponseEntity.ok(clientWorkflowService.createTask(projectId, request));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<ProjectTaskResponse>> getProjectTasks(@PathVariable Long projectId) {
        return ResponseEntity.ok(clientWorkflowService.getProjectTasks(projectId));
    }

    @PatchMapping("/tasks/{taskId}/done")
    public ResponseEntity<ProjectTaskResponse> markTaskDone(@PathVariable Long taskId) {
        return ResponseEntity.ok(clientWorkflowService.markTaskDone(taskId));
    }

    private Long resolveCurrentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return clientWorkflowService.resolveCurrentActorUserId(authentication.getName()).orElse(null);
    }
}

