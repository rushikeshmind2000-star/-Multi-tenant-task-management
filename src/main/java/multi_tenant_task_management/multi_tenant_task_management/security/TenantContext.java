package multi_tenant_task_management.multi_tenant_task_management.security;

public class TenantContext {

    private static final ThreadLocal<Long> tenantId = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long id) {
        tenantId.set(id);
    }

    public static Long getTenantId() {
        return tenantId.get();
    }

    public static void clear() {
        tenantId.remove();
    }
}
