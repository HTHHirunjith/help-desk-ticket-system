# Help Desk Ticket System — Software Architecture v1

**Status:** Design baseline
**Version:** 1.0
**Scope:** Full-stack MVP architecture

## 1. Purpose

This document defines the architectural baseline for the Help Desk Ticket System.

It is derived from the approved business rules, API contract, and PostgreSQL schema. It establishes the implementation boundaries for the frontend, backend, persistence layer, security model, and testing strategy.

Implementation agents must not redesign the architecture, change API semantics, or change database/domain decisions without an explicit design change.

## 2. Repository structure

The repository is a full-stack project with explicit frontend/backend boundaries:

```text
help-desk-ticket-system/

├── frontend/
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.ts
│   ├── src/
│   └── ...

├── backend/
│   ├── pom.xml
│   └── src/

├── docs/
│   ├── api-contract.md
│   ├── architecture.md
│   ├── database-schema.md
│   └── database-schema.sql

├── README.md
└── .gitignore
```

The frontend is already stabilized under `frontend/`. Backend implementation begins separately under `backend/`.

The `docs/` directory contains design and implementation-reference documentation.

## 3. Frontend stabilization principle

The existing Bolt-generated frontend is the UI/product baseline.

Its visual design and route structure should be preserved unless a functional requirement requires a change.

The frontend may use mock services during early development, but those mocks are not the backend domain contract.

During API integration:

1. Replace mock authentication with real authentication.
2. Replace mock service calls with REST API calls.
3. Adapt frontend types to the documented API.
4. Remove client-controlled values that are authoritative on the server.
5. Keep frontend-only presentation state where appropriate.
6. Preserve the working UI unless a functional change is intentionally required.

The frontend must not become a second implementation of backend business rules.

## 4. Backend technology baseline

The backend uses:

* Java 17
* Spring Boot 3.x
* Spring Web
* Spring Data JPA
* Spring Security
* JWT authentication
* PostgreSQL
* Flyway database migrations
* Jakarta Bean Validation
* JUnit 5
* Mockito
* Spring Security Test

Testcontainers/PostgreSQL integration testing may be introduced where real persistence behavior requires it.

## 5. Backend architectural style

The primary application flow is:

```text
HTTP request
     ↓
Controller
     ↓
DTO validation
     ↓
Service / business rules
     ↓
Repository
     ↓
PostgreSQL
```

Cross-cutting concerns include:

```text
Spring Security / JWT
Global exception handling
Validation
Transactions
Audit logging
Configuration
Logging
```

Controllers are thin.

Business rules belong in services.

Persistence belongs in repositories and JPA entities.

REST DTOs are separate from JPA entities.

## 6. Package structure

The backend is feature-oriented:

```text
com.hansana.helpdesk/

├── HelpDeskApplication.java

├── auth/
│   ├── controller/
│   ├── dto/
│   ├── security/
│   └── service/

├── user/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/

├── category/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/

├── ticket/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/

├── comment/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/

├── audit/
│   ├── entity/
│   ├── repository/
│   └── service/

├── dashboard/
│   ├── controller/
│   ├── dto/
│   └── service/

├── common/
│   ├── exception/
│   ├── response/
│   └── validation/

└── config/
```

Feature packages may depend on shared infrastructure, but unrelated feature logic should not be mixed together.

## 7. Domain entities

The MVP has exactly five persistence entities:

```text
User
Category
Ticket
Comment
TicketAudit
```

There is no separate `Agent` entity.

A support agent is a `User` whose role is `SUPPORT_AGENT`.

Database mapping:

```text
users         → User
categories    → Category
tickets       → Ticket
comments      → Comment
ticket_audits → TicketAudit
```

## 8. Domain relationships

```text
User 1 ───────< Ticket       (requester)

User 1 ───────< Ticket       (assigned agent)

User 1 ───────< Ticket       (resolution confirmer)

User 1 ───────< Comment      (author)

User 1 ───────< TicketAudit   (actor)

Category 1 ───< Ticket

Ticket 1 ─────< Comment

Ticket 1 ─────< TicketAudit
```

