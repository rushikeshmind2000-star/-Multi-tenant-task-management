package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.dto.AssignTaskRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.CreateTaskRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.UpdateStatusRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.Role;
import multi_tenant_task_management.multi_tenant_task_management.entity.Status;
import multi_tenant_task_management.multi_tenant_task_management.entity.Task;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.exception.ResourceNotFoundException;
import multi_tenant_task_management.multi_tenant_task_management.repository.TaskRepository;
import multi_tenant_task_management.multi_tenant_task_management.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a task scoped to the given tenant.
     * tenant_id comes from TenantContext (JWT) — never from the request body.
     */
    @Transactional
    public Task createTask(CreateTaskRequest request, Long tenantId) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(Status.PENDING);
        task.setTenantId(tenantId);
        return taskRepository.save(task);
    }

    /**
     * Assigns a task to a user.
     * Both the task and the target user must belong to the same tenant — cross-tenant assignment is blocked.
     */
    @Transactional
    public Task assignTask(Long taskId, AssignTaskRequest request, Long tenantId) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Validate that assignee belongs to the same tenant (cross-tenant protection)
        User assignee = userRepository.findByIdAndTenantId(request.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId() + " in your tenant"));

        task.setAssignedTo(assignee.getId());
        return taskRepository.save(task);
    }

    /**
     * Updates a task's status.
     * SECURITY: Only the assigned user can update their own task's status.
     * Any attempt to update a task not assigned to the caller is rejected.
     */
    @Transactional
    public Task updateStatus(Long taskId, UpdateStatusRequest request, Long tenantId, Long userId) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Enforce: user can only update tasks assigned to them
        if (task.getAssignedTo() == null || !task.getAssignedTo().equals(userId)) {
            throw new AccessDeniedException("You can only update the status of tasks assigned to you");
        }

        task.setStatus(request.getStatus());
        return taskRepository.save(task);
    }

    /**
     * Returns tasks based on role:
     * - USER: only tasks assigned to them (within their tenant)
     * - MANAGER / ADMIN: all tasks within their tenant
     *
     * All queries are filtered by tenant_id — cross-tenant data is never returned.
     */
    public List<Task> getTasks(Long tenantId, Long userId, Role role) {
        if (role == Role.USER) {
            // USER sees only their assigned tasks
            return taskRepository.findByAssignedToAndTenantId(userId, tenantId);
        }
        // MANAGER and ADMIN see all tasks within the tenant
        return taskRepository.findAllByTenantId(tenantId);
    }
}
