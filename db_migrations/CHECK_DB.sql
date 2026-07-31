/*
  Read-only health check. Changes nothing.

  Everyone on the team runs this against their own PublicationTracker and compares the output.
  Every row should read OK. Anything reading MISSING or WRONG names the migration to run.

  Faster than asking "did you run it?" — a mismatched column type is invisible until the backend
  fails to start or Vietnamese text comes back as question marks.
*/

USE PublicationTracker;
GO
SET NOCOUNT ON;
GO

PRINT '========= 1. COLUMN TYPES  (migration 2026-07-29) =========';

SELECT
    t.name + '.' + c.name                                  AS [column],
    ty.name                                                AS actual_type,
    CASE WHEN ty.name = 'nvarchar' THEN 'OK' ELSE 'WRONG — run 2026-07-29' END AS verdict
FROM sys.columns c
JOIN sys.tables t   ON t.object_id = c.object_id
JOIN sys.types ty   ON ty.user_type_id = c.user_type_id
WHERE (t.name = 'papers'   AND c.name = 'title')
   OR (t.name = 'authors'  AND c.name IN ('full_name','affiliation'))
   OR (t.name = 'journals' AND c.name IN ('name','publisher'))
   OR (t.name = 'keywords' AND c.name = 'keyword_name')
ORDER BY t.name, c.name;

PRINT '';
PRINT '========= 2. DATABASE SETTINGS =========';

SELECT
    'PARAMETER_SNIFFING'                                            AS setting,
    CAST(value AS VARCHAR)                                          AS actual,
    CASE WHEN value = 0 THEN 'OK' ELSE 'WRONG — run 2026-07-29' END AS verdict
FROM sys.database_scoped_configurations
WHERE name = 'PARAMETER_SNIFFING';

PRINT '';
PRINT '========= 3. TABLES THE BACKEND EXPECTS =========';

SELECT x.name AS [table],
       CASE WHEN OBJECT_ID(x.name) IS NULL THEN 'MISSING — run 2026-07-23_sync_taxonomy' ELSE 'OK' END AS verdict
FROM (VALUES ('topic_domains'), ('topic_fields'), ('topic_subfields'),
             ('scheduler_settings'), ('sync_jobs'), ('paper_topics'), ('notifications')) AS x(name);

PRINT '';
PRINT '========= 4. COLUMNS THE BACKEND EXPECTS =========';

SELECT x.tbl + '.' + x.col AS [column],
       CASE WHEN COL_LENGTH(x.tbl, x.col) IS NULL THEN 'MISSING — run 2026-07-23_sync_taxonomy' ELSE 'OK' END AS verdict
FROM (VALUES ('topics','openalex_id'), ('topics','subfield_id'), ('topics','last_synced_at'),
             ('papers','last_sync_job_id'), ('papers','last_sync_action'),
             ('sync_jobs','total_topics_count'), ('sync_jobs','processed_topics_count')) AS x(tbl, col);

PRINT '';
PRINT '========= 5. SYNC STATUS CONSTRAINT =========';

SELECT 'sync_jobs status allows CANCELED / PARTIAL_SUCCESS' AS [check],
       CASE WHEN EXISTS (SELECT 1 FROM sys.check_constraints
                         WHERE parent_object_id = OBJECT_ID('sync_jobs')
                           AND definition NOT LIKE '%CANCELED%')
            THEN 'WRONG — run 2026-07-23_sync_taxonomy' ELSE 'OK' END AS verdict;

PRINT '';
PRINT '========= 6. DATA SANITY  (migration 2026-07-31) =========';

SELECT 'papers dated in the future' AS [check],
       CAST(COUNT(*) AS VARCHAR)    AS value,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'run 2026-07-31_cleanup' END AS verdict
FROM papers WHERE publication_year > YEAR(GETDATE())
UNION ALL
SELECT 'titles that are archive filenames', CAST(COUNT(*) AS VARCHAR),
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'run 2026-07-31_cleanup' END
FROM papers WHERE title LIKE '%.zip' OR title LIKE '%.rar' OR title LIKE '%.7z'
UNION ALL
SELECT 'titles with lost characters', CAST(COUNT(*) AS VARCHAR),
       CASE WHEN COUNT(*) < 20 THEN 'OK' ELSE 'run 2026-07-29 then Sync All' END
FROM papers WHERE title LIKE '%??%';

PRINT '';
PRINT '========= 7. SCALE  (compare these across machines) =========';

SELECT 'papers'          AS [table], CAST(COUNT(*) AS VARCHAR) AS rows FROM papers
UNION ALL SELECT 'official topics', CAST(COUNT(*) AS VARCHAR) FROM topics WHERE openalex_id IS NOT NULL
UNION ALL SELECT 'authors',         CAST(COUNT(*) AS VARCHAR) FROM authors
UNION ALL SELECT 'journals',        CAST(COUNT(*) AS VARCHAR) FROM journals
UNION ALL SELECT 'sync jobs',       CAST(COUNT(*) AS VARCHAR) FROM sync_jobs;

PRINT '';
PRINT 'Official topics should read 4510. A much lower number means Seed Official Topics';
PRINT 'has not finished — run it from Admin > Data Sync.';
GO
