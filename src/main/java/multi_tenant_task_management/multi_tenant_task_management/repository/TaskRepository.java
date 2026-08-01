package multi_tenant_task_management.multi_tenant_task_management.repository;

import multi_tenant_task_management.multi_tenant_task_management.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // save() and findById() inherited from JpaRepository

    Optional<Task> findByIdAndTenantId(Long id, Long tenantId);

    List<Task> findAllByTenantId(Long tenantId);

    List<Task> findByAssignedToAndTenantId(Long assignedTo, Long tenantId);
}
