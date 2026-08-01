package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.dto.RegisterTenantRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.Role;
import multi_tenant_task_management.multi_tenant_task_management.entity.Tenant;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.repository.TenantRepository;
import multi_tenant_task_management.multi_tenant_task_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Unit Tests")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantService tenantService;

    private RegisterTenantRequest request;
    private Tenant savedTenant;

    @BeforeEach
    void setUp() {
        request = new RegisterTenantRequest("Acme Corp", "admin@acme.com", "secret123");
        savedTenant = new Tenant(1L, "Acme Corp", LocalDateTime.now());
    }

    @Test
    @DisplayName("registerTenant — should create tenant and admin user successfully")
    void registerTenant_ShouldCreateTenantAndAdminUser() {
        when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret");

        Tenant result = tenantService.registerTenant(request);

        assertNotNull(result);
        assertEquals("Acme Corp", result.getName());
        assertEquals(1L, result.getId());

        // Verify admin user was saved with correct values
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("admin@acme.com", savedUser.getEmail());
        assertEquals("encoded_secret", savedUser.getPassword());
        assertEquals(Role.ADMIN, savedUser.getRole());
        assertEquals(1L, savedUser.getTenantId());
    }

    @Test
    @DisplayName("registerTenant — should throw when tenant name already exists")
    void registerTenant_ShouldThrowWhenTenantNameAlreadyExists() {
        when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.of(savedTenant));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.registerTenant(request)
        );

        assertTrue(ex.getMessage().contains("Acme Corp"));
        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerTenant — should throw when admin email already exists")
    void registerTenant_ShouldThrowWhenAdminEmailAlreadyExists() {
        when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.empty());
        User existingUser = new User(99L, "admin@acme.com", "pass", Role.ADMIN, 2L);
        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.of(existingUser));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.registerTenant(request)
        );

        assertTrue(ex.getMessage().contains("admin@acme.com"));
        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerTenant — should encode the admin password")
    void registerTenant_ShouldEncodeAdminPassword() {
        when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt_hash");

        tenantService.registerTenant(request);

        verify(passwordEncoder).encode("secret123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("bcrypt_hash", userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("registerTenant — should call tenantRepository.save once")
    void registerTenant_ShouldCallTenantSaveOnce() {
        when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        tenantService.registerTenant(request);

        verify(tenantRepository, times(1)).save(any(Tenant.class));
        verify(userRepository, times(1)).save(any(User.class));
    }
}
