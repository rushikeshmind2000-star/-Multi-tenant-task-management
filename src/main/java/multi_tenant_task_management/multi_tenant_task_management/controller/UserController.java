package multi_tenant_task_management.multi_tenant_task_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import multi_tenant_task_management.multi_tenant_task_management.dto.CreateUserRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.User;
import multi_tenant_task_management.multi_tenant_task_management.security.TenantContext;
import multi_tenant_task_management.multi_tenant_task_management.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management — ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /users
     * Role: ADMIN
     * Creates a new user under the same tenant.
     * tenant_id is sourced from JWT via TenantContext — never from request body.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user (ADMIN only)", description = "Creates a new user under the caller's tenant. tenant_id is taken from JWT.")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        
    	Long tenantId = TenantContext.getTenantId(); // Always from JWT, never from request
      
        User user = userService.createUser(request, tenantId);
        
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }
}
