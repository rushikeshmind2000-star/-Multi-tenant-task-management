package multi_tenant_task_management.multi_tenant_task_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import multi_tenant_task_management.multi_tenant_task_management.dto.AssignTaskRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.CreateTaskRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.UpdateStatusRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.Role;
import multi_tenant_task_management.multi_tenant_task_management.entity.Task;
import multi_tenant_task_management.multi_tenant_task_management.security.CustomUserDetails;
import multi_tenant_task_management.multi_tenant_task_management.security.TenantContext;
import multi_tenant_task_management.multi_tenant_task_management.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Task management with role-based and tenant-scoped access")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * POST /tasks
     * Role: MANAGER
     * Creates a task. tenant_id is automatically set from JWT — never from request body.
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create task (MANAGER only)", description = "Creates a task under the caller's tenant. tenant_id injected from JWT.")
    public ResponseEntity<Task> createTask(
            @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskService.createTask(request, tenantId);
        return new ResponseEntity<>(task, HttpStatus.CREATED);
    }

    /**
     * PUT /tasks/{id}/assign
     * Role: MANAGER
     * Assigns a task to a user of the same tenant (cross-tenant assignment blocked at service level).
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Assign task (MANAGER only)", description = "Assigns a task to a user. Both task and user must belong to the caller's tenant.")
    public ResponseEntity<Task> assignTask(
            @PathVariable Long id,
            @RequestBody AssignTaskRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskService.assignTask(id, request, tenantId);
        return ResponseEntity.ok(task);
    }

    /**
     * PUT /tasks/{id}/status
     * Role: USER
     * A USER can only update the status of tasks assigned to them.
     * Cross-task modification is blocked at service level.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update task status (USER only)", description = "USER can update status only for tasks assigned to them.")
    public ResponseEntity<Task> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = currentUser.getUserId();
        Task task = taskService.updateStatus(id, request, tenantId, userId);
        return ResponseEntity.ok(task);
    }

    /**
     * GET /tasks
     * Role: USER (assigned tasks only), MANAGER (all tenant tasks), ADMIN (all tenant tasks)
     * Returns tasks filtered by role — all scoped to the caller's tenant.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get tasks (role-based)", description = "USER sees only their assigned tasks. MANAGER and ADMIN see all tasks in their tenant.")
    public ResponseEntity<List<Task>> getTasks(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = currentUser.getUserId();
        Role role = currentUser.getUser().getRole();
        List<Task> tasks = taskService.getTasks(tenantId, userId, role);
        return ResponseEntity.ok(tasks);
    }
}
