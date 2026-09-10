-- Help Desk Ticket System
-- PostgreSQL schema v4
-- Flyway migration: add must_change_password column to users table
--

ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
