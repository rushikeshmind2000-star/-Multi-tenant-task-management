package multi_tenant_task_management.multi_tenant_task_management.dto;

import multi_tenant_task_management.multi_tenant_task_management.entity.Role;

public class CreateUserRequest {

    private String email;
    private String password;
    private Role role;

    // Constructors
    public CreateUserRequest() {}

    public CreateUserRequest(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
