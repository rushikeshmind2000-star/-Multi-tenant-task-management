package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.dto.RegisterTenantRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.Role;
import multi_tenant_task_management.multi_tenant_task_management.entity.Tenant;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.repository.TenantRepository;
import multi_tenant_task_management.multi_tenant_task_management.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    
    private final UserRepository userRepository;
    
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Tenant registerTenant(RegisterTenantRequest request) {
        // Check if tenant name already exists
        tenantRepository.findByName(request.getTenantName()).ifPresent(t -> {
            throw new IllegalArgumentException("Tenant name '" + request.getTenantName() + "' is already taken");
        });

        // Check if admin email already exists
        userRepository.findByEmail(request.getAdminEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Email '" + request.getAdminEmail() + "' is already in use");
        });

        // Create and save tenant
        Tenant tenant = new Tenant();
        
        tenant.setName(request.getTenantName());
        
        Tenant savedTenant = tenantRepository.save(tenant);

        // Create admin user for this tenant
        User adminUser = new User();
       
        adminUser.setEmail(request.getAdminEmail());
        
        adminUser.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        
        adminUser.setRole(Role.ADMIN);
        
        adminUser.setTenantId(savedTenant.getId());
        
        userRepository.save(adminUser);

        return savedTenant;
    }
}
