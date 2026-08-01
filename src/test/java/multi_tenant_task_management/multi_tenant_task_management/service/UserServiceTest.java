package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.dto.CreateUserRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.Role;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateUserRequest("member@acme.com", "pass123", Role.USER);
    }

    @Test
    @DisplayName("createUser — should create and return user with encoded password")
    void createUser_ShouldCreateAndReturnUser() {
        Long tenantId = 1L;
        when(userRepository.findByEmail("member@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("hashed_pass");
        User savedUser = new User(5L, "member@acme.com", "hashed_pass", Role.USER, tenantId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(request, tenantId);

        assertNotNull(result);
        assertEquals("member@acme.com", result.getEmail());
        assertEquals(Role.USER, result.getRole());
        assertEquals(tenantId, result.getTenantId());
    }

    @Test
    @DisplayName("createUser — should throw when email already exists")
    void createUser_ShouldThrowWhenEmailAlreadyExists() {
        User existing = new User(1L, "member@acme.com", "pass", Role.USER, 1L);
        when(userRepository.findByEmail("member@acme.com")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request, 1L)
        );

        assertTrue(ex.getMessage().contains("member@acme.com"));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("createUser — should encode the password before saving")
    void createUser_ShouldEncodePassword() {
        when(userRepository.findByEmail("member@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("bcrypt_hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(request, 1L);

        verify(passwordEncoder, times(1)).encode("pass123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("bcrypt_hash", userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("createUser — should set tenantId from parameter, not from request")
    void createUser_ShouldSetTenantIdFromParameter() {
        Long tenantId = 42L;
        when(userRepository.findByEmail("member@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(request, tenantId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(tenantId, userCaptor.getValue().getTenantId());
    }

    @Test
    @DisplayName("createUser — should set the correct role from request")
    void createUser_ShouldSetCorrectRole() {
        request.setRole(Role.MANAGER);
        when(userRepository.findByEmail("member@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(request, 1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.MANAGER, userCaptor.getValue().getRole());
    }
}