`assignedAgent` is nullable.

When populated, it must identify an active `SUPPORT_AGENT`.

## 9. Entity/API separation

JPA entities must not be returned directly from REST controllers.

Use DTOs for requests and responses, such as:

```text
RegisterRequest
LoginRequest

CreateTicketRequest
UpdateTicketRequest
ChangePriorityRequest
AssignmentRequest

AddCommentRequest

TicketSummaryResponse
TicketDetailResponse

CategoryResponse
UserResponse

...
```

This prevents database implementation details from becoming API contracts.

It also allows the frontend response shape to be convenient without corrupting the relational model.

## 10. Authentication and security

Authentication uses stateless JWTs.

Login flow:

```text
POST /api/v1/auth/login
        ↓
AuthenticationManager
        ↓
UserDetailsService
        ↓
PasswordEncoder verification
        ↓
JWT generation
```

Protected request flow:

```text
Authorization: Bearer <JWT>
        ↓
JWT authentication filter
        ↓
SecurityContext
        ↓
Controller
        ↓
Service
```

A current-user abstraction should expose the authenticated user's ID and role to application services without making services parse JWT claims directly.

Role authorization and resource authorization are separate:

```text
Spring Security → role-level permission

Service         → ownership/assignment/business rules
```

Example:

A `USER` may access the ticket-detail endpoint generally, but the service must verify that the requested ticket belongs to the authenticated user.

## 11. Business rule location

### Database

The database is responsible for core data integrity:

* primary keys;
* foreign keys;
* unique constraints;
* not-null constraints;
* controlled-value checks;
* indexes.

### Service layer

Services are responsible for domain behavior:

* ticket lifecycle;
* role-dependent operations;
* ownership checks;
* assignment rules;
* resolution confirmation;
* reopening behavior;
* category activation rules;
* account activation/deactivation;
* audit creation.

### Security layer

Spring Security is responsible for:

* authentication;
* JWT processing;
* broad role-level authorization.

## 12. Ticket lifecycle architecture

The canonical lifecycle is:

```text
OPEN
  ↓
IN_PROGRESS
  ↓
RESOLVED
  ├── requester confirms → remains RESOLVED → ADMIN may close
  │
  └── requester rejects  → OPEN, same agent remains assigned

RESOLVED + confirmed
  ↓
ADMIN CLOSES
  ↓
CLOSED
```

`CLOSED` is final in MVP.

Lifecycle operations use dedicated service/controller actions rather than a generic status mutation because each transition has different permissions and preconditions.

## 13. Service transaction rules

Multi-step business operations are transactional.

Examples:

```text
assign ticket
    → update ticket
    → create audit

resolve ticket
    → update ticket
    → create audit

reject resolution
    → set status OPEN
    → preserve assigned agent
    → clear confirmation
    → create audit
```

The state change and corresponding audit operation must succeed or fail together.

## 14. Audit architecture

Audit logging is part of MVP.

The service performing a business operation creates the corresponding `TicketAudit` record.

Controllers must not accept trusted audit claims from clients.

Audit details are stored as structured JSON/JSONB data so the same table can represent multiple event types.

Important actions include:

```text
TICKET_CREATED
TICKET_ASSIGNED
TICKET_REASSIGNED
TICKET_UNASSIGNED
STATUS_CHANGED
PRIORITY_CHANGED
TICKET_REOPENED
RESOLUTION_CONFIRMED
TICKET_CLOSED
COMMENT_ADDED
CATEGORY_CHANGED
```

For status changes, `details` contains the previous and new status.

For assignment changes, `details` may contain the previous and new agent IDs.

## 15. Exception handling

A single global exception-handling mechanism produces the standardized API error structure defined in `docs/api-contract.md`.

Controllers must not invent their own error formats.

Expected categories include:

```text
400 → invalid request / invalid business operation
401 → unauthenticated
403 → authenticated but forbidden
404 → resource not found
409 → data or business conflict
```

## 16. Validation architecture

Validation occurs at two levels.

### Request validation

Jakarta Bean Validation handles structural validity:

