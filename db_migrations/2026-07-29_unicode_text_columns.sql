/*
  Convert the free-text columns from VARCHAR to NVARCHAR.

  Why: the database collation is SQL_Latin1_General_CP1_CI_AS, so a VARCHAR column can only
  hold code page 1252. Every character outside it — Chinese, Japanese, Korean, Cyrillic, and
  Vietnamese diacritics — was replaced by a literal '?' at INSERT time and lost for good.
  A stored title read back as bytes showed 3F 3F 3F 3F 3F 3F 3F for its first seven characters,
  and "Xu hướng công bố khoa học" round-tripped through VARCHAR as "Xu hu?ng c?ng b? khoa h?c".

  Identifier columns (doi, openalex_id, source_url, issn, status enums) stay VARCHAR: they are
  ASCII by definition and widening them would only double their storage.

  Run against your LOCAL PublicationTracker database, then restart the backend. Safe to re-run —
  each step checks the current column type first and does nothing if already converted.

  Titles corrupted before this migration cannot be repaired from the database, because the
  original characters were never stored. Re-running "Sync All" fetches the correct text from
  OpenAlex and updates them in place.
*/

USE PublicationTracker;
GO

-- The indexes have to go first: SQL Server refuses to ALTER a column that an index keys on.
IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_papers_title' AND object_id = OBJECT_ID('papers'))
BEGIN
    DROP INDEX idx_papers_title ON papers;
    PRINT 'Dropped idx_papers_title';
END
GO

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_authors_fullname' AND object_id = OBJECT_ID('authors'))
BEGIN
    DROP INDEX idx_authors_fullname ON authors;
    PRINT 'Dropped idx_authors_fullname';
END
GO

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_journals_name' AND object_id = OBJECT_ID('journals'))
BEGIN
    DROP INDEX idx_journals_name ON journals;
    PRINT 'Dropped idx_journals_name';
END
GO

-- keyword_name is backed by a system-named UNIQUE constraint, so its name has to be looked up.
DECLARE @uq NVARCHAR(200);
SELECT @uq = kc.name
FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID('keywords') AND c.name = 'keyword_name';

IF @uq IS NOT NULL
BEGIN
    EXEC('ALTER TABLE keywords DROP CONSTRAINT ' + @uq);
    PRINT 'Dropped UNIQUE constraint on keywords.keyword_name';
END
GO

-- Column conversions. Nullability is left exactly as it is today; this migration changes the
-- character type and nothing else.
IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('papers') AND c.name = 'title' AND t.name = 'varchar')
BEGIN
    ALTER TABLE papers ALTER COLUMN title NVARCHAR(500) NULL;
    PRINT 'papers.title -> NVARCHAR(500)';
END
GO

IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('authors') AND c.name = 'full_name' AND t.name = 'varchar')
BEGIN
    ALTER TABLE authors ALTER COLUMN full_name NVARCHAR(255) NULL;
    PRINT 'authors.full_name -> NVARCHAR(255)';
END
GO

IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('authors') AND c.name = 'affiliation' AND t.name = 'varchar')
BEGIN
    ALTER TABLE authors ALTER COLUMN affiliation NVARCHAR(255) NULL;
    PRINT 'authors.affiliation -> NVARCHAR(255)';
END
GO

IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('journals') AND c.name = 'name' AND t.name = 'varchar')
BEGIN
    ALTER TABLE journals ALTER COLUMN name NVARCHAR(255) NULL;
    PRINT 'journals.name -> NVARCHAR(255)';
END
GO

IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('journals') AND c.name = 'publisher' AND t.name = 'varchar')
BEGIN
    ALTER TABLE journals ALTER COLUMN publisher NVARCHAR(255) NULL;
    PRINT 'journals.publisher -> NVARCHAR(255)';
END
GO

IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('keywords') AND c.name = 'keyword_name' AND t.name = 'varchar')
BEGIN
    ALTER TABLE keywords ALTER COLUMN keyword_name NVARCHAR(255) NULL;
    PRINT 'keywords.keyword_name -> NVARCHAR(255)';
END
GO

IF EXISTS (SELECT * FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
           WHERE c.object_id = OBJECT_ID('topics') AND c.name = 'description' AND t.name = 'varchar')
BEGIN
    ALTER TABLE topics ALTER COLUMN description NVARCHAR(MAX) NULL;
    PRINT 'topics.description -> NVARCHAR(MAX)';
END
GO

-- Put the indexes back. NVARCHAR(500) keys at 1000 bytes, inside the 1700-byte non-clustered
-- limit on SQL Server 2016 and later.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_papers_title' AND object_id = OBJECT_ID('papers'))
BEGIN
    CREATE INDEX idx_papers_title ON papers(title);
    PRINT 'Recreated idx_papers_title';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_authors_fullname' AND object_id = OBJECT_ID('authors'))
BEGIN
    CREATE INDEX idx_authors_fullname ON authors(full_name);
    PRINT 'Recreated idx_authors_fullname';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_journals_name' AND object_id = OBJECT_ID('journals'))
BEGIN
    CREATE INDEX idx_journals_name ON journals(name);
    PRINT 'Recreated idx_journals_name';
END
GO

IF NOT EXISTS (
    SELECT * FROM sys.key_constraints kc
    JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = OBJECT_ID('keywords') AND c.name = 'keyword_name'
)
BEGIN
    ALTER TABLE keywords ADD CONSTRAINT UQ_keywords_keyword_name UNIQUE (keyword_name);
    PRINT 'Recreated UNIQUE constraint on keywords.keyword_name';
END
GO

PRINT '=== Unicode column migration complete ===';
GO
