-- Help Desk Ticket System
-- PostgreSQL schema migration v3
-- Phase 3E bugfix: add reopened_at timestamp to tickets
--
-- Adds a nullable column to explicitly track when a ticket was reopened
-- (i.e., the requester rejected a RESOLVED ticket's resolution).
-- This allows the frontend to reliably distinguish a brand-new OPEN ticket
-- from a genuinely reopened ticket without relying on heuristics.

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS reopened_at TIMESTAMPTZ NULL;
