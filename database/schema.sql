-- ============================================================
-- Scientific Journal Publication Trend Tracking System
-- Team 7 - SU26SWP06 - SWP391
-- Database Schema (SQL Server)
-- Converted for SQL Server Compatibility
-- Updated: 2026-06-09
-- ============================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

USE PublicationTracker;
GO

-- Dynamically drop all foreign key constraints in the database
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql += N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + N'.' + QUOTENAME(OBJECT_NAME(parent_object_id)) + 
               N' DROP CONSTRAINT ' + QUOTENAME(name) + N';' + CHAR(13)
FROM sys.foreign_keys;
EXEC sp_executesql @sql;
GO

-- Drop existing tables to ensure a clean install
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS premium_orders;
DROP TABLE IF EXISTS sync_jobs;
DROP TABLE IF EXISTS report_tickets;
DROP TABLE IF EXISTS search_history;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS follow_journal;
DROP TABLE IF EXISTS follow_topic;
DROP TABLE IF EXISTS follow_author;
DROP TABLE IF EXISTS folder_papers;
DROP TABLE IF EXISTS paper_topics;
DROP TABLE IF EXISTS topics;
DROP TABLE IF EXISTS bookmark_folders;
DROP TABLE IF EXISTS paper_keywords;
DROP TABLE IF EXISTS paper_authors;
DROP TABLE IF EXISTS papers;
DROP TABLE IF EXISTS keywords;
DROP TABLE IF EXISTS authors;
DROP TABLE IF EXISTS journals;
DROP TABLE IF EXISTS research_fields;
DROP TABLE IF EXISTS premium_subscriptions;
DROP TABLE IF EXISTS subscription_plans;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS api_sources;
DROP TABLE IF EXISTS user_subscriptions;
GO

-- ============================================================
-- 1. ROLES
-- ============================================================
CREATE TABLE roles (
    role_id   INT         NOT NULL IDENTITY(1,1),
    role_name VARCHAR(50) NOT NULL UNIQUE,
    PRIMARY KEY (role_id)
);
GO

-- ============================================================
-- 2. USERS
-- ============================================================
CREATE TABLE users (
    user_id            BIGINT       NOT NULL IDENTITY(1,1),
    role_id            INT          NOT NULL,
    full_name          VARCHAR(150) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    affiliation        VARCHAR(255),
    status             VARCHAR(20)  NOT NULL DEFAULT 'pending' CONSTRAINT chk_users_status CHECK (status IN ('active','inactive','pending', 'ACTIVE', 'INACTIVE', 'BANNED', 'PENDING')),
    proof_document_url VARCHAR(500),
    primary_field_id   INT,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
);
GO

-- ============================================================
-- 3. SUBSCRIPTION PLANS
-- ============================================================
CREATE TABLE subscription_plans (
    plan_id        INT            NOT NULL IDENTITY(1,1),
    plan_name      VARCHAR(100)   NOT NULL UNIQUE,
    price          DECIMAL(10, 2) NOT NULL,
    discount       DECIMAL(5, 2)  NOT NULL DEFAULT 0.00 CONSTRAINT chk_sp_discount CHECK (discount >= 0.00 AND discount <= 100.00), -- discount percentage
    duration_days  INT            NOT NULL, -- 30 for monthly, 365 for yearly, 99999 for free
    description    VARCHAR(MAX),
    status         VARCHAR(20)    NOT NULL DEFAULT 'active' CONSTRAINT chk_sp_status CHECK (status IN ('active','inactive', 'ACTIVE', 'INACTIVE')),
    PRIMARY KEY (plan_id)
);
GO

-- ============================================================
-- 3.5 PREMIUM ORDERS
-- ============================================================
CREATE TABLE premium_orders (
    order_id        BIGINT         NOT NULL IDENTITY(1,1),
    user_id         BIGINT         NOT NULL,
    plan_id         INT            NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    discount        DECIMAL(5, 2)  NOT NULL DEFAULT 0.00,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING' CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'pending', 'paid', 'cancelled')),
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(plan_id)
);
GO

-- ============================================================
-- 4. PREMIUM SUBSCRIPTIONS
-- ============================================================
CREATE TABLE premium_subscriptions (
    subscription_id  BIGINT         NOT NULL IDENTITY(1,1),
    user_id          BIGINT         NOT NULL,
    plan_id          INT            NOT NULL,
    subscribed_price DECIMAL(10, 2) NOT NULL,
    discount         DECIMAL(5, 2)  NOT NULL DEFAULT 0.00 CONSTRAINT chk_ps_discount CHECK (discount >= 0.00 AND discount <= 100.00),
    start_date       DATE           NOT NULL,
    end_date         DATE           NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'active' CONSTRAINT chk_ps_status CHECK (status IN ('active','expired','cancelled', 'ACTIVE','EXPIRED','CANCELLED')),
    PRIMARY KEY (subscription_id),
    CONSTRAINT fk_ps_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(plan_id)
);
GO

