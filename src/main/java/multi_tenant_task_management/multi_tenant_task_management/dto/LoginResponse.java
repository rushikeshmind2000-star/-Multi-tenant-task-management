package multi_tenant_task_management.multi_tenant_task_management.dto;

import java.time.LocalDateTime;

public class LoginResponse {

    // Token info
    private String token;
    private String tokenType;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    // User info
    private Long userId;
    private String email;
    private String role;

    // Tenant info
    private Long tenantId;
    private String tenantName;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String token, String tokenType, LocalDateTime issuedAt, LocalDateTime expiresAt,
                         Long userId, String email, String role,
                         Long tenantId, String tenantName) {
        this.token = token;
        this.tokenType = tokenType;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
    }

    // Getters
    public String getToken()               { return token; }
    public String getTokenType()           { return tokenType; }
    public LocalDateTime getIssuedAt()     { return issuedAt; }
    public LocalDateTime getExpiresAt()    { return expiresAt; }
    public Long getUserId()                { return userId; }
    public String getEmail()               { return email; }
    public String getRole()                { return role; }
    public Long getTenantId()              { return tenantId; }
    public String getTenantName()          { return tenantName; }

    // Setters
    public void setToken(String token)                     { this.token = token; }
    public void setTokenType(String tokenType)             { this.tokenType = tokenType; }
    public void setIssuedAt(LocalDateTime issuedAt)       { this.issuedAt = issuedAt; }
    public void setExpiresAt(LocalDateTime expiresAt)     { this.expiresAt = expiresAt; }
    public void setUserId(Long userId)                     { this.userId = userId; }
    public void setEmail(String email)                     { this.email = email; }
    public void setRole(String role)                       { this.role = role; }
    public void setTenantId(Long tenantId)                 { this.tenantId = tenantId; }
    public void setTenantName(String tenantName)           { this.tenantName = tenantName; }
}
