package multi_tenant_task_management.multi_tenant_task_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import multi_tenant_task_management.multi_tenant_task_management.dto.RegisterTenantRequest;
import multi_tenant_task_management.multi_tenant_task_management.entity.Tenant;
import multi_tenant_task_management.multi_tenant_task_management.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
@Tag(name = "Tenant", description = "Tenant registration (public)")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * POST /tenant/register
     * Public — creates a new tenant and its first ADMIN user.
     * tenant_id is assigned by the system, never from the request body.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new tenant", description = "Creates a new tenant and its first ADMIN user. No authentication required.")
    public ResponseEntity<Tenant> registerTenant(@RequestBody RegisterTenantRequest request) {
        Tenant tenant = tenantService.registerTenant(request);
        return new ResponseEntity<>(tenant, HttpStatus.CREATED);
    }
}
