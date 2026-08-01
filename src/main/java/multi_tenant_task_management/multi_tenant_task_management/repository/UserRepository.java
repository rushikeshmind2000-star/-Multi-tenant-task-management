package multi_tenant_task_management.multi_tenant_task_management.repository;

import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // save() and findById() inherited from JpaRepository

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findAllByTenantId(Long tenantId);
}
