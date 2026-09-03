-- Help Desk Ticket System
-- PostgreSQL schema v1
-- Reference schema for the initial database design.
-- Derived from docs/api-contract.md and docs/database-schema.md.
--
-- IMPORTANT:
-- This file is not the application's Flyway migration yet.
-- The real database migration will be created later under:
-- backend/src/main/resources/db/migration/V1__initial_schema.sql

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ------------------------------------------------------------
-- Utility trigger for updated_at timestamps
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(30) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    avatar_url      VARCHAR(2048),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'SUPPORT_AGENT', 'ADMIN'))
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
CREATE INDEX idx_users_role_active ON users (role, active);

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- Categories
-- ------------------------------------------------------------
CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_categories_name_lower ON categories (LOWER(name));
CREATE INDEX idx_categories_active ON categories (active);

CREATE TRIGGER trg_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- Tickets
-- ------------------------------------------------------------
CREATE TABLE tickets (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number              BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
    title                       VARCHAR(200) NOT NULL,
    description                 TEXT NOT NULL,
    category_id                 UUID NOT NULL,
    priority                    VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status                      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    requester_id                UUID NOT NULL,
    assigned_agent_id           UUID,
    resolution_confirmed_at     TIMESTAMPTZ,
    resolution_confirmed_by     UUID,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tickets_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),

    CONSTRAINT chk_tickets_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),

    CONSTRAINT chk_tickets_resolution_confirmation_pair
        CHECK (
            (resolution_confirmed_at IS NULL AND resolution_confirmed_by IS NULL)
            OR
            (resolution_confirmed_at IS NOT NULL AND resolution_confirmed_by IS NOT NULL)
        ),

    CONSTRAINT chk_tickets_resolution_confirmation_status
        CHECK (
            resolution_confirmed_at IS NULL
            OR status IN ('RESOLVED', 'CLOSED')
        ),

    CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tickets_requester
        FOREIGN KEY (requester_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tickets_assigned_agent
        FOREIGN KEY (assigned_agent_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tickets_resolution_confirmed_by
        FOREIGN KEY (resolution_confirmed_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_tickets_requester_updated
    ON tickets (requester_id, updated_at DESC);

CREATE INDEX idx_tickets_assigned_agent_updated
    ON tickets (assigned_agent_id, updated_at DESC);

CREATE INDEX idx_tickets_status_updated
    ON tickets (status, updated_at DESC);

CREATE INDEX idx_tickets_priority_updated
    ON tickets (priority, updated_at DESC);

CREATE INDEX idx_tickets_category_updated
    ON tickets (category_id, updated_at DESC);

CREATE TRIGGER trg_tickets_updated_at
BEFORE UPDATE ON tickets
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- Comments
-- ------------------------------------------------------------
CREATE TABLE comments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL,
    author_id       UUID NOT NULL,
    body            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_comments_body_not_blank
        CHECK (length(btrim(body)) > 0),

    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_comments_ticket_created
    ON comments (ticket_id, created_at ASC);

CREATE TRIGGER trg_comments_updated_at
BEFORE UPDATE ON comments
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- Ticket audit history
-- ------------------------------------------------------------
CREATE TABLE ticket_audits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL,
    actor_id        UUID,
    action          VARCHAR(40) NOT NULL,
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ticket_audits_action
        CHECK (
            action IN (
                'TICKET_CREATED',
                'TICKET_ASSIGNED',
                'TICKET_REASSIGNED',
                'TICKET_UNASSIGNED',
                'STATUS_CHANGED',
                'PRIORITY_CHANGED',
                'TICKET_REOPENED',
                'RESOLUTION_CONFIRMED',
                'TICKET_CLOSED',
                'COMMENT_ADDED',
                'CATEGORY_CHANGED'
            )
        ),

    CONSTRAINT fk_ticket_audits_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticket_audits_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_ticket_audits_ticket_created
    ON ticket_audits (ticket_id, created_at ASC);

CREATE INDEX idx_ticket_audits_actor_created
    ON ticket_audits (actor_id, created_at DESC);

CREATE INDEX idx_ticket_audits_action_created
    ON ticket_audits (action, created_at DESC);

-- ------------------------------------------------------------
-- Optional seed data for the initial MVP categories.
-- Use application migrations/seed tooling in the real project.
-- ------------------------------------------------------------
INSERT INTO categories (name, description)
VALUES
    ('GENERAL', 'General inquiries and questions'),
    ('TECHNICAL', 'Technical issues and problems'),
    ('BILLING', 'Billing and payment related issues'),
    ('ACCOUNT', 'Account access and configuration'),
    ('BUG_REPORT', 'Software bug reports'),
    ('FEATURE_REQUEST', 'New feature suggestions')
ON CONFLICT DO NOTHING;

COMMIT;
