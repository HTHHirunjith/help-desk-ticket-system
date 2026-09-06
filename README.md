# Help Desk / Support Ticket Management System

A full-stack web application for managing support requests between end-users, support agents, and administrators.

---

## Overview

This system provides a structured workflow for handling support tickets. Users submit requests, support agents investigate and resolve them, and administrators oversee all activity and manage the platform.

**User roles:**

| Role            | Description                                                                 |
|-----------------|-----------------------------------------------------------------------------|
| `USER`          | Submits and tracks their own support tickets                                |
| `SUPPORT_AGENT` | Investigates and resolves tickets assigned to them                          |
| `ADMIN`         | Manages users, categories, ticket assignments, and platform administration  |

---

## Current Status

```
Phase 2  — Authentication and Authorization  (Complete)
Phase 3A — Ticket & Category Foundation       (Complete)
Status:   Complete

Next planned phase: Phase 3B — Core Ticket Management
```

See [`docs/development-status.md`](docs/development-status.md) for the full breakdown of what is and is not yet implemented.

---

## Current Features

### Authentication

- User registration (self-service; always creates a `USER`-role account)
- Email and password login
- BCrypt password hashing
- Stateless JWT-based authentication (24-hour token lifetime by default)
- Session restoration on page load via `GET /api/v1/auth/me`
- Logout

### Authorization

- Role-based access control enforced on the backend (`USER`, `SUPPORT_AGENT`, `ADMIN`)
- Protected API endpoints — unauthenticated requests receive `401 Unauthorized`
- Insufficient-role requests receive `403 Forbidden`
- Frontend route guards based on authenticated role

### Frontend

- React/TypeScript SPA (Vite)
- Login page
- Registration page
- Role-aware routing — each role is directed to its own area of the application
- Session restoration and loading state on application start
- Protected routes (authentication and role-based)
- Development proxy to backend API (`/api` → `http://localhost:8080`)

### Backend Infrastructure

- Feature-oriented Spring Boot REST API
- PostgreSQL database
- Flyway database migrations (schema versioning)
- Optional development user seeding for local environments
- Structured JSON error responses

---

## Technology Stack

| Area               | Technology                                     |
|--------------------|------------------------------------------------|
| Frontend           | React 18, TypeScript 5.5, Vite 5.4            |
| Styling            | Tailwind CSS 3.x                               |
| HTTP client        | Axios 1.x                                      |
| Routing            | React Router DOM 6.x                           |
| Backend            | Java 17, Spring Boot 3.3.5                     |
| Security           | Spring Security, JWT (jjwt 0.12.6)             |
| Password hashing   | BCrypt                                         |
| Persistence        | Spring Data JPA / Hibernate                    |
| Database           | PostgreSQL                                     |
| Database migration | Flyway                                         |
| Validation         | Jakarta Bean Validation                        |
| Build              | Maven (backend), npm (frontend)                |
| Testing            | JUnit 5, Mockito, Spring Security Test         |

---

## Architecture

```
React Frontend (Vite / TypeScript)
           ↓
  Axios HTTP client
  (Bearer token attached to every request)
           ↓
Spring Boot REST API  (/api/v1)
           ↓
Spring Security / JWT filter chain
           ↓
Service layer (business rules)
           ↓
Spring Data JPA / Hibernate
           ↓
PostgreSQL
```

The backend follows a feature-oriented package structure. Controllers are thin; business rules belong in services. JPA entities are not returned directly from REST endpoints — dedicated DTOs are used for all request and response shapes.

For the full architectural specification, see [`docs/architecture.md`](docs/architecture.md).

---

## Project Structure

```
help-desk-ticket-system/
├── frontend/               React + TypeScript SPA
│   ├── src/
│   │   ├── api/            Axios client and API service modules
│   │   ├── components/     Shared UI components and route guards
│   │   ├── context/        Authentication context
│   │   ├── pages/          Page components (auth, user, agent, admin)
│   │   └── types/          Shared TypeScript types
│   └── vite.config.ts
│
├── backend/                Spring Boot REST API
│   └── src/
│       └── main/java/com/hansana/helpdesk/
│           ├── auth/       Authentication (controller, DTOs, security, service)
│           ├── user/       User entity, repository, service
│           ├── config/     SecurityConfig, DevDataSeeder
│           └── common/     Exception handling, shared response types
│
├── docs/
│   ├── architecture.md
│   ├── api-contract.md
│   ├── database-schema.md
│   ├── database-schema.sql
│   └── development-status.md
│
├── README.md
└── .gitignore
```

