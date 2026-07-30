/*
  Data fix: keywords.trend_score / topics.trend_score were left NULL for rows inserted by
  database/real_data.sql (that script only specifies keyword_name/topic fields — never
  trend_score), unlike rows created through the app via keywordRepository.save() /
  topicRepository.save(), which get 0.0 from each entity's @PrePersist.

  Safe to re-run.
*/

USE PublicationTracker;
GO

UPDATE keywords SET trend_score = 0 WHERE trend_score IS NULL;
GO

IF NOT EXISTS (
    SELECT * FROM sys.default_constraints dc
    WHERE dc.parent_object_id = OBJECT_ID('keywords')
      AND dc.parent_column_id = (SELECT column_id FROM sys.columns WHERE object_id = OBJECT_ID('keywords') AND name = 'trend_score')
)
BEGIN
    ALTER TABLE keywords ADD CONSTRAINT DF_keywords_trend_score DEFAULT 0 FOR trend_score;
    PRINT 'Added DF_keywords_trend_score default constraint';
END
GO

-- topics.trend_score already has a DB-level DEFAULT from the original schema (unlike
-- keywords) — real_data.sql's rows only ended up NULL because they explicitly inserted NULL
-- rather than omitting the column, which bypasses a DEFAULT. Just needs the data fix.
UPDATE topics SET trend_score = 0 WHERE trend_score IS NULL;
GO

PRINT '=== keywords/topics trend_score fix complete ===';
GO
