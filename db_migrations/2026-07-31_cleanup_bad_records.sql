/*
  Removes records that are not publications, plus papers dated in the future.

  The sync no longer lets either kind in — an upper bound on publication_date and a work-type
  filter were added on 2026-07-31 — but a database populated before that still holds them, and
  they are visible: papers dated 2050 put stray columns on the dashboard chart, and Zenodo's bulk
  .zip uploads of public-domain novels carry research topics that skew every growth rate they
  land on.

  Run this AFTER 2026-07-29_unicode_text_columns.sql, on the same local database.

  Everything deleted is copied to bk_* tables first, so any step can be undone. Drop those tables
  once you are satisfied.

  For reference, on the machine this was written against: 869 future-dated papers and 1,079
  non-publications, out of roughly 26,000.
*/

USE PublicationTracker;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Papers dated after today. A publisher placeholder date, copied verbatim by OpenAlex —
--    Cairn.info files a 2024 issue as 2050-01-01 — which the newest-first sort then puts on
--    page one of every topic, so these were re-fetched on every run.
IF OBJECT_ID('bk_cleanup_future_papers') IS NULL
BEGIN
    SELECT * INTO bk_cleanup_future_papers FROM papers WHERE publication_year > YEAR(GETDATE());
    PRINT 'Backed up future-dated papers: ' + CAST(@@ROWCOUNT AS VARCHAR);
END
GO

-- 2. Titles that are just an archive filename. These are bulk file deposits, not papers. The
--    work type does not identify them: they arrive as type=book, alongside real academic books.
IF OBJECT_ID('bk_cleanup_archive_papers') IS NULL
BEGIN
    SELECT * INTO bk_cleanup_archive_papers FROM papers
    WHERE title LIKE '%.zip' OR title LIKE '%.tar' OR title LIKE '%.tar.gz'
       OR title LIKE '%.tgz' OR title LIKE '%.rar' OR title LIKE '%.7z';
    PRINT 'Backed up archive-filename papers: ' + CAST(@@ROWCOUNT AS VARCHAR);
END
GO

-- Delete children first: paper_authors, paper_keywords and paper_topics have no cascade.
DECLARE @doomed TABLE (paper_id BIGINT PRIMARY KEY);

INSERT INTO @doomed (paper_id)
SELECT paper_id FROM papers
WHERE publication_year > YEAR(GETDATE())
   OR title LIKE '%.zip' OR title LIKE '%.tar' OR title LIKE '%.tar.gz'
   OR title LIKE '%.tgz' OR title LIKE '%.rar' OR title LIKE '%.7z';

DECLARE @doomedCount INT = (SELECT COUNT(*) FROM @doomed);
PRINT 'Rows to delete: ' + CAST(@doomedCount AS VARCHAR);

-- Anything a user has acted on is left alone: a bookmark or a report means someone can see it
-- disappear, which is worse than one odd row on a chart.
DELETE FROM @doomed WHERE paper_id IN (SELECT paper_id FROM bookmark_papers)
                       OR paper_id IN (SELECT paper_id FROM report_tickets WHERE paper_id IS NOT NULL);

BEGIN TRANSACTION;
    DELETE x FROM paper_authors  x JOIN @doomed d ON d.paper_id = x.paper_id;
    DELETE x FROM paper_keywords x JOIN @doomed d ON d.paper_id = x.paper_id;
    DELETE x FROM paper_topics   x JOIN @doomed d ON d.paper_id = x.paper_id;
    DELETE p FROM papers         p JOIN @doomed d ON d.paper_id = p.paper_id;
COMMIT TRANSACTION;
GO

-- 3. Report what is left. Non-zero here is fine — it means those rows are bookmarked or reported
--    and were deliberately kept.
SELECT 'future-dated papers remaining' AS check_name, COUNT(*) AS value FROM papers WHERE publication_year > YEAR(GETDATE())
UNION ALL SELECT 'archive-filename papers remaining', COUNT(*) FROM papers WHERE title LIKE '%.zip'
UNION ALL SELECT 'papers total', COUNT(*) FROM papers
UNION ALL SELECT 'titles still holding lost characters', COUNT(*) FROM papers WHERE title LIKE '%??%'
UNION ALL SELECT 'newest publication year', MAX(publication_year) FROM papers;
GO

PRINT '=== Cleanup complete. Re-run Sync All afterwards to refresh titles. ===';
GO
