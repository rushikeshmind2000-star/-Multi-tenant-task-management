package multi_tenant_task_management.multi_tenant_task_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import multi_tenant_task_management.multi_tenant_task_management.dto.LoginRequest;
import multi_tenant_task_management.multi_tenant_task_management.dto.LoginResponse;
import multi_tenant_task_management.multi_tenant_task_management.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication — returns JWT with tenant_id and role")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/login
     * Public — authenticates user credentials and returns a JWT.
     * The JWT payload includes: email (sub), tenant_id, role, userId.
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate and receive JWT. Use the token in Authorization: Bearer <token> for all secured endpoints.")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
