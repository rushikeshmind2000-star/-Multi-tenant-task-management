package multi_tenant_task_management.multi_tenant_task_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // Constructors
    public Task() {}

    public Task(Long id, String title, String description, Status status, Long assignedTo, Long tenantId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.tenantId = tenantId;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public Long getTenantId() {
        return tenantId;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
