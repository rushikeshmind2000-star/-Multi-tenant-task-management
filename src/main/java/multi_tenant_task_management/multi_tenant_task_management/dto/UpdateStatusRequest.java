package multi_tenant_task_management.multi_tenant_task_management.dto;

import multi_tenant_task_management.multi_tenant_task_management.entity.Status;

public class UpdateStatusRequest {

    private Status status;

    // Constructors
    public UpdateStatusRequest() {}

    public UpdateStatusRequest(Status status) {
        this.status = status;
    }

    // Getter
    public Status getStatus() {
        return status;
    }

    // Setter
    public void setStatus(Status status) {
        this.status = status;
    }
}
