# Multi-Tenant Task Management System

A SaaS-style task management backend where multiple organizations (tenants) share a **single H2 database**
and a **single codebase**, but their data is completely isolated through JWT-based tenant resolution.

---

## 📖 Project Flow

The Multi-Tenant Task Management System is a SaaS-based backend application built using Spring Boot, Spring Security, JWT, and a single H2 database. The application supports multiple organizations (tenants) while ensuring complete data isolation between them. Every organization has its own users and tasks, but all data is stored in the same database. Tenant isolation is achieved by associating every user and task with a `tenant_id`, and this `tenant_id` is never accepted from the client. Instead, it is extracted only from the JWT token after successful authentication. This ensures that users cannot manipulate or access another tenant's data.

The application flow starts with tenant registration. When a new organization calls `POST /tenant/register`, the system creates a new record in the tenant table and automatically creates the first ADMIN user for that tenant. The admin can then log in using `POST /auth/login`. During authentication, Spring Security validates the email and password using the `AuthenticationManager`. If authentication succeeds, the application generates a JWT containing the user's email, role, and `tenant_id`. This token is returned to the client and is used for all subsequent requests.

For every secured request, the client sends the JWT in the `Authorization` header as a Bearer token. A custom `JwtAuthenticationFilter`, implemented using `OncePerRequestFilter`, intercepts every request before it reaches the controller. The filter validates the JWT, extracts the email, role, and `tenant_id`, creates an authenticated user in the `SecurityContext`, and stores the `tenant_id` in a `TenantContext` using `ThreadLocal`. This makes the current tenant available throughout the entire request lifecycle without requiring the client to send the tenant ID again.

Role-based authorization is enforced using Spring Security. An **ADMIN** can create users only within the same tenant and can view all tasks belonging to that tenant. A **MANAGER** can create tasks and assign them only to users who belong to the same tenant. A **USER** can view only the tasks assigned to them and can update the status only of those assigned tasks. Each secured endpoint checks the authenticated user's role before executing business logic.

Whenever a task or user is created, the application automatically assigns the `tenant_id` obtained from the JWT. At no point does the application accept `tenant_id` from request parameters or request bodies. During task assignment, the service validates that both the task and the target user belong to the same tenant before updating the assignment. Similarly, when a user updates a task status, the application verifies that the task is assigned to the currently authenticated user.

The repository layer provides the final level of security. Every repository query filters data using `tenant_id`, such as `findByIdAndTenantId()` and `findAllByTenantId()`. This guarantees that no query can return data belonging to another tenant. Before updating any entity, ownership validation is performed to ensure the entity belongs to the authenticated tenant. This layered approach — JWT authentication, tenant context, role-based authorization, service-level validation, and repository-level filtering — ensures complete tenant isolation throughout the application.

Overall, the project follows a secure request lifecycle:

> **Tenant Registration → Login → JWT Generation → JWT Validation in Filter → Tenant Context Creation → Role Authorization → Service Validation → Repository Filtering → Response**

By enforcing tenant isolation at every layer, the system prevents cross-tenant data access while maintaining a single codebase and a single database, fulfilling all the assignment requirements.

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Steps

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd multi-tenant-task-management

# 2. Build the project
mvn clean install

# 3. Run the application
mvn spring-boot:run
```

The server starts at **http://localhost:8080**

### Access Points
| URL | Purpose |
|-----|---------|
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI — test all APIs |
| `http://localhost:8080/h2-console` | H2 Console (JDBC URL: `jdbc:h2:mem:multitenant`) |

---

## 🏗️ How Tenant Isolation Is Achieved

### Core Principle
> **Single database, single codebase, multiple tenants. `tenant_id` column exists in every table.**

Tenant isolation is enforced at **every layer**:

### 1. Database Layer
- Every table (`tenant`, `users`, `task`) has a `tenant_id` column.
- Foreign keys enforce referential integrity within the schema.
- No cross-tenant FK relationships exist.

### 2. JWT Layer
- When a user logs in, the JWT is generated with their `tenant_id` embedded as a claim.
- `tenant_id` is **never** accepted from request body or query params — **only** from the JWT.

### 3. Filter Layer (`JwtAuthenticationFilter`)
- On every request, the filter extracts `tenant_id` from the JWT.
- Stores it in `TenantContext` (a `ThreadLocal<Long>`) for the duration of the request.
- Clears `TenantContext` in a `finally` block after the response is sent.

### 4. Service / Repository Layer
- **All** repository queries include `tenant_id` as a filter parameter.
- Example: `findByIdAndTenantId(id, tenantId)` — a task from Tenant B can never be accessed by Tenant A.
- Cross-tenant assignment validation: when assigning a task to a user, `findByIdAndTenantId(userId, tenantId)` ensures the user belongs to the same tenant.

### 5. Role Layer
- `@PreAuthorize` on each endpoint enforces role-based access.
- `PUT /tasks/{id}/status` additionally validates that the authenticated user is the task assignee.

---

## 🔐 JWT Structure and Claims

### JWT Payload (Example)

```json
{
  "sub": "admin@acme.com",
  "userId": 1,
  "tenantId": 1,
  "role": "ADMIN",
  "iat": 1722523200,
  "exp": 1722609600
}
```

### Claim Descriptions

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | User's email address |
| `userId` | Long | User's database ID |
| `tenantId` | Long | Tenant the user belongs to — **the isolation key** |
| `role` | String | `ADMIN`, `MANAGER`, or `USER` |
| `iat` | Long | Issued-at timestamp (Unix) |
| `exp` | Long | Expiry timestamp (Unix, default: 24h) |

### JWT Configuration (`application.properties`)
```properties
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000  # 24 hours in milliseconds
```

