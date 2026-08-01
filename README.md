# Multi-Tenant Task Management System

A SaaS-style task management backend where multiple organizations (tenants) share a **single H2 database**
and a **single codebase**, but their data is completely isolated through JWT-based tenant resolution.

---

## 📖 Overview

Build a SaaS-style task management backend where multiple organizations (tenants) share the same backend and database, but their data must be completely isolated. No tenant should ever see another tenant’s data.

This assignment tests:
- Spring Security fundamentals
- JWT handling and customization
- Filter usage
- Tenant isolation logic
- Repository-level data protection

### CORE IDEA
Single database, single codebase, multiple tenants.
Tenant isolation must be enforced at every layer of the application.

### ENTITIES
**Tenant**
- id
- name
- created_at

**User**
- id
- email
- password
- role (ADMIN, MANAGER, USER)
- tenant_id

**Task**
- id
- title
- description
- status
- assigned_to
- tenant_id

### ROLES
**ADMIN**
- Can manage users within the tenant
- Can view all tasks in the tenant

**MANAGER**
- Can create tasks
- Can assign tasks to users of the same tenant

**USER**
- Can view only assigned tasks
- Can update status of assigned tasks

### SECURITY RULES (MANDATORY)
- JWT must contain `tenant_id` and `role`
- Tenant must be resolved only from JWT
- `tenant_id` must never be accepted via request parameters or request body
- Cross-tenant access must be impossible
- All secured APIs must be authenticated

### API DETAILS
**`POST /tenant/register`**
- Creates a new tenant
- Creates the first ADMIN user for the tenant

**`POST /auth/login`**
- Authenticates user
- Returns JWT containing `tenant_id` and `role`

**`POST /users`**
- Role: ADMIN
- Creates a new user under the same tenant

**`POST /tasks`**
- Role: MANAGER
- Creates a task
- Task must automatically use `tenant_id` from JWT

**`PUT /tasks/{id}/assign`**
- Role: MANAGER
- Assigns task to a user of the same tenant

**`PUT /tasks/{id}/status`**
- Role: USER
- User can update only their assigned tasks

**`GET /tasks`**
- Role: USER, MANAGER, ADMIN
- Returns tasks belonging only to the user’s tenant

### SECURITY IMPLEMENTATION EXPECTATIONS
- Use `OncePerRequestFilter` to extract `tenant_id` from JWT
- Store tenant context in `SecurityContext` or `ThreadLocal`
- Tenant context must be available throughout request lifecycle

### REPOSITORY RULES
- All repository queries must filter by `tenant_id`
- No repository method should return data across tenants
- Entity ownership must be validated before updates

### DATABASE RULES
- Use single MySQL database
- `tenant_id` column must exist in every table
- No multiple schemas or databases

### SUBMISSION RULES
- Upload project to GitHub
- README must explain:
  - How tenant isolation is achieved
  - JWT structure and claims
  - Security filter flow
  - Include example JWT payload
  - Provide steps to run the application
- Unit test case for all ap is required (At least 80% coverage)
- Share a video of yourself at least of 5 mins explaining about project in detail
- Time Duration to submit is 24 Hours after receiving Email.

*NOTE: This is a backend-focused assignment. UI is not required.*

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
