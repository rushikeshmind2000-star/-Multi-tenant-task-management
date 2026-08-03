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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskService}.
 *
 * Covers:
 *  - createTask: happy path, tenant isolation
 *  - assignTask: happy path, task-not-found, cross-tenant user blocked
 *  - updateStatus: assignee succeeds, non-assignee rejected, unassigned task rejected
 *  - getTasks: ADMIN all-tenant, MANAGER all-tenant, USER own tasks only
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService — Unit Tests")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    // ─── shared constants ──────────────────────────────────────────────────────

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID   = 10L;
    private static final Long TASK_ID   = 100L;

    private Task pendingTask;
    private User assignee;

    @BeforeEach
    void setUp() {
        pendingTask = new Task(TASK_ID, "Fix login bug", "Critical", Status.PENDING, null, TENANT_ID);
        assignee    = new User(USER_ID, "bob@rsm.com", "hash", Role.USER, TENANT_ID);
    }

    // ─── createTask ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("should save task with PENDING status, correct title and tenantId")
        void shouldSaveTaskWithPendingStatusAndTenantId() {
            CreateTaskRequest req = new CreateTaskRequest("Deploy v2", "Deploy to prod");
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.createTask(req, TENANT_ID);

            assertThat(result.getTitle()).isEqualTo("Deploy v2");
            assertThat(result.getDescription()).isEqualTo("Deploy to prod");
            assertThat(result.getStatus()).isEqualTo(Status.PENDING);
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getAssignedTo()).isNull();
        }

        @Test
        @DisplayName("should call taskRepository.save exactly once")
        void shouldCallSaveOnce() {
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.createTask(new CreateTaskRequest("T", "D"), TENANT_ID);

            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        @DisplayName("should isolate task to the given tenantId (not any other tenant)")
        void shouldIsolateTaskToTenant() {
            Long specificTenant = 42L;
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.createTask(new CreateTaskRequest("T", "D"), specificTenant);

            assertThat(result.getTenantId()).isEqualTo(specificTenant);
        }
    }

    // ─── assignTask ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("assignTask()")
    class AssignTask {

        @Test
        @DisplayName("should assign user to task and save")
        void shouldAssignUserToTask() {
            AssignTaskRequest req = new AssignTaskRequest(USER_ID);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));
            when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.assignTask(TASK_ID, req, TENANT_ID);

            assertThat(result.getAssignedTo()).isEqualTo(USER_ID);
            verify(taskRepository).save(pendingTask);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when task not found in tenant")
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignTask(TASK_ID, new AssignTaskRequest(USER_ID), TENANT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(TASK_ID));

            verify(userRepository, never()).findByIdAndTenantId(any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user belongs to a different tenant (cross-tenant blocked)")
        void shouldThrowWhenUserIsFromDifferentTenant() {
            Long foreignUserId = 999L;
            AssignTaskRequest req = new AssignTaskRequest(foreignUserId);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));
            // User 999 does NOT exist in tenant 1
            when(userRepository.findByIdAndTenantId(foreignUserId, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignTask(TASK_ID, req, TENANT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(foreignUserId));

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set assignedTo field on task entity before saving")
        void shouldSetAssignedToField() {
            AssignTaskRequest req = new AssignTaskRequest(USER_ID);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));
            when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.assignTask(TASK_ID, req, TENANT_ID);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedTo()).isEqualTo(USER_ID);
        }
    }

    // ─── updateStatus ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("should update status when caller is the assigned user")
        void shouldUpdateStatusForAssignee() {
            pendingTask.setAssignedTo(USER_ID);
            UpdateStatusRequest req = new UpdateStatusRequest(Status.IN_PROGRESS);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.updateStatus(TASK_ID, req, TENANT_ID, USER_ID);

            assertThat(result.getStatus()).isEqualTo(Status.IN_PROGRESS);
            verify(taskRepository).save(pendingTask);
        }

        @Test
        @DisplayName("should update status to COMPLETED successfully")
        void shouldUpdateStatusToCompleted() {
            pendingTask.setAssignedTo(USER_ID);
            UpdateStatusRequest req = new UpdateStatusRequest(Status.COMPLETED);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.updateStatus(TASK_ID, req, TENANT_ID, USER_ID);

            assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when caller is NOT the assignee")
        void shouldThrowWhenCallerIsNotAssignee() {
            pendingTask.setAssignedTo(777L);  // assigned to someone else
            UpdateStatusRequest req = new UpdateStatusRequest(Status.COMPLETED);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));

            assertThatThrownBy(() -> taskService.updateStatus(TASK_ID, req, TENANT_ID, USER_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when task has no assignee (unassigned)")
        void shouldThrowWhenTaskIsUnassigned() {
            pendingTask.setAssignedTo(null);
            UpdateStatusRequest req = new UpdateStatusRequest(Status.IN_PROGRESS);
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.of(pendingTask));

            assertThatThrownBy(() -> taskService.updateStatus(TASK_ID, req, TENANT_ID, USER_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when task not found")
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.findByIdAndTenantId(TASK_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    taskService.updateStatus(TASK_ID, new UpdateStatusRequest(Status.COMPLETED), TENANT_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(TASK_ID));
        }
    }

    // ─── getTasks ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTasks() — role-based filtering")
    class GetTasks {

        private Task otherTask;

        @BeforeEach
        void moreFixtures() {
            otherTask = new Task(101L, "Write tests", "TDD", Status.PENDING, 20L, TENANT_ID);
        }

        @Test
        @DisplayName("ADMIN → should receive ALL tasks in the tenant")
        void admin_ShouldReceiveAllTenantTasks() {
            List<Task> all = List.of(pendingTask, otherTask);
            when(taskRepository.findAllByTenantId(TENANT_ID)).thenReturn(all);

            List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.ADMIN);

            assertThat(result).hasSize(2).containsExactlyElementsOf(all);
            verify(taskRepository).findAllByTenantId(TENANT_ID);
            verify(taskRepository, never()).findByAssignedToAndTenantId(any(), any());
        }

        @Test
        @DisplayName("MANAGER → should receive ALL tasks in the tenant")
        void manager_ShouldReceiveAllTenantTasks() {
            List<Task> all = List.of(pendingTask);
            when(taskRepository.findAllByTenantId(TENANT_ID)).thenReturn(all);

            List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.MANAGER);

            assertThat(result).hasSize(1);
            verify(taskRepository).findAllByTenantId(TENANT_ID);
            verify(taskRepository, never()).findByAssignedToAndTenantId(any(), any());
        }

        @Test
        @DisplayName("USER → should receive ONLY tasks assigned to them")
        void user_ShouldReceiveOnlyOwnTasks() {
            pendingTask.setAssignedTo(USER_ID);
            List<Task> myTasks = List.of(pendingTask);
            when(taskRepository.findByAssignedToAndTenantId(USER_ID, TENANT_ID)).thenReturn(myTasks);

            List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.USER);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssignedTo()).isEqualTo(USER_ID);
            verify(taskRepository).findByAssignedToAndTenantId(USER_ID, TENANT_ID);
            verify(taskRepository, never()).findAllByTenantId(any());
        }

        @Test
        @DisplayName("USER → should receive empty list if no tasks assigned to them")
        void user_ShouldReceiveEmptyListWhenNoTasksAssigned() {
            when(taskRepository.findByAssignedToAndTenantId(USER_ID, TENANT_ID)).thenReturn(List.of());

            List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.USER);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("USER → should NOT see tasks assigned to other users in the same tenant")
        void user_ShouldNotSeeOtherUsersTasks() {
            // Only USER_ID tasks returned by the repo
            when(taskRepository.findByAssignedToAndTenantId(USER_ID, TENANT_ID)).thenReturn(List.of());

            List<Task> result = taskService.getTasks(TENANT_ID, USER_ID, Role.USER);

            // otherTask assigned to 20L should never appear
            assertThat(result).doesNotContain(otherTask);
        }
    }
}