```text
@NotBlank
@Email
@NotNull
@Size
```

### Business validation

Services enforce context-dependent rules:

```text
USER can edit only own OPEN ticket

AGENT can act only on assigned tickets

ADMIN alone can assign/reassign/unassign

only requester can confirm/reject resolution

only ADMIN can close tickets

ADMIN can close only confirmed RESOLVED ticket

closed tickets cannot be reopened

inactive categories cannot be selected for new tickets

deactivated users cannot authenticate
```

## 17. Database migrations

Flyway is the authoritative mechanism for database evolution.

The initial migration will be created later as:

```text
backend/src/main/resources/db/migration/V1__initial_schema.sql
```

Future schema changes use new migrations:

```text
V2__...
V3__...
```

Already-applied migrations must not be casually rewritten.

The current `docs/database-schema.sql` is the design/reference SQL for the initial schema. It is not being executed manually at this frontend checkpoint.

## 18. Testing architecture

The test strategy is layered.

### Unit tests

Focus on services and business rules.

Examples:

* user cannot access another user's ticket;
* agent cannot act on another agent's ticket;
* agent cannot assign a ticket;
* admin can reassign a ticket;
* reopened ticket remains assigned to the same agent;
* unconfirmed ticket cannot be closed;
* confirmed resolution can be closed by admin;
* closed ticket cannot be reopened.

### API/controller tests

Use Spring MVC test support to verify:

* endpoint behavior;
* validation;
* authentication;
* authorization;
* response status codes;
* error responses.

### Security tests

Verify:

* unauthenticated access is rejected;
* role restrictions are enforced;
* ownership restrictions are enforced;
* deactivated users cannot authenticate.

### Integration tests

Introduce PostgreSQL/Testcontainers where real database behavior cannot be confidently covered by unit and controller tests.

A feature is not considered complete merely because the source code compiles. Relevant tests and a successful build are part of the feature checkpoint.

## 19. Agent workflow rules

The architecture is designed for controlled use with Cursor, Copilot and other coding agents.

Every implementation task follows:

```text
Read docs
    ↓
Inspect current implementation
    ↓
Implement only requested scope
    ↓
Run relevant tests/build/lint
    ↓
Report changes and evidence
    ↓
Human reviews
    ↓
Human commits/pushes
    ↓
Update documentation/checkpoint if required
```

Agents must not:

* redesign the architecture without approval;
* modify unrelated features;
* change API contracts silently;
* modify database structure without an approved migration/design change;
* add frameworks/dependencies without justification;
* commit or push Git changes;
* regenerate or replace large portions of the project unnecessarily.

## 20. Frontend/backend integration boundary

The frontend communicates with the backend through the documented REST API.

The frontend may contain API client/service modules, but those modules must not reproduce backend business rules as authoritative logic.

The backend is authoritative for:

* authentication;
* authorization;
* ownership;
* assignment;
* lifecycle transitions;
* priority changes;
* category validity;
* audit history;
* dashboard statistics.

Frontend route guards and UI restrictions improve user experience but are not security boundaries.

## 21. Architecture decision summary

The following decisions are fixed for v1:

```text
Repository: frontend / backend / docs

Package style: feature-oriented

API: REST, /api/v1

Authentication: JWT, stateless

Persistence: PostgreSQL + Spring Data JPA

Migrations: Flyway

DTOs separate from entities

Roles:
    USER
    SUPPORT_AGENT
    ADMIN

Core entities:
    User
    Category
    Ticket
    Comment
    TicketAudit

No Agent table/entity

Ticket assignment: ADMIN only

Ticket lifecycle:
    OPEN → IN_PROGRESS → RESOLVED → CLOSED

Requester can reject:
    RESOLVED → OPEN

Rejected resolution retains the same assigned agent

CLOSED is final in MVP

Audit logging: MVP

No agent availability subsystem in MVP
```

## 22. Related authoritative documents

* `docs/api-contract.md` — HTTP API and API semantics.
* `docs/database-schema.md` — relational schema and design rationale.
* `docs/database-schema.sql` — SQL reference schema.
