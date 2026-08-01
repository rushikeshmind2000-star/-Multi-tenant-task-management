package multi_tenant_task_management.multi_tenant_task_management.dto;

public class RegisterTenantRequest {

    private String tenantName;
    private String adminEmail;
    private String adminPassword;

    // Constructors
    public RegisterTenantRequest() {}

    public RegisterTenantRequest(String tenantName, String adminEmail, String adminPassword) {
        this.tenantName = tenantName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    // Getters
    public String getTenantName() {
        return tenantName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    // Setters
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
