# Help Desk Ticket System — Development Status

**Document version:** 1.1
**Phase covered:** Phase 2 (Authentication & Authorization) — complete; Phase 3A (Ticket & Category Foundation) — complete
**Next phase:** Phase 3B (Core Ticket Management) — not started

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

## 6. Phase 3A Implementation Summary & What Is Not Yet Implemented

### 6.1 Phase 3A: Ticket & Category Foundation (Complete)

Phase 3A established the backend domain foundation for tickets, categories, comments, and audits:
- **JPA Entities**: `Category`, `Ticket`, `Comment`, `TicketAudit` with appropriate constraints, column types, and lazy relationships.
- **Controlled Values / Enums**: `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), `TicketStatus` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), and `AuditAction` (11 approved actions).
- **Repositories**: `CategoryRepository`, `TicketRepository`, `CommentRepository`, and `TicketAuditRepository` using Spring Data JPA.
- **Service**: `CategoryService` foundation for category lookups.
- **Database & Migration**: `V1__initial_schema.sql` establishes tables, check constraints, foreign keys, triggers, indexes, and initial 6 seed categories. `V2__ticket_domain_baseline.sql` documents the Phase 3A schema checkpoint.
- **Tests**: Comprehensive entity, enum, repository query declaration, and service unit tests (90 tests total, all passing).

### 6.2 Not Yet Implemented (Phase 3B+)

The following features are designed and specified in the API contract and architecture documents but have **NOT** been implemented:

- **Phase 3B**: Ticket REST controllers, create ticket endpoint, list tickets endpoint, ticket detail endpoint, edit ticket endpoint, change priority endpoint
- **Phase 3C**: Ticket assignment and reassignment endpoints, ticket lifecycle workflow endpoints (`/start`, `/resolve`, `/confirm-resolution`, `/reject-resolution`, `/close`)
- **Phase 3D**: Comment REST endpoints (`GET /api/v1/tickets/{id}/comments`, `POST /api/v1/tickets/{id}/comments`), audit REST endpoints / event logging workflow
- **Phase 3E**: Frontend ticket/category integration, replacing mock services with real API calls
- **Future phases**: Category management REST endpoints, dashboard statistics, user management (activation/deactivation, role management by admin), agent management views

---

## 7. Reference Documents

| Document                 | Path                         | Description                                      |
|--------------------------|------------------------------|--------------------------------------------------|
| Architecture             | `docs/architecture.md`       | Full-stack architectural decisions and rules     |
| API Contract             | `docs/api-contract.md`       | HTTP API specification, request/response shapes  |
| Database Schema (design) | `docs/database-schema.md`    | Relational schema design and rationale           |
| Database Schema (SQL)    | `docs/database-schema.sql`   | Reference SQL for the initial schema             |
| Development Status       | `docs/development-status.md` | This document                                    |
