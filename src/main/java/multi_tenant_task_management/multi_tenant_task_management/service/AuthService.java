package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.config.JwtUtil;
import multi_tenant_task_management.multi_tenant_task_management.dto.LoginRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.LoginResponse;
import multi_tenant_task_management.multi_tenant_task_management.entity.Tenant;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.exception.ResourceNotFoundException;
import multi_tenant_task_management.multi_tenant_task_management.repository.TenantRepository;
import multi_tenant_task_management.multi_tenant_task_management.repository.UserRepository;
import multi_tenant_task_management.multi_tenant_task_management.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
   
    private final JwtUtil jwtUtil;
    
    private final UserRepository userRepository;
    
    private final TenantRepository tenantRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       TenantRepository tenantRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    public LoginResponse login(LoginRequest request) {
        // 1. Authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Load user details
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getEmail()));

        // 3. Load tenant info
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + user.getTenantId()));

        // 4. Generate JWT
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        // 5. Calculate token timestamps
        LocalDateTime issuedAt  = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(jwtExpirationMs / 1000);

        // 6. Build rich response
        return new LoginResponse(
                token,
                "Bearer",
                issuedAt,
                expiresAt,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getId(),
                tenant.getName()
        );
    }
}