-- ============================================================
-- 5. PAYMENT TRANSACTIONS
-- ============================================================
CREATE TABLE payment_transactions (
    transaction_id   BIGINT         NOT NULL IDENTITY(1,1),
    user_id          BIGINT         NOT NULL,
    plan_id          INT            NOT NULL,
    order_id         BIGINT         NULL,
    amount           DECIMAL(10, 2) NOT NULL,
    transaction_type VARCHAR(20)    NOT NULL CONSTRAINT chk_tx_type CHECK (transaction_type IN ('BUY', 'RENEW', 'UPGRADE', 'buy', 'renew', 'upgrade')),
    payment_method   VARCHAR(50),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING' CONSTRAINT chk_tx_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'pending', 'success', 'failed')),
    transaction_date DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(plan_id),
    CONSTRAINT fk_tx_order FOREIGN KEY (order_id) REFERENCES premium_orders(order_id)
);
GO

-- ============================================================
-- 6. PASSWORD RESET TOKENS
-- ============================================================
CREATE TABLE password_reset_tokens (
    token_id    BIGINT       NOT NULL IDENTITY(1,1),
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  DATETIME     NOT NULL,
    used        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (token_id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 7. RESEARCH FIELDS
-- ============================================================
CREATE TABLE research_fields (
    field_id     INT          NOT NULL IDENTITY(1,1),
    field_name   VARCHAR(200) NOT NULL UNIQUE,
    description  VARCHAR(MAX),
    PRIMARY KEY (field_id)
);
GO

-- Add foreign key from users to research_fields
ALTER TABLE users ADD CONSTRAINT fk_users_field FOREIGN KEY (primary_field_id) REFERENCES research_fields(field_id);
GO

-- ============================================================
-- 8. JOURNALS
-- ============================================================
CREATE TABLE journals (
    journal_id INT          NOT NULL IDENTITY(1,1),
    name       VARCHAR(500) NOT NULL,
    issn       VARCHAR(20),
    publisher  VARCHAR(300),
    status     VARCHAR(20)  NOT NULL DEFAULT 'active' CONSTRAINT chk_journals_status CHECK (status IN ('active','inactive', 'ACTIVE', 'INACTIVE')),
    PRIMARY KEY (journal_id)
);
GO

-- ============================================================
-- 9. AUTHORS
-- ============================================================
CREATE TABLE authors (
    author_id   BIGINT       NOT NULL IDENTITY(1,1),
    full_name   VARCHAR(300) NOT NULL,
    affiliation VARCHAR(500),
    orcid       VARCHAR(50),
    PRIMARY KEY (author_id)
);
GO

-- ============================================================
-- 10. KEYWORDS
-- ============================================================
CREATE TABLE keywords (
    keyword_id   INT          NOT NULL IDENTITY(1,1),
    keyword_name VARCHAR(200) NOT NULL UNIQUE,
    PRIMARY KEY (keyword_id)
);
GO

-- ============================================================
-- 10.5 TOPICS
-- ============================================================
CREATE TABLE topics (
    topic_id    INT          NOT NULL IDENTITY(1,1),
    topic_name  VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(MAX),
    PRIMARY KEY (topic_id)
);
GO

-- ============================================================
-- 11. API SOURCES
-- ============================================================
CREATE TABLE api_sources (
    source_id   INT          NOT NULL IDENTITY(1,1),
    source_name VARCHAR(100) NOT NULL UNIQUE,
    base_url    VARCHAR(500) NOT NULL,
    api_key_ref VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'active' CONSTRAINT chk_api_sources_status CHECK (status IN ('active','inactive', 'ACTIVE', 'INACTIVE')),
    PRIMARY KEY (source_id)
);
GO

-- ============================================================
-- 12. PAPERS
-- ============================================================
CREATE TABLE papers (
    paper_id           BIGINT       NOT NULL IDENTITY(1,1),
    journal_id         INT,
    field_id           INT,
    api_source_id      INT,
    publication_type   VARCHAR(50)  NOT NULL DEFAULT 'other' CONSTRAINT chk_papers_pub_type CHECK (publication_type IN ('journal_article','conference_paper','preprint','book_chapter','repository_item','other', 'JOURNAL_ARTICLE','CONFERENCE_PAPER','PREPRINT','BOOK_CHAPTER','REPOSITORY_ITEM','OTHER')),
    title              VARCHAR(500) NOT NULL,
    abstract           VARCHAR(MAX),
    publication_year   SMALLINT,
    doi                VARCHAR(300),
    source_url         VARCHAR(500),
    citation_count     INT          NOT NULL DEFAULT 0,
    visibility_status  VARCHAR(20)  NOT NULL DEFAULT 'visible' CONSTRAINT chk_papers_visibility CHECK (visibility_status IN ('visible','hidden', 'VISIBLE', 'HIDDEN')),
    is_open_access     BIT          DEFAULT 0,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (paper_id),
    CONSTRAINT fk_papers_journal
        FOREIGN KEY (journal_id) REFERENCES journals(journal_id) ON DELETE SET NULL,
    CONSTRAINT fk_papers_field
        FOREIGN KEY (field_id) REFERENCES research_fields(field_id) ON DELETE SET NULL,
    CONSTRAINT fk_papers_api_source
        FOREIGN KEY (api_source_id) REFERENCES api_sources(source_id) ON DELETE SET NULL
);

-- SQL Server specific unique filtered index to support multiple NULLs
CREATE UNIQUE NONCLUSTERED INDEX uq_papers_doi ON papers (doi) WHERE doi IS NOT NULL;

CREATE INDEX idx_papers_year ON papers (publication_year);
CREATE INDEX idx_papers_citation ON papers (citation_count);
CREATE INDEX idx_papers_visibility ON papers (visibility_status);
CREATE INDEX idx_papers_type ON papers (publication_type);
GO

-- ============================================================
-- 13. PAPER AUTHORS
-- ============================================================
CREATE TABLE paper_authors (
    paper_id      BIGINT   NOT NULL,
    author_id     BIGINT   NOT NULL,
    author_order  SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (paper_id, author_id),
    CONSTRAINT fk_pa_paper
        FOREIGN KEY (paper_id) REFERENCES papers(paper_id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_author
        FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 14. PAPER KEYWORDS
-- ============================================================
CREATE TABLE paper_keywords (
    paper_id   BIGINT NOT NULL,
    keyword_id INT    NOT NULL,
    PRIMARY KEY (paper_id, keyword_id),
    CONSTRAINT fk_pk_paper
        FOREIGN KEY (paper_id) REFERENCES papers(paper_id) ON DELETE CASCADE,
    CONSTRAINT fk_pk_keyword
        FOREIGN KEY (keyword_id) REFERENCES keywords(keyword_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 14.5 PAPER TOPICS
-- ============================================================
CREATE TABLE paper_topics (
    paper_id BIGINT NOT NULL,
    topic_id INT    NOT NULL,
    PRIMARY KEY (paper_id, topic_id),
    CONSTRAINT fk_pt_paper
        FOREIGN KEY (paper_id) REFERENCES papers(paper_id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_topic
        FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 15. BOOKMARK FOLDERS
-- ============================================================
CREATE TABLE bookmark_folders (
    folder_id    BIGINT       NOT NULL IDENTITY(1,1),
    user_id      BIGINT       NOT NULL,
    folder_name  VARCHAR(200) NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (folder_id),
    CONSTRAINT fk_bf_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 16. FOLDER PAPERS
-- ============================================================
CREATE TABLE folder_papers (
    folder_id  BIGINT   NOT NULL,
    paper_id   BIGINT   NOT NULL,
    saved_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (folder_id, paper_id),
    CONSTRAINT fk_fp_folder
        FOREIGN KEY (folder_id) REFERENCES bookmark_folders(folder_id) ON DELETE CASCADE,
    CONSTRAINT fk_fp_paper
        FOREIGN KEY (paper_id) REFERENCES papers(paper_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 17. FOLLOW TOPIC
-- ============================================================
CREATE TABLE follow_topic (
    user_id      BIGINT   NOT NULL,
    topic_id     INT      NOT NULL,
    followed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, topic_id),
    CONSTRAINT fk_ft_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ft_topic
        FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 18. FOLLOW JOURNAL
-- ============================================================
CREATE TABLE follow_journal (
    user_id      BIGINT   NOT NULL,
    journal_id   INT      NOT NULL,
    followed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, journal_id),
    CONSTRAINT fk_fj_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_fj_journal
        FOREIGN KEY (journal_id) REFERENCES journals(journal_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 18.5 FOLLOW AUTHOR
-- ============================================================
CREATE TABLE follow_author (
    user_id      BIGINT   NOT NULL,
    author_id    BIGINT   NOT NULL,
    followed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, author_id),
    CONSTRAINT fk_fa_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_fa_author
        FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 19. NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    notification_id BIGINT       NOT NULL IDENTITY(1,1),
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(100) NOT NULL,
    title           VARCHAR(300) NOT NULL,
    content         VARCHAR(MAX),
    is_read         TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE INDEX idx_notif_user_read ON notifications (user_id, is_read);
GO

-- ============================================================
-- 20. SEARCH HISTORY
-- ============================================================
CREATE TABLE search_history (
    search_id     BIGINT       NOT NULL IDENTITY(1,1),
    user_id       BIGINT       NOT NULL,
    query_text    VARCHAR(500) NOT NULL,
    search_type   VARCHAR(100) NOT NULL,
    searched_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (search_id),
    CONSTRAINT fk_sh_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE INDEX idx_sh_user_time ON search_history (user_id, searched_at DESC);
GO

-- ============================================================
-- 21. REPORT TICKETS
-- ============================================================
CREATE TABLE report_tickets (
    ticket_id   BIGINT   NOT NULL IDENTITY(1,1),
    user_id     BIGINT   NOT NULL,
    paper_id    BIGINT   NOT NULL,
    reason      VARCHAR(MAX) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'pending' CONSTRAINT chk_rt_status CHECK (status IN ('pending','reviewed','resolved','rejected', 'PENDING','REVIEWED','RESOLVED','REJECTED')),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ticket_id),
    CONSTRAINT fk_rt_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_rt_paper
        FOREIGN KEY (paper_id) REFERENCES papers(paper_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 22. SYNC JOBS
-- ============================================================
CREATE TABLE sync_jobs (
    sync_job_id     BIGINT       NOT NULL IDENTITY(1,1),
    source_id       INT          NOT NULL,
    triggered_by    BIGINT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'running' CONSTRAINT chk_sj_status CHECK (status IN ('running','success','failed', 'RUNNING','SUCCESS','FAILED')),
    added_count     INT          NOT NULL DEFAULT 0,
    updated_count   INT          NOT NULL DEFAULT 0,
    error_message   VARCHAR(MAX),
    started_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     DATETIME,
    PRIMARY KEY (sync_job_id),
    CONSTRAINT fk_sj_source FOREIGN KEY (source_id) REFERENCES api_sources(source_id),
    CONSTRAINT fk_sj_user FOREIGN KEY (triggered_by) REFERENCES users(user_id) ON DELETE SET NULL
);
GO

-- ============================================================
-- SEED BASIC DATA
-- ============================================================
INSERT INTO roles (role_name) VALUES
    ('STUDENT'),
    ('LECTURER'),
    ('RESEARCHER'),
    ('ADMIN'),
    ('MEMBER');
GO

INSERT INTO subscription_plans (plan_name, price, discount, duration_days, description, status) VALUES
    ('FREE', 0.00, 0.00, 99999, 'Free access to basic research paper trends.', 'active'),
    ('PREMIUM_MONTHLY', 49000.00, 0.00, 30, 'Monthly premium access for standard users.', 'active'),
    ('PREMIUM_SEMI_ANNUAL', 249000.00, 15.00, 180, 'Semi-annual premium access with discount included.', 'active'),
    ('PREMIUM_YEARLY', 449000.00, 23.00, 365, 'Annual premium access with discount included.', 'active');
GO

INSERT INTO api_sources (source_name, base_url, status) VALUES
    ('OpenAlex', 'https://api.openalex.org', 'active'),
    ('Semantic Scholar', 'https://api.semanticscholar.org/graph', 'active');
GO

-- Insert test user for testing/validation
DECLARE @member_role_id INT;
SELECT @member_role_id = role_id FROM roles WHERE role_name = 'MEMBER';

IF @member_role_id IS NOT NULL
BEGIN
    INSERT INTO users (role_id, full_name, email, password_hash, status, created_at, updated_at)
    VALUES (@member_role_id, 'Member User', 'member@gmail.com', '$2a$10$e0MYzXy5Z1Kk86o8Y.o40e0G3Z7P6bQ4r7aKqO.Lq4M6qPZ4iOFeS', 'ACTIVE', GETDATE(), GETDATE());
END
GO

-- ============================================================
-- VIEW: v_papers_detail
-- Tự động JOIN papers, paper_authors, authors và đổi tên các cột sang tiếng Anh (Version 3 với DOI và Affiliation)
-- ============================================================
IF OBJECT_ID('v_papers_detail', 'V') IS NOT NULL
    DROP VIEW v_papers_detail;
GO

CREATE VIEW v_papers_detail AS
SELECT 
    p.paper_id AS [ID],
    LEFT(p.title, 60) AS [Title],
    a.full_name AS [Author],
    LEFT(a.affiliation, 40) AS [Affiliation],
    p.publication_year AS [Year],
    p.citation_count AS [Citations],
    p.source_url AS [DOI],
    p.abstract AS [Abstract] 
FROM papers p
JOIN paper_authors pa ON p.paper_id = pa.paper_id
JOIN authors a ON pa.author_id = a.author_id;
GO

-- Added for Sync performance
CREATE INDEX idx_papers_title ON papers (title);
CREATE INDEX idx_papers_created_at ON papers (created_at);
CREATE INDEX idx_authors_fullname ON authors (full_name);
CREATE INDEX idx_journals_name ON journals (name);
