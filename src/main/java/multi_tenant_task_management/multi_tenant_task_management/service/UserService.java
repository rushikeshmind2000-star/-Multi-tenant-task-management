package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.dto.CreateUserRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.exception.ResourceNotFoundException;
import multi_tenant_task_management.multi_tenant_task_management.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(CreateUserRequest request, Long tenantId) {
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already in use");
        });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setTenantId(tenantId);

        return userRepository.save(user);
    }
}
