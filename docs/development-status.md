# Help Desk Ticket System — Development Status

**Document version:** 1.4
**Phase covered:** Phase 2 (Authentication & Authorization) — complete; Phase 3A (Ticket & Category Foundation) — complete; Phase 3B (Core Ticket Management) — complete; Phase 3C (Assignment + Workflow) — complete; Phase 3D (Comments + Audit API) — complete
**Next phase:** Phase 3E (Frontend Integration) — not started

---

## 1. Project Overview

The Help Desk Ticket Management System is a full-stack web application that allows end-users to submit and track support tickets, support agents to investigate and resolve those tickets, and administrators to oversee all activity.

The repository is organized as:

```text
help-desk-ticket-system/
├── frontend/       React + TypeScript SPA (Vite)
├── backend/        Spring Boot 3 REST API (Java 17)
└── docs/           Architecture, API contract, schema
```

---

## 2. Technology Stack

### Backend

| Concern              | Technology                                    |
|----------------------|-----------------------------------------------|
| Runtime              | Java 17                                       |
| Framework            | Spring Boot 3.3.5                             |
| Web layer            | Spring Web (REST)                             |
| Persistence          | Spring Data JPA + Hibernate                   |
| Database             | PostgreSQL                                    |
| Migrations           | Flyway                                        |
| Security             | Spring Security (stateless JWT)               |
| JWT library          | jjwt 0.12.6                                   |
| Password hashing     | BCryptPasswordEncoder                         |
| Validation           | Jakarta Bean Validation                       |
| Environment config   | dotenv-java 3.2.0                             |
| Testing              | JUnit 5, Mockito, Spring Security Test        |
| Build                | Maven                                         |

### Frontend

| Concern              | Technology                                    |
|----------------------|-----------------------------------------------|
| Language             | TypeScript 5.5                                |
| Framework            | React 18.3                                    |
| Build tool           | Vite 5.4                                      |
| HTTP client          | Axios 1.x                                     |
| Routing              | React Router DOM 6.x                          |
| Icons                | lucide-react                                  |
| Styling              | Tailwind CSS 3.x                              |
| Linting              | ESLint 9 + typescript-eslint                  |

---

## 3. Phase 2 Implementation Summary

Phase 2 implemented a complete stateless JWT authentication and authorization system across the full stack. It is now frozen and verified.

### 3.1 Completed sub-phases

| Sub-phase | Description                                          | Status      |
|-----------|------------------------------------------------------|-------------|
| 2A        | Backend project scaffolding, Maven, Spring Boot boot | ✅ Complete  |
| 2B        | Database schema design and Flyway initial migration  | ✅ Complete  |
| 2C        | User entity, repository, and UserDetailsService      | ✅ Complete  |
| 2D        | JWT infrastructure (JwtService, filter, handlers)    | ✅ Complete  |
| 2E        | Development user seeding, SecurityConfig hardening   | ✅ Complete  |
| 2F        | Frontend authentication integration                  | ✅ Complete  |
| 2G        | Final verification and hardening                     | ✅ Complete  |

### 3.2 Backend — authentication components

**Security configuration** (`config/SecurityConfig.java`)

- Stateless session management (no HTTP sessions)
- CSRF disabled (JWT-authenticated SPA)
- Public endpoints: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`
- All other endpoints require authentication
- Role-based access enforced at both the Security layer (broad) and Service layer (ownership/assignment rules)

**JWT infrastructure** (`auth/security/`)

| Class                           | Role                                                              |
|---------------------------------|-------------------------------------------------------------------|
| `JwtService`                    | Token generation, claim extraction, expiration validation         |
| `JwtAuthenticationFilter`       | Extracts Bearer token from `Authorization` header, populates `SecurityContext` |
| `JwtAuthenticationEntryPoint`   | Returns structured `401` JSON on unauthenticated access           |
| `JwtAccessDeniedHandler`        | Returns structured `403` JSON on insufficient role               |
| `UserPrincipal`                 | Wraps `User` entity as a Spring Security `UserDetails`            |

**Authentication endpoints** (`auth/controller/AuthController.java`)

| Method | Path                    | Access  | Description                                         |
|--------|-------------------------|---------|-----------------------------------------------------|
| POST   | `/api/v1/auth/register` | Public  | Self-registration; always creates `USER` role       |
| POST   | `/api/v1/auth/login`    | Public  | Returns JWT on valid credentials                    |
| GET    | `/api/v1/auth/me`       | Bearer  | Returns authenticated user profile from token       |

**Development user seeder** (`config/DevDataSeeder.java`)

- Activated only when `SEED_ENABLED=true` in the environment
- Creates three pre-configured development accounts (ADMIN, SUPPORT_AGENT, USER)
- Skips creation if the account already exists (idempotent)
- Passwords are supplied via environment variables; never logged or hardcoded
- Logs only counts of created and skipped users, not credentials

**Configuration** (`application.yml`)

All sensitive values are externalized via environment variables. No secrets are present in committed configuration files.

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}  # default: 24 h
  seed:
    enabled: ${SEED_ENABLED:false}
    # email and password vars via .env
```

