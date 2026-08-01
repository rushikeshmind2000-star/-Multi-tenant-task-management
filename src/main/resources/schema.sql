-- =====================================================================
-- Multi-Tenant Task Management System - MySQL Schema
-- Single Database, tenant_id column in every table for data isolation
-- Database: techplus
-- =====================================================================

CREATE TABLE IF NOT EXISTS tenant (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    email     VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    role      VARCHAR(20)  NOT NULL,
    tenant_id BIGINT       NOT NULL,
    CONSTRAINT fk_user_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE IF NOT EXISTS task (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status      VARCHAR(30)  NOT NULL,
    assigned_to BIGINT,
    tenant_id   BIGINT       NOT NULL,
    CONSTRAINT fk_task_user
        FOREIGN KEY (assigned_to) REFERENCES users(id),
    CONSTRAINT fk_task_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);
