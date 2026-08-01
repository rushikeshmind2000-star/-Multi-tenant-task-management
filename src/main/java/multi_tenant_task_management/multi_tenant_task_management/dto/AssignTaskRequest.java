package multi_tenant_task_management.multi_tenant_task_management.dto;

public class AssignTaskRequest {

    private Long userId;

    // Constructors
    public AssignTaskRequest() {}

    public AssignTaskRequest(Long userId) {
        this.userId = userId;
    }

    // Getter
    public Long getUserId() {
        return userId;
    }

    // Setter
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
