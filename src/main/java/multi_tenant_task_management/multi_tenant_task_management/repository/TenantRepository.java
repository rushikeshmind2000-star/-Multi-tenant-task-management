package multi_tenant_task_management.multi_tenant_task_management.repository;

import multi_tenant_task_management.multi_tenant_task_management.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    // save() and findById() inherited from JpaRepository

    Optional<Tenant> findByName(String name);
}
