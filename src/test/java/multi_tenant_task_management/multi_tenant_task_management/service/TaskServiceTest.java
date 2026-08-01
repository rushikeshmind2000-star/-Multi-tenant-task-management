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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long TASK_ID = 100L;

    private Task task;
    private User user;

    @BeforeEach
    void setUp() {
        task = new Task(TASK_ID, "Fix bug", "Critical issue", Status.PENDING, null, TENANT_ID);
        user = new User(USER_ID, "user@acme.com", "pass", Role.USER, TENANT_ID);
    }

    // ─────────────────────────────────────────────────────────────
    // createTask
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTask — should save task with PENDING status and correct tenantId")
    void createTask_ShouldSaveWithPendingStatusAndTenantId() {
        CreateTaskRequest request = new CreateTaskRequest("New Task", "Description here");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(request, TENANT_ID);

        assertNotNull(result);
        assertEquals("New Task", result.getTitle());
        assertEquals("Description here", result.getDescription());
        assertEquals(Status.PENDING, result.getStatus());
        assertEquals(TENANT_ID, result.getTenantId());
        assertNull(result.getAssignedTo());
    }

    @Test
    @DisplayName("createTask — should call taskRepository.save once")
    void createTask_ShouldCallSaveOnce() {
        CreateTaskRequest request = new CreateTaskRequest("Task", "Desc");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.createTask(request, TENANT_ID);

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    // ─────────────────────────────────────────────────────────────
    // assignTask
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignTask — should assign user to task within same tenant")
    void assignTask_ShouldAssignUserToTask() {
        AssignTaskRequest request = new AssignTaskRequest(USER_ID);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(task));
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.assignTask(TASK_ID, request, TENANT_ID);

        assertEquals(USER_ID, result.getAssignedTo());
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("assignTask — should throw ResourceNotFoundException when task not found")
    void assignTask_ShouldThrowWhenTaskNotFound() {
        AssignTaskRequest request = new AssignTaskRequest(USER_ID);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.assignTask(TASK_ID, request, TENANT_ID));
        verify(userRepository, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    @DisplayName("assignTask — should throw when user belongs to different tenant (cross-tenant protection)")
    void assignTask_ShouldThrowWhenUserNotInSameTenant() {
        AssignTaskRequest request = new AssignTaskRequest(99L);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(task));
        // User 99 doesn't exist in tenant 1
        when(userRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.assignTask(TASK_ID, request, TENANT_ID));
        verify(taskRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus — should succeed when user is the assignee")
    void updateStatus_ShouldSucceedWhenUserIsAssignee() {
        task.setAssignedTo(USER_ID);
        UpdateStatusRequest request = new UpdateStatusRequest(Status.IN_PROGRESS);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateStatus(TASK_ID, request, TENANT_ID, USER_ID);

        assertEquals(Status.IN_PROGRESS, result.getStatus());
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("updateStatus — should throw AccessDeniedException when user is NOT the assignee")
    void updateStatus_ShouldThrowWhenUserIsNotAssignee() {
        task.setAssignedTo(999L); // Assigned to a different user
        UpdateStatusRequest request = new UpdateStatusRequest(Status.COMPLETED);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class,
                () -> taskService.updateStatus(TASK_ID, request, TENANT_ID, USER_ID));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus — should throw AccessDeniedException when task has no assignee")
    void updateStatus_ShouldThrowWhenTaskHasNoAssignee() {
        task.setAssignedTo(null); // Unassigned task
        UpdateStatusRequest request = new UpdateStatusRequest(Status.COMPLETED);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class,
                () -> taskService.updateStatus(TASK_ID, request, TENANT_ID, USER_ID));
    }

    @Test
    @DisplayName("updateStatus — should throw ResourceNotFoundException when task not found")
    void updateStatus_ShouldThrowWhenTaskNotFound() {
        UpdateStatusRequest request = new UpdateStatusRequest(Status.COMPLETED);
        when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateStatus(TASK_ID, request, TENANT_ID, USER_ID));
    }

    // ─────────────────────────────────────────────────────────────
    // getTasks (role-aware)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTasks — ADMIN should receive all tenant tasks")
    void getTasks_AsAdmin_ShouldReturnAllTenantTasks() {
        List<Task> allTasks = List.of(task, new Task(101L, "Other", "desc", Status.PENDING, 20L, TENANT_ID));
        when(taskRepository.findAllByTenantId(TENANT_ID)).thenReturn(allTasks);

        List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.ADMIN);

        assertEquals(2, result.size());
        verify(taskRepository).findAllByTenantId(TENANT_ID);
        verify(taskRepository, never()).findByAssignedToAndTenantId(any(), any());
    }

    @Test
    @DisplayName("getTasks — MANAGER should receive all tenant tasks")
    void getTasks_AsManager_ShouldReturnAllTenantTasks() {
        List<Task> allTasks = List.of(task);
        when(taskRepository.findAllByTenantId(TENANT_ID)).thenReturn(allTasks);

        List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.MANAGER);

        assertEquals(1, result.size());
        verify(taskRepository).findAllByTenantId(TENANT_ID);
        verify(taskRepository, never()).findByAssignedToAndTenantId(any(), any());
    }

    @Test
    @DisplayName("getTasks — USER should receive only their assigned tasks")
    void getTasks_AsUser_ShouldReturnOnlyAssignedTasks() {
        task.setAssignedTo(USER_ID);
        List<Task> assignedTasks = List.of(task);
        when(taskRepository.findByAssignedToAndTenantId(USER_ID, TENANT_ID)).thenReturn(assignedTasks);

        List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.USER);

        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getAssignedTo());
        verify(taskRepository).findByAssignedToAndTenantId(USER_ID, TENANT_ID);
        verify(taskRepository, never()).findAllByTenantId(any());
    }

    @Test
    @DisplayName("getTasks — USER should NOT see tasks assigned to other users in the same tenant")
    void getTasks_AsUser_ShouldNotSeeOtherUsersTasks() {
        // Only task assigned to USER_ID is returned by repository
        when(taskRepository.findByAssignedToAndTenantId(USER_ID, TENANT_ID)).thenReturn(List.of());

        List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.USER);

        assertTrue(result.isEmpty());
        verify(taskRepository).findByAssignedToAndTenantId(USER_ID, TENANT_ID);
    }
}
