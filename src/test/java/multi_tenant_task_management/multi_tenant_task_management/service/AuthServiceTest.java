package multi_tenant_task_management.multi_tenant_task_management.service;

import multi_tenant_task_management.multi_tenant_task_management.config.JwtUtil;
import multi_tenant_task_management.multi_tenant_task_management.dto.LoginRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.LoginResponse;
import multi_tenant_task_management.multi_tenant_task_management.entity.Role;
import multi_tenant_task_management.multi_tenant_task_management.entity.Tenant;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.exception.ResourceNotFoundException;
import multi_tenant_task_management.multi_tenant_task_management.repository.TenantRepository;
import multi_tenant_task_management.multi_tenant_task_management.repository.UserRepository;
import multi_tenant_task_management.multi_tenant_task_management.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private User user;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("user@acme.com", "password");
        user = new User(1L, "user@acme.com", "encoded_password", Role.ADMIN, 10L);
        tenant = new Tenant(10L, "Acme Corp", LocalDateTime.now());
    }

    @Test
    @DisplayName("login — should return JWT token on valid credentials")
    void login_ShouldReturnTokenOnValidCredentials() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByEmail("user@acme.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("jwt.token.here");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("user@acme.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertEquals(1L, response.getUserId());
        assertEquals(10L, response.getTenantId());
        assertEquals("Acme Corp", response.getTenantName());
        assertNotNull(response.getIssuedAt());
        assertNotNull(response.getExpiresAt());
    }

    @Test
    @DisplayName("login — should call authenticationManager with correct credentials")
    void login_ShouldCallAuthenticationManagerWithCredentials() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(userRepository.findByEmail("user@acme.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(loginRequest);

        verify(authenticationManager).authenticate(
                argThat(token ->
                        token instanceof UsernamePasswordAuthenticationToken &&
                        ((UsernamePasswordAuthenticationToken) token).getPrincipal().equals("user@acme.com")
                )
        );
    }

    @Test
    @DisplayName("login — should throw BadCredentialsException on wrong password")
    void login_ShouldThrowOnWrongPassword() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).findByEmail(any());
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    @DisplayName("login — should throw ResourceNotFoundException when user not found after auth")
    void login_ShouldThrowWhenUserNotFoundAfterAuth() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(userRepository.findByEmail("user@acme.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateToken(any());
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    @DisplayName("login — should throw ResourceNotFoundException when tenant not found")
    void login_ShouldThrowWhenTenantNotFound() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(userRepository.findByEmail("user@acme.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("login — should call jwtUtil.generateToken with correct UserDetails")
    void login_ShouldCallGenerateTokenWithUserDetails() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(userRepository.findByEmail("user@acme.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("token123");

        authService.login(loginRequest);

        verify(jwtUtil, times(1)).generateToken(any(CustomUserDetails.class));
    }
}