### 3.3 Frontend — authentication integration

**Axios client** (`src/api/http.ts`)

- Single, canonical Axios instance used throughout the frontend
- Request interceptor: injects `Authorization: Bearer <token>` from `localStorage`
- Response interceptor:
  - On `401` from a protected endpoint: clears `helpdesk_token`, redirects to `/login`
  - On `401` from login/register/me: passes the error through to the caller
  - On network errors, `5xx`, or non-401 failures from `/auth/me`: does not clear the token; surfaces an initialization error state instead

**Authentication API service** (`src/api/auth.ts`)

| Export                | Description                                       |
|-----------------------|---------------------------------------------------|
| `loginApi`            | `POST /api/v1/auth/login`                        |
| `registerApi`         | `POST /api/v1/auth/register`                     |
| `getMeApi`            | `GET /api/v1/auth/me`                            |
| `extractErrorMessage` | Maps known API error shapes to user-friendly strings |

**Authentication context** (`src/context/AuthContext.tsx`)

- Provides `user`, `isAuthenticated`, `isLoading` state to the component tree
- On mount: restores session by calling `GET /api/v1/auth/me` if `helpdesk_token` is present
  - `401` from `/auth/me` → token is cleared, state is set to logged-out
  - Network/5xx → initialization error; token is not discarded
- `login()` and `register()` call the API, store the returned token, and update state
- `logout()` clears `helpdesk_token` and resets state

**Route protection** (`src/components/routing/ProtectedRoute.tsx`)

- `ProtectedRoute` — requires authentication and an allowed role; redirects to `/login` otherwise
- `PublicOnlyRoute` — redirects already-authenticated users to their role's dashboard

**localStorage key**

The sole authentication storage key is `helpdesk_token`. Legacy mock-authentication keys (`helpdesk_auth`, `token`) from the original frontend scaffold have been removed.

**Vite proxy** (`vite.config.ts`)

`/api` requests from the dev server are proxied to `http://localhost:8080` to avoid CORS during local development.

### 3.4 Roles and routes

| Role            | Dashboard route           |
|-----------------|---------------------------|
| `USER`          | `/dashboard`              |
| `SUPPORT_AGENT` | `/agent/dashboard`        |
| `ADMIN`         | `/admin/dashboard`        |

Self-registration always creates a `USER`-role account. Role elevation is an administrative operation not exposed through any public or user-facing API endpoint.

### 3.5 Database

**Flyway migration:** `V1__initial_schema.sql`

The initial schema defines all five domain tables:

```text
users
categories
tickets
comments
ticket_audits
```

`ddl-auto` is set to `none`; Flyway is the sole mechanism for schema evolution. No manual schema changes should be applied outside of versioned migration files.

---

## 4. Test Coverage (Phase 2)

All backend tests pass as of the Phase 2G checkpoint.

| Test class                  | Scope                                                        |
|-----------------------------|--------------------------------------------------------------|
| `HelpDeskApplicationTests`  | Spring context load                                          |
| `AuthControllerTest`        | Login, register, and /me endpoint behavior                   |
| `RoleTestControllerTest`    | Role-based endpoint access (403/200 per role)                |
| `JwtServiceTest`            | Token generation, claim extraction, expiration               |
| `AuthServiceTest`           | Registration and login service logic                         |
| `DevDataSeederTest`         | Seeder idempotency and creation behavior                     |
| `PasswordEncoderTest`       | BCrypt encoder bean wiring                                   |
| `UserRepositoryTest`        | `findByEmail` query                                          |

**Total: 63 tests, all passing.**

Frontend build and ESLint lint pass with no errors.

---

## 5. Environment Configuration

### Required environment variables (backend)

| Variable              | Description                           | Required                     |
|-----------------------|---------------------------------------|------------------------------|
| `DATABASE_URL`        | JDBC URL for PostgreSQL               | Always                       |
| `DATABASE_USERNAME`   | Database user                         | Always                       |
| `DATABASE_PASSWORD`   | Database password                     | Always                       |
| `JWT_SECRET`          | HS256 signing key (min 256-bit)       | Always                       |
| `JWT_EXPIRATION_MS`   | Token TTL in milliseconds             | Optional (default: 86400000) |
| `SERVER_PORT`         | HTTP server port                      | Optional (default: 8080)     |
| `SEED_ENABLED`        | Enable development user seeding       | Optional (default: false)    |
| `DEV_ADMIN_EMAIL`     | Seed admin account email              | If seeding                   |
| `DEV_AGENT_EMAIL`     | Seed agent account email              | If seeding                   |
| `DEV_USER_EMAIL`      | Seed user account email               | If seeding                   |
| `DEV_ADMIN_PASSWORD`  | Seed admin account password           | If seeding                   |
| `DEV_AGENT_PASSWORD`  | Seed agent account password           | If seeding                   |
| `DEV_USER_PASSWORD`   | Seed user account password            | If seeding                   |

