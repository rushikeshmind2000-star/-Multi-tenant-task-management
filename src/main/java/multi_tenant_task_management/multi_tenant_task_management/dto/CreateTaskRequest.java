package multi_tenant_task_management.multi_tenant_task_management.dto;

public class CreateTaskRequest {

    private String title;
    private String description;

    // Constructors
    public CreateTaskRequest() {}

    public CreateTaskRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
