/*
  Migration for feature/sync (Implementation Plan v3): official OpenAlex topic taxonomy
  + structured sync job tracking.

  Run this against your LOCAL PublicationTracker database in SSMS after pulling the
  feature/sync / develop branch. Safe to re-run — every step checks for existing
  objects first, so running it twice does nothing on the second run.

  After running this script, start the backend once (Spring Boot ddl-auto=update
  will also validate/add anything the app entities still expect) and then trigger
  "Seed Official Topics" from the admin Data Sync page to populate the new tables.
*/

USE PublicationTracker;
GO

-- 1. topic_domains
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'topic_domains')
BEGIN
    CREATE TABLE topic_domains (
        domain_id     INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        openalex_id   VARCHAR(255) NOT NULL,
        display_name  VARCHAR(255) NOT NULL,
        CONSTRAINT UQ_topic_domains_openalex_id UNIQUE (openalex_id)
    );
    PRINT 'Created table topic_domains';
END
GO

-- 2. topic_fields
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'topic_fields')
BEGIN
    CREATE TABLE topic_fields (
        field_id      INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        openalex_id   VARCHAR(255) NOT NULL,
        display_name  VARCHAR(255) NOT NULL,
        domain_id     INT NOT NULL,
        CONSTRAINT UQ_topic_fields_openalex_id UNIQUE (openalex_id),
        CONSTRAINT FK_topic_fields_domain FOREIGN KEY (domain_id) REFERENCES topic_domains(domain_id)
    );
    PRINT 'Created table topic_fields';
END
GO

-- 3. topic_subfields
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'topic_subfields')
BEGIN
    CREATE TABLE topic_subfields (
        subfield_id   INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        openalex_id   VARCHAR(255) NOT NULL,
        display_name  VARCHAR(255) NOT NULL,
        field_id      INT NOT NULL,
        CONSTRAINT UQ_topic_subfields_openalex_id UNIQUE (openalex_id),
        CONSTRAINT FK_topic_subfields_field FOREIGN KEY (field_id) REFERENCES topic_fields(field_id)
    );
    PRINT 'Created table topic_subfields';
END
GO

-- 4. topics: new columns (openalex_id, subfield_id, last_synced_at)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('topics') AND name = 'openalex_id')
BEGIN
    ALTER TABLE topics ADD openalex_id VARCHAR(255) NULL;
    PRINT 'Added topics.openalex_id';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('topics') AND name = 'subfield_id')
BEGIN
    ALTER TABLE topics ADD subfield_id INT NULL;
    PRINT 'Added topics.subfield_id';
END
GO

IF NOT EXISTS (
    SELECT * FROM sys.foreign_keys fk
    WHERE fk.parent_object_id = OBJECT_ID('topics')
      AND fk.referenced_object_id = OBJECT_ID('topic_subfields')
)
BEGIN
    ALTER TABLE topics ADD CONSTRAINT FK_topics_subfield FOREIGN KEY (subfield_id) REFERENCES topic_subfields(subfield_id);
    PRINT 'Added FK_topics_subfield';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('topics') AND name = 'last_synced_at')
BEGIN
    ALTER TABLE topics ADD last_synced_at DATETIME2 NULL;
    PRINT 'Added topics.last_synced_at';
END
GO

-- 5. scheduler_settings (single-row table: global on/off switch for the weekly background sync)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'scheduler_settings')
BEGIN
    CREATE TABLE scheduler_settings (
        id       INT NOT NULL PRIMARY KEY,
        enabled  BIT NOT NULL
    );
    PRINT 'Created table scheduler_settings';
END
GO

-- 6. papers: last_sync_job_id / last_sync_action (which sync job last touched this paper)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('papers') AND name = 'last_sync_job_id')
BEGIN
    ALTER TABLE papers ADD last_sync_job_id BIGINT NULL;
    PRINT 'Added papers.last_sync_job_id';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('papers') AND name = 'last_sync_action')
BEGIN
    ALTER TABLE papers ADD last_sync_action VARCHAR(10) NULL;
    PRINT 'Added papers.last_sync_action';
END
GO

-- 7. sync_jobs: total_topics_count / processed_topics_count (progress tracking for Sync All)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('sync_jobs') AND name = 'total_topics_count')
BEGIN
    ALTER TABLE sync_jobs ADD total_topics_count INT NULL;
    PRINT 'Added sync_jobs.total_topics_count';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('sync_jobs') AND name = 'processed_topics_count')
BEGIN
    ALTER TABLE sync_jobs ADD processed_topics_count INT NULL;
    PRINT 'Added sync_jobs.processed_topics_count';
END
GO

-- 8. sync_jobs: fix status CHECK constraint to allow CANCELED / PARTIAL_SUCCESS
--    (older constraint only allowed RUNNING/SUCCESS/FAILED, so Cancel/Retry silently failed at the DB level)
IF EXISTS (
    SELECT * FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('sync_jobs')
      AND definition NOT LIKE '%CANCELED%'
)
BEGIN
    DECLARE @constraintName NVARCHAR(200);
    SELECT @constraintName = cc.name
    FROM sys.check_constraints cc
    WHERE cc.parent_object_id = OBJECT_ID('sync_jobs')
      AND cc.definition NOT LIKE '%CANCELED%';

    EXEC('ALTER TABLE sync_jobs DROP CONSTRAINT ' + @constraintName);
    ALTER TABLE sync_jobs ADD CONSTRAINT chk_sync_jobs_status
        CHECK ([status] IN ('RUNNING','SUCCESS','FAILED','CANCELED','PARTIAL_SUCCESS'));
    PRINT 'Fixed sync_jobs status CHECK constraint (added CANCELED, PARTIAL_SUCCESS)';
END
GO

PRINT '=== Migration complete ===';
GO