Variables are loaded from `backend/.env` (git-ignored). A `backend/.env.example` template is provided in the repository.

---

## 6. Phase 3 Implementation Summary & What Is Not Yet Implemented

### 6.1 Phase 3A: Ticket & Category Foundation (Complete)

Phase 3A established the backend domain foundation for tickets, categories, comments, and audits:
- **JPA Entities**: `Category`, `Ticket`, `Comment`, `TicketAudit` with appropriate constraints, column types, and lazy relationships.
- **Controlled Values / Enums**: `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), `TicketStatus` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), and `AuditAction` (11 approved actions).
- **Repositories**: `CategoryRepository`, `TicketRepository`, `CommentRepository`, and `TicketAuditRepository` using Spring Data JPA.
- **Service**: `CategoryService` foundation for category lookups.
- **Database & Migration**: `V1__initial_schema.sql` establishes tables, check constraints, foreign keys, triggers, indexes, and initial 6 seed categories. `V2__ticket_domain_baseline.sql` documents the Phase 3A schema checkpoint.
- **Tests**: Comprehensive entity, enum, repository query declaration, and service unit tests (90 tests total, all passing).

### 6.2 Phase 3B: Core Ticket Management (Complete)

Phase 3B implemented the core ticket lifecycle endpoints, category read support, DTOs, and role-based security:
- **Category Read API**: `GET /api/v1/categories` (supporting optional `?active=true` and `?active=false` filtering via service delegation, keeping the controller thin).
- **Ticket Management API**:
  - `POST /api/v1/tickets`: USER-only creation. Initial priority required. Requester automatically derived from authenticated principal.
  - `GET /api/v1/tickets`: Role-scoped filtered listing (USER sees owned tickets, SUPPORT_AGENT sees assigned tickets, ADMIN sees all; with optional status, priority, categoryId filters; sorted by `updatedAt DESC` via `@PageableDefault`).
  - `GET /api/v1/tickets/{ticketId}`: Role-scoped detail lookup (non-owner/unassigned tickets return 404 to hide existence).
  - `PATCH /api/v1/tickets/{ticketId}`: USER-only open ticket update (enforces strict security ordering: ownership check first returning 404, status check returning 409 if not OPEN, non-empty field validation, active category validation, partial update).
  - `PATCH /api/v1/tickets/{ticketId}/priority`: SUPPORT_AGENT (assigned only) and ADMIN priority change endpoint (USER receives 403; unassigned/differently assigned agent receives 404).
- **Error Handling & DTOs**: `TicketNotEditableException` (409 Conflict), malformed JSON/invalid enum handling (400 Bad Request), `PagedResponse<T>`, `CategoryResponse`, `TicketSummaryResponse`, `TicketDetailResponse`, `CreateTicketRequest`, `UpdateTicketRequest`, `ChangePriorityRequest`.
- **Tests**: Comprehensive controller security tests (`TicketControllerSecurityTest`, `CategoryControllerSecurityTest`) and service business-rule tests (`TicketServiceTest`, `CategoryServiceTest`). Total 137 tests, all passing.

### 6.3 Phase 3C: Assignment + Workflow (Complete)

Phase 3C implemented ticket assignment/reassignment/unassignment, lifecycle workflow actions, and transactional audit event persistence:
- **Assignment Endpoints (ADMIN-only)**:
  - `PUT /api/v1/tickets/{ticketId}/assignment`: Assigns an unassigned ticket (`TICKET_ASSIGNED`) or reassigns to a different agent (`TICKET_REASSIGNED`). Validates target agent exists, is active, and has `SUPPORT_AGENT` role. Assigning the exact same agent returns `409 Conflict` and generates no audit event.
  - `DELETE /api/v1/tickets/{ticketId}/assignment`: Unassigns the current agent (`TICKET_UNASSIGNED`). If already unassigned, returns `409 Conflict` and generates no audit event.
  - **No Self-Claim**: Explicitly enforces ADMIN-only assignment authority. No self-claim endpoint or self-assignment exists.
- **Workflow Lifecycle Endpoints**:
  - `POST /api/v1/tickets/{ticketId}/start`: Assigned `SUPPORT_AGENT` only. Transitions `OPEN` → `IN_PROGRESS`. Persists `STATUS_CHANGED` audit record.
  - `POST /api/v1/tickets/{ticketId}/resolve`: Assigned `SUPPORT_AGENT` only. Transitions `IN_PROGRESS` → `RESOLVED`. Persists `STATUS_CHANGED` audit record.
  - `POST /api/v1/tickets/{ticketId}/confirm-resolution`: Requester (`USER`) only. Validates ticket is `RESOLVED`, records `resolutionConfirmedAt` timestamp and `resolutionConfirmedBy` (authenticated requester), preserves assigned agent. Persists `RESOLUTION_CONFIRMED` audit record.
  - `POST /api/v1/tickets/{ticketId}/reject-resolution`: Requester (`USER`) only. Transitions `RESOLVED` → `OPEN`, clears resolution confirmation fields, updates `updatedAt`, preserves assigned agent. Persists `TICKET_REOPENED` audit record.
  - `POST /api/v1/tickets/{ticketId}/close`: `ADMIN` only. Requires ticket in `RESOLVED` state with both `resolutionConfirmedAt` and `resolutionConfirmedBy` populated. Transitions `RESOLVED` → `CLOSED` (terminal state). Persists `TICKET_CLOSED` audit record.
- **Audit Persistence & Error Handling**:
  - Reuses Phase 3A `TicketAudit` entity and repository within the same `@Transactional` boundary as state mutations.
  - Derives `actorId` strictly from authenticated security context (`UserPrincipal`).
  - Structured error handling: `InvalidTicketStateException` (409 Conflict) for invalid lifecycle transitions and redundant assignment/unassignment operations.
- **Tests**: Comprehensive unit, controller security, and service business-rule tests (161 tests total, all passing).

### 6.4 Phase 3D: Comments + Audit API (Complete)

Phase 3D implemented the comment and audit history REST endpoints, server-derived author/actor tracking, and transactional audit event persistence:
- **Comment Endpoints**:
  - `GET /api/v1/tickets/{ticketId}/comments`: Lists comments on a ticket. Access is strictly gated by ticket visibility rules (USER sees comments on owned tickets; SUPPORT_AGENT sees comments on assigned tickets; ADMIN sees comments on any ticket; inaccessible or nonexistent tickets return 404). Ordered deterministically by `createdAt ASC`.
  - `POST /api/v1/tickets/{ticketId}/comments`: Adds a comment to a ticket. Subject to the exact same ticket visibility rules. Author is strictly derived from the authenticated security principal (`UserPrincipal`), client cannot choose author. Persists `COMMENT_ADDED` audit event and comment record within the same transaction. Returns `201 Created` with `CommentResponse`.
- **Audit History Endpoint (ADMIN-only)**:
  - `GET /api/v1/tickets/{ticketId}/audit`: Retrieves full audit timeline for a ticket. Restricted strictly to `ADMIN` (enforced at both SecurityConfig and controller layers; non-ADMIN authenticated requests receive `403 Forbidden`; nonexistent tickets return `404 Not Found`). Ordered deterministically by `createdAt DESC`.
- **DTOs and Information Sanitization**:
  - `CreateCommentRequest`: Validates `@NotBlank` and `@Size(max = 5000)`.
  - `CommentResponse`: Exposes `id`, `body`, `author` (`AuthorRef(id, name, role)`), `createdAt`, `updatedAt`. Never exposes passwords or sensitive credentials.
  - `AuditResponse`: Exposes `id`, `action`, `actor` (`ActorRef(id, name, email, role)`), `details`, `createdAt`. Never exposes passwords or sensitive credentials.
- **Tests**: 37 comprehensive unit, controller security, and service business-rule tests (`CommentServiceTest`, `CommentControllerSecurityTest`). Total 198 tests, all passing.

### 6.5 Not Yet Implemented (Phase 3E+)

The following features are designed and specified in the API contract and architecture documents but have **NOT** been implemented:

- **Phase 3E**: Frontend ticket/category/workflow/comments/audit integration, replacing mock services with real API calls
- **Future phases**: Category management write endpoints, dashboard statistics, user management (activation/deactivation, role management by admin), agent management views

---

## 7. Reference Documents

| Document                 | Path                         | Description                                      |
|--------------------------|------------------------------|--------------------------------------------------|
| Architecture             | `docs/architecture.md`       | Full-stack architectural decisions and rules     |
| API Contract             | `docs/api-contract.md`       | HTTP API specification, request/response shapes  |
| Database Schema (design) | `docs/database-schema.md`    | Relational schema design and rationale           |
| Database Schema (SQL)    | `docs/database-schema.sql`   | Reference SQL for the initial schema             |
| Development Status       | `docs/development-status.md` | This document                                    |
