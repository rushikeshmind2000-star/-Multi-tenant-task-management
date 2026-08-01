package multi_tenant_task_management.multi_tenant_task_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // Constructors
    public User() {}

    public User(Long id, String email, String password, Role role, Long tenantId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