---

## Getting Started

### Prerequisites

- Java 17
- Maven 3.8+
- Node.js 18+ and npm
- PostgreSQL database

### Environment configuration

The backend requires environment variables for database connection, JWT signing, and optional development seeding. No secrets should be committed to the repository.

Copy the example file and fill in values for your local environment:

```
backend/.env.example  →  backend/.env
```

The `.env` file is git-ignored. Variables required for the backend:

| Variable             | Description                                   |
|----------------------|-----------------------------------------------|
| `DATABASE_URL`       | PostgreSQL JDBC URL                           |
| `DATABASE_USERNAME`  | Database user                                 |
| `DATABASE_PASSWORD`  | Database password                             |
| `JWT_SECRET`         | Random secret key for JWT signing (min 256-bit) |

Optional variables and development seeding configuration are documented in [`backend/.env.example`](backend/.env.example) and [`docs/development-status.md`](docs/development-status.md).

---

## Running Locally

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API will start on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at `http://localhost:5173`.

In development, the Vite dev server proxies all `/api` requests to `http://localhost:8080`, so the backend must be running.

---

## Testing

### Backend tests

```bash
cd backend
mvn test
```

### Frontend lint

```bash
cd frontend
npm run lint
```

### Frontend build

```bash
cd frontend
npm run build
```

---

## Authentication Flow

```
1. User submits credentials via Login or Register form
            ↓
2. Frontend sends request to Spring Boot backend
            ↓
3. Backend authenticates and issues a signed JWT
            ↓
4. Frontend stores JWT in localStorage
            ↓
5. Axios attaches JWT as Authorization: Bearer <token> on every request
            ↓
6. Spring Security validates the token on each protected endpoint
            ↓
7. On page reload, GET /api/v1/auth/me restores the session
```

The backend is authoritative for all authorization decisions. Frontend route guards provide user-experience convenience but are not a security boundary.

---

## Roles

### USER

Default role assigned to all self-registered accounts. Users will be able to create and track their own support tickets.

### SUPPORT_AGENT

Assigned to support staff. Support agents will be able to investigate and resolve tickets assigned to them.

### ADMIN

Administrative role with the highest access level. Administrators manage users, categories, ticket assignments, and platform-wide operations.

Role elevation above `USER` is an administrative operation and is not available through any public or self-service endpoint.

---

## API

Base path: `/api/v1`

**Currently implemented endpoints:**

| Method | Path                    | Auth     | Description                        |
|--------|-------------------------|----------|------------------------------------|
| POST   | `/api/v1/auth/register` | Public   | Create a new user account          |
| POST   | `/api/v1/auth/login`    | Public   | Authenticate and receive a JWT     |
| GET    | `/api/v1/auth/me`       | Bearer   | Get the current authenticated user |

For the complete API specification, request/response shapes, and planned endpoints, see [`docs/api-contract.md`](docs/api-contract.md).

---

## Database

PostgreSQL is used as the database. Flyway manages all schema evolution through versioned migration scripts; no manual schema changes should be applied directly.

- Current migration: `V1__initial_schema.sql` (defines `users`, `categories`, `tickets`, `comments`, `ticket_audits`)
- Schema reference: [`docs/database-schema.md`](docs/database-schema.md)
- SQL reference: [`docs/database-schema.sql`](docs/database-schema.sql)

---

## Roadmap

- [x] Project foundation and architecture
- [x] Database schema and Flyway migrations
- [x] Authentication and authorization (JWT, BCrypt, role-based access)
- [x] Frontend authentication integration
- [ ] Core ticket management (create, list, view tickets)
- [ ] Ticket lifecycle (assign, resolve, confirm, reject, close)
- [ ] Comments
- [ ] Dashboards and statistics
- [ ] Administrative user and category management

---

## Documentation

| Document                                                    | Description                                        |
|-------------------------------------------------------------|----------------------------------------------------|
| [`docs/development-status.md`](docs/development-status.md) | Current implementation state, test results, environment reference |
| [`docs/architecture.md`](docs/architecture.md)             | Full-stack architectural decisions and rules       |
| [`docs/api-contract.md`](docs/api-contract.md)             | HTTP API specification, request/response shapes    |
| [`docs/database-schema.md`](docs/database-schema.md)       | Relational schema design and rationale             |

---

## License

No license has been specified for this project.