---

## 🔄 Security Filter Flow

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│           JwtAuthenticationFilter (OncePerRequestFilter) │
│                                                          │
│  1. Extract "Authorization: Bearer <token>" header       │
│  2. jwtUtil.extractEmail(token)                         │
│  3. Load user from DB via CustomUserDetailsService       │
│  4. jwtUtil.validateToken(token, userDetails)           │
│  5. TenantContext.setTenantId(jwt.extractTenantId())    │  ← ThreadLocal
│  6. Set UsernamePasswordAuthenticationToken in           │
│     SecurityContextHolder                                │
└─────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Security Authorization               │
│                                                          │
│  • Public: /tenant/register, /auth/login, /swagger-ui/  │
│  • Protected: all other endpoints (must be authenticated)│
│  • Role check: @PreAuthorize("hasRole('ADMIN')")         │
└─────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│                     Controller Layer                     │
│                                                          │
│  • Reads tenantId via TenantContext.getTenantId()        │
│  • NEVER reads tenantId from request body/params         │
│  • Passes tenantId + userId + role to service            │
└─────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│                      Service Layer                       │
│                                                          │
│  • All business logic uses tenantId for filtering        │
│  • Cross-tenant assignment validation                    │
│  • updateStatus: validates task.assignedTo == userId     │
└─────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│                    Repository Layer                      │
│                                                          │
│  • findByIdAndTenantId()  ← always scoped               │
│  • findAllByTenantId()    ← always scoped               │
│  • No method returns cross-tenant data                   │
└─────────────────────────────────────────────────────────┘
     │
     ▼
  Response  →  [finally] TenantContext.clear()
```

---

## 📋 API Reference

### Public Endpoints (No Auth Required)

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/tenant/register` | Register new tenant + first ADMIN user |
| `POST` | `/auth/login` | Login, receive JWT |

### Protected Endpoints (JWT Required)

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| `POST` | `/users` | `ADMIN` | Create user in caller's tenant |
| `POST` | `/tasks` | `MANAGER` | Create task (tenant from JWT) |
| `PUT` | `/tasks/{id}/assign` | `MANAGER` | Assign task to same-tenant user |
| `PUT` | `/tasks/{id}/status` | `USER` | Update status (own assigned tasks only) |
| `GET` | `/tasks` | `USER/MANAGER/ADMIN` | USER: assigned only; others: all tenant tasks |

---

## 🧪 Example API Flow

### 1. Register a Tenant
```bash
POST /tenant/register
{
  "tenantName": "Acme Corp",
  "adminEmail": "admin@acme.com",
  "adminPassword": "secret123"
}
```

### 2. Login as Admin
```bash
POST /auth/login
{
  "email": "admin@acme.com",
  "password": "secret123"
}
# Response: { "token": "eyJhbGci..." }
```

### 3. Create a Manager (use Admin JWT)
```bash
POST /users
Authorization: Bearer <admin-token>
{
  "email": "manager@acme.com",
  "password": "pass123",
  "role": "MANAGER"
}
```

### 4. Create a Task (use Manager JWT)
```bash
POST /tasks
Authorization: Bearer <manager-token>
{
  "title": "Fix critical bug",
  "description": "Memory leak in production"
}
```

### 5. Assign Task to User (use Manager JWT)
```bash
PUT /tasks/1/assign
Authorization: Bearer <manager-token>
{ "userId": 3 }
```

### 6. Update Status (use User JWT)
```bash
PUT /tasks/1/status
Authorization: Bearer <user-token>
{ "status": "IN_PROGRESS" }
```

---

## 🧪 Running Tests

```bash
mvn test
```

### Test Coverage
Tests are written with **JUnit 5** and **Mockito** for the service layer:

| Test Class | Tests | Covers |
|-----------|-------|--------|
| `TenantServiceTest` | 5 | Register tenant, duplicate checks, password encoding |
| `AuthServiceTest` | 5 | Login success, bad credentials, user not found |
| `UserServiceTest` | 5 | Create user, duplicate email, password encoding, tenant isolation |
| `TaskServiceTest` | 13 | Create, assign (cross-tenant block), updateStatus (assignee validation), getTasks role-aware |

---

## 🏛️ Architecture Overview

```
multi_tenant_task_management/
├── config/
│   ├── JwtAuthenticationFilter.java  ← OncePerRequestFilter, sets TenantContext
│   ├── JwtUtil.java                  ← Token generation/validation
│   ├── SecurityConfig.java           ← Stateless JWT security
│   ├── PasswordConfig.java           ← BCrypt bean
│   └── SwaggerConfig.java            ← OpenAPI 3 with Bearer auth
├── controller/                       ← HTTP layer, reads tenantId from TenantContext only
├── service/                          ← Business logic, tenant-scoped operations
├── repository/                       ← All queries filter by tenant_id
├── entity/                           ← Tenant, User, Task (scalar tenant_id Long)
├── dto/                              ← Request/response objects (no tenant_id fields)
├── security/
│   ├── TenantContext.java            ← ThreadLocal<Long> tenant store
│   ├── CustomUserDetails.java        ← Spring Security principal
│   └── CustomUserDetailsService.java ← Loads user by email
└── exception/                        ← GlobalExceptionHandler, ResourceNotFoundException
```

---

## 🔑 Role Permissions Summary

| Action | ADMIN | MANAGER | USER |
|--------|-------|---------|------|
| Create user | ✅ | ❌ | ❌ |
| Create task | ❌ | ✅ | ❌ |
| Assign task | ❌ | ✅ | ❌ |
| Update status | ❌ | ❌ | ✅ (own tasks only) |
| View tasks | ✅ (all) | ✅ (all) | ✅ (assigned only) |
