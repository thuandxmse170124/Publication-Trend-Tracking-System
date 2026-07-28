USE [master]
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'PublicationTracker')
BEGIN
    ALTER DATABASE [PublicationTracker] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [PublicationTracker];
END
GO

/****** Object:  Database [PublicationTracker]    Script Date: 17/06/2026 13:43:47 ******/
CREATE DATABASE [PublicationTracker]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'PublicationTracker', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL15.MSSQLSERVER\MSSQL\DATA\PublicationTracker.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'PublicationTracker_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL15.MSSQLSERVER\MSSQL\DATA\PublicationTracker_log.ldf' , SIZE = 73728KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT
GO
ALTER DATABASE [PublicationTracker] SET COMPATIBILITY_LEVEL = 150
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [PublicationTracker].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [PublicationTracker] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [PublicationTracker] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [PublicationTracker] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [PublicationTracker] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [PublicationTracker] SET ARITHABORT OFF 
GO
ALTER DATABASE [PublicationTracker] SET AUTO_CLOSE OFF 
GO
ALTER DATABASE [PublicationTracker] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [PublicationTracker] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [PublicationTracker] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [PublicationTracker] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [PublicationTracker] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [PublicationTracker] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [PublicationTracker] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [PublicationTracker] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [PublicationTracker] SET  DISABLE_BROKER 
GO
ALTER DATABASE [PublicationTracker] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [PublicationTracker] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [PublicationTracker] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [PublicationTracker] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [PublicationTracker] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [PublicationTracker] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [PublicationTracker] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [PublicationTracker] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [PublicationTracker] SET  MULTI_USER 
GO
ALTER DATABASE [PublicationTracker] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [PublicationTracker] SET DB_CHAINING OFF 
GO
ALTER DATABASE [PublicationTracker] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [PublicationTracker] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [PublicationTracker] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [PublicationTracker] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
EXEC sys.sp_db_vardecimal_storage_format N'PublicationTracker', N'ON'
GO
ALTER DATABASE [PublicationTracker] SET QUERY_STORE = ON
GO
ALTER DATABASE [PublicationTracker] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [PublicationTracker]
GO
/****** Object:  Table [dbo].[users]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[users](
	[user_id] [bigint] IDENTITY(1,1) NOT NULL,
	[role_id] [int] NOT NULL,
	[full_name] [varchar](255) NULL,
	[email] [varchar](255) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[status] [varchar](255) NULL,
	[created_at] [datetime] NOT NULL,
	[updated_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[authors]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[authors](
	[author_id] [bigint] IDENTITY(1,1) NOT NULL,
	[full_name] [varchar](255) NULL,
	[affiliation] [varchar](255) NULL,
	[orcid] [varchar](50) NULL,
	[deleted_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[author_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[papers]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[papers](
	[paper_id] [bigint] IDENTITY(1,1) NOT NULL,
	[journal_id] [int] NULL,
	[field_id] [int] NULL,
	[api_source_id] [int] NULL,
	[publication_type] [varchar](255) NULL,
	[title] [varchar](255) NULL,
	[abstract] [varchar](max) NULL,
	[publication_year] [smallint] NULL,
	[doi] [varchar](300) NULL,
	[source_url] [varchar](255) NULL,
	[citation_count] [int] NOT NULL,
	[visibility_status] [varchar](255) NULL,
	[deleted_at] [datetime] NULL,
	[created_at] [datetime] NOT NULL,
	[updated_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[paper_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[paper_authors]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[paper_authors](
	[paper_id] [bigint] NOT NULL,
	[author_id] [bigint] NOT NULL,
	[author_order] [smallint] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[paper_id] ASC,
	[author_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[v_papers_detail]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[v_papers_detail] AS
SELECT 
    p.paper_id         AS [ID],
    LEFT(p.title, 60)  AS [Title],
    a.full_name        AS [Author],
    LEFT(COALESCE(a.affiliation, ''), 40) AS [Affiliation],
    p.publication_year AS [Year],
    p.citation_count   AS [Citations],
    p.doi              AS [DOI],
    p.source_url       AS [SourceURL],
    p.publication_type AS [Type],
    pa.author_order    AS [AuthorOrder]
FROM papers p
JOIN paper_authors pa ON p.paper_id = pa.paper_id
JOIN authors a        ON pa.author_id = a.author_id
WHERE p.deleted_at IS NULL AND a.deleted_at IS NULL;

GO
/****** Object:  Table [dbo].[api_sources]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[api_sources](
	[source_id] [int] IDENTITY(1,1) NOT NULL,
	[source_name] [varchar](255) NULL,
	[base_url] [varchar](255) NULL,
	[api_key_ref] [varchar](255) NULL,
	[status] [varchar](255) NULL,
	[last_synced_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[source_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[source_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[journals]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[journals](
	[journal_id] [int] IDENTITY(1,1) NOT NULL,
	[name] [varchar](255) NULL,
	[issn] [varchar](255) NULL,
	[publisher] [varchar](255) NULL,
	[status] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[journal_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[keywords]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[keywords](
	[keyword_id] [int] IDENTITY(1,1) NOT NULL,
	[keyword_name] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[keyword_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[keyword_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[paper_keywords]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[paper_keywords](
	[paper_id] [bigint] NOT NULL,
	[keyword_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[paper_id] ASC,
	[keyword_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[paper_topics]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[paper_topics](
	[paper_id] [bigint] NOT NULL,
	[topic_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[paper_id] ASC,
	[topic_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[password_reset_tokens]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[password_reset_tokens](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NULL,
	[token] [varchar](255) NOT NULL,
	[expiry_time] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[pending_registrations]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[pending_registrations](
	[pending_id] [bigint] IDENTITY(1,1) NOT NULL,
	[full_name] [nvarchar](255) NOT NULL,
	[email] [varchar](255) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[otp_code] [varchar](255) NULL,
	[otp_expired_at] [datetime] NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[pending_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[research_fields]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[research_fields](
	[field_id] [int] IDENTITY(1,1) NOT NULL,
	[field_name] [nvarchar](200) NOT NULL,
	[description] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[field_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[field_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[roles]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[roles](
	[role_id] [int] IDENTITY(1,1) NOT NULL,
	[role_name] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[role_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[search_history]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[search_history](
	[search_id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[query_text] [nvarchar](500) NOT NULL,
	[search_type] [varchar](50) NOT NULL,
	[searched_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[search_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[topics]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[topics](
	[topic_id] [int] IDENTITY(1,1) NOT NULL,
	[topic_name] [nvarchar](200) NOT NULL,
	[description] [varchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[topic_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[topic_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

/****** Object:  Table [dbo].[sync_jobs]    Script Date: 17/06/2026 13:43:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[sync_jobs] (
    [sync_job_id]   [bigint] IDENTITY(1,1) NOT NULL,
    [source_id]     [int] NOT NULL,
    [triggered_by]  [bigint] NULL,
    [status]        [varchar](50) NOT NULL DEFAULT 'RUNNING',
    [added_count]   [int] NOT NULL DEFAULT 0,
    [updated_count] [int] NOT NULL DEFAULT 0,
    [error_message] [varchar](max) NULL,
    [started_at]    [datetime] NOT NULL DEFAULT CURRENT_TIMESTAMP,
    [finished_at]   [datetime] NULL,
    PRIMARY KEY CLUSTERED ([sync_job_id] ASC),
    CONSTRAINT [fk_sj_source] FOREIGN KEY([source_id]) REFERENCES [dbo].[api_sources] ([source_id]),
    CONSTRAINT [fk_sj_user] FOREIGN KEY([triggered_by]) REFERENCES [dbo].[users] ([user_id]) ON DELETE SET NULL,
    CONSTRAINT [chk_sync_jobs_status] CHECK ([status]='RUNNING' OR [status]='SUCCESS' OR [status]='FAILED')
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [uq_authors_orcid]    Script Date: 17/06/2026 13:43:47 ******/
CREATE UNIQUE NONCLUSTERED INDEX [uq_authors_orcid] ON [dbo].[authors]
(
	[orcid] ASC
)
WHERE ([orcid] IS NOT NULL AND [deleted_at] IS NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_citation]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_citation] ON [dbo].[papers]
(
	[citation_count] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_field_id]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_field_id] ON [dbo].[papers]
(
	[field_id] ASC
)
WHERE ([field_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_journal_id]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_journal_id] ON [dbo].[papers]
(
	[journal_id] ASC
)
WHERE ([journal_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_source_id]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_source_id] ON [dbo].[papers]
(
	[api_source_id] ASC
)
WHERE ([api_source_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [idx_papers_type]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_type] ON [dbo].[papers]
(
	[publication_type] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [idx_papers_visibility]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_visibility] ON [dbo].[papers]
(
	[visibility_status] ASC
)
WHERE ([deleted_at] IS NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_year]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_papers_year] ON [dbo].[papers]
(
	[publication_year] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [uq_papers_doi]    Script Date: 17/06/2026 13:43:47 ******/
CREATE UNIQUE NONCLUSTERED INDEX [uq_papers_doi] ON [dbo].[papers]
(
	[doi] ASC
)
WHERE ([doi] IS NOT NULL AND [deleted_at] IS NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_sh_user_time]    Script Date: 17/06/2026 13:43:47 ******/
CREATE NONCLUSTERED INDEX [idx_sh_user_time] ON [dbo].[search_history]
(
	[user_id] ASC,
	[searched_at] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[api_sources] ADD  DEFAULT ('ACTIVE') FOR [status]
GO
ALTER TABLE [dbo].[journals] ADD  DEFAULT ('ACTIVE') FOR [status]
GO
ALTER TABLE [dbo].[paper_authors] ADD  DEFAULT ((1)) FOR [author_order]
GO
ALTER TABLE [dbo].[papers] ADD  DEFAULT ('OTHER') FOR [publication_type]
GO
ALTER TABLE [dbo].[papers] ADD  DEFAULT ((0)) FOR [citation_count]
GO
ALTER TABLE [dbo].[papers] ADD  DEFAULT ('VISIBLE') FOR [visibility_status]
GO
ALTER TABLE [dbo].[papers] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[papers] ADD  DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[pending_registrations] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[search_history] ADD  DEFAULT (getdate()) FOR [searched_at]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT ('PENDING') FOR [status]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[paper_authors]  WITH CHECK ADD  CONSTRAINT [fk_pa_author] FOREIGN KEY([author_id])
REFERENCES [dbo].[authors] ([author_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[paper_authors] CHECK CONSTRAINT [fk_pa_author]
GO
ALTER TABLE [dbo].[paper_authors]  WITH CHECK ADD  CONSTRAINT [fk_pa_paper] FOREIGN KEY([paper_id])
REFERENCES [dbo].[papers] ([paper_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[paper_authors] CHECK CONSTRAINT [fk_pa_paper]
GO
ALTER TABLE [dbo].[paper_keywords]  WITH CHECK ADD  CONSTRAINT [fk_pk_keyword] FOREIGN KEY([keyword_id])
REFERENCES [dbo].[keywords] ([keyword_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[paper_keywords] CHECK CONSTRAINT [fk_pk_keyword]
GO
ALTER TABLE [dbo].[paper_keywords]  WITH CHECK ADD  CONSTRAINT [fk_pk_paper] FOREIGN KEY([paper_id])
REFERENCES [dbo].[papers] ([paper_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[paper_keywords] CHECK CONSTRAINT [fk_pk_paper]
GO
ALTER TABLE [dbo].[paper_topics]  WITH CHECK ADD  CONSTRAINT [fk_pt_paper] FOREIGN KEY([paper_id])
REFERENCES [dbo].[papers] ([paper_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[paper_topics] CHECK CONSTRAINT [fk_pt_paper]
GO
ALTER TABLE [dbo].[paper_topics]  WITH CHECK ADD  CONSTRAINT [fk_pt_topic] FOREIGN KEY([topic_id])
REFERENCES [dbo].[topics] ([topic_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[paper_topics] CHECK CONSTRAINT [fk_pt_topic]
GO
ALTER TABLE [dbo].[papers]  WITH CHECK ADD  CONSTRAINT [fk_papers_api_source] FOREIGN KEY([api_source_id])
REFERENCES [dbo].[api_sources] ([source_id])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[papers] CHECK CONSTRAINT [fk_papers_api_source]
GO
ALTER TABLE [dbo].[papers]  WITH CHECK ADD  CONSTRAINT [fk_papers_field] FOREIGN KEY([field_id])
REFERENCES [dbo].[research_fields] ([field_id])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[papers] CHECK CONSTRAINT [fk_papers_field]
GO
ALTER TABLE [dbo].[papers]  WITH CHECK ADD  CONSTRAINT [fk_papers_journal] FOREIGN KEY([journal_id])
REFERENCES [dbo].[journals] ([journal_id])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[papers] CHECK CONSTRAINT [fk_papers_journal]
GO
ALTER TABLE [dbo].[password_reset_tokens]  WITH CHECK ADD  CONSTRAINT [fk_prt_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[password_reset_tokens] CHECK CONSTRAINT [fk_prt_user]
GO
ALTER TABLE [dbo].[search_history]  WITH CHECK ADD  CONSTRAINT [fk_sh_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[search_history] CHECK CONSTRAINT [fk_sh_user]
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD  CONSTRAINT [fk_users_role] FOREIGN KEY([role_id])
REFERENCES [dbo].[roles] ([role_id])
GO
ALTER TABLE [dbo].[users] CHECK CONSTRAINT [fk_users_role]
GO
ALTER TABLE [dbo].[api_sources]  WITH CHECK ADD  CONSTRAINT [chk_api_sources_status] CHECK  (([status]='INACTIVE' OR [status]='ACTIVE'))
GO
ALTER TABLE [dbo].[api_sources] CHECK CONSTRAINT [chk_api_sources_status]
GO
ALTER TABLE [dbo].[journals]  WITH CHECK ADD  CONSTRAINT [chk_journals_status] CHECK  (([status]='INACTIVE' OR [status]='ACTIVE'))
GO
ALTER TABLE [dbo].[journals] CHECK CONSTRAINT [chk_journals_status]
GO
ALTER TABLE [dbo].[papers]  WITH CHECK ADD  CONSTRAINT [chk_papers_citation] CHECK  (([citation_count]>=(0)))
GO
ALTER TABLE [dbo].[papers] CHECK CONSTRAINT [chk_papers_citation]
GO
ALTER TABLE [dbo].[papers]  WITH CHECK ADD  CONSTRAINT [chk_papers_pub_type] CHECK  (([publication_type]='OTHER' OR [publication_type]='REPOSITORY_ITEM' OR [publication_type]='BOOK_CHAPTER' OR [publication_type]='PREPRINT' OR [publication_type]='CONFERENCE_PAPER' OR [publication_type]='JOURNAL_ARTICLE'))
GO
ALTER TABLE [dbo].[papers] CHECK CONSTRAINT [chk_papers_pub_type]
GO
ALTER TABLE [dbo].[papers]  WITH CHECK ADD  CONSTRAINT [chk_papers_visibility] CHECK  (([visibility_status]='HIDDEN' OR [visibility_status]='VISIBLE'))
GO
ALTER TABLE [dbo].[papers] CHECK CONSTRAINT [chk_papers_visibility]
GO
ALTER TABLE [dbo].[search_history]  WITH CHECK ADD  CONSTRAINT [chk_sh_type] CHECK  (([search_type]='ADVANCED' OR [search_type]='FIELD' OR [search_type]='TOPIC' OR [search_type]='JOURNAL' OR [search_type]='AUTHOR' OR [search_type]='KEYWORD'))
GO
ALTER TABLE [dbo].[search_history] CHECK CONSTRAINT [chk_sh_type]
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD  CONSTRAINT [chk_users_status] CHECK  (([status]='PENDING' OR [status]='BANNED' OR [status]='INACTIVE' OR [status]='ACTIVE'))
GO
ALTER TABLE [dbo].[users] CHECK CONSTRAINT [chk_users_status]
GO


-- ============================================================
-- SEED DATA (ROLES, API SOURCES, TEST USERS, AND REAL RESEARCH DATA)
-- Added automatically: 2026-06-17 15:10:25
-- ============================================================

-- SEED BASIC DATA
-- ============================================================
INSERT INTO roles (role_name) VALUES
    ('ADMIN'),
    ('MEMBER');
GO

INSERT INTO api_sources (source_name, base_url, status) VALUES
    ('OpenAlex', 'https://api.openalex.org', 'ACTIVE'),
    ('Semantic Scholar', 'https://api.semanticscholar.org/graph', 'ACTIVE');
GO

-- ============================================================
-- SEED TEST USERS (FOR TESTING/VALIDATION ONLY - DO NOT USE IN PRODUCTION)
-- Passwords are bcrypt hashes of '123456Aa@'
-- ============================================================
DECLARE @member_role_id INT, @admin_role_id INT;
SELECT @member_role_id = role_id FROM roles WHERE role_name = 'MEMBER';
SELECT @admin_role_id  = role_id FROM roles WHERE role_name = 'ADMIN';

IF @member_role_id IS NOT NULL
BEGIN
    INSERT INTO users (role_id, full_name, email, password_hash, status, created_at, updated_at)
    VALUES (@member_role_id, 'Member User', 'member@gmail.com', '$2a$10$iocTXseLdQ6HL9hl3r6JlO4W0pQKW2dIn6arNV0QiDS1pkgO/dNAm', 'ACTIVE', GETDATE(), GETDATE());
END

IF @admin_role_id IS NOT NULL
BEGIN
    INSERT INTO users (role_id, full_name, email, password_hash, status, created_at, updated_at)
    VALUES (@admin_role_id, 'Admin User', 'admin@gmail.com', '$2a$10$iocTXseLdQ6HL9hl3r6JlO4W0pQKW2dIn6arNV0QiDS1pkgO/dNAm', 'ACTIVE', GETDATE(), GETDATE());
END
GO

-- ============================================================

GO

-- RESEARCH FIELDS
-- ============================================================
INSERT INTO research_fields (field_name, description) VALUES
    ('Computer Science', 'Research related to AI, machine learning, computer vision, NLP, and data mining.'),
    ('Bioinformatics', 'Research related to computational biology, molecular evolution, and biomedical analysis.');

-- ============================================================
-- JOURNALS
-- ============================================================
INSERT INTO journals (name, issn, publisher, status) VALUES
    ('Lecture Notes in Computer Science', '', 'Springer', 'ACTIVE'),
    ('Nature', '', 'Springer Nature', 'ACTIVE'),
    ('DROPS (Schloss Dagstuhl - Leibniz Center for Informatics)', '', 'Schloss Dagstuhl', 'ACTIVE'),
    ('Communications of the ACM', '', 'ACM', 'ACTIVE'),
    ('arXiv (Cornell University)', '', 'Cornell University', 'ACTIVE'),
    ('IEEE Transactions on Pattern Analysis and Machine Intelligence', '', 'IEEE', 'ACTIVE'),
    ('Molecular Biology and Evolution', '', 'Oxford University Press', 'ACTIVE'),
    ('International Journal of Computer Vision', '', 'Springer', 'ACTIVE'),
    ('Bioinformatics', '', 'Oxford University Press', 'ACTIVE');

-- ============================================================
-- AUTHORS
-- ============================================================
INSERT INTO authors (full_name, affiliation, orcid) VALUES
    ('Kathryn Tunyasuvunakool', 'Google DeepMind (United Kingdom)', NULL),
    ('Alexander Pritzel', 'Google DeepMind (United Kingdom)', NULL),
    ('Jonathan Krause', 'Stanford University', NULL),
    ('John Jumper', 'Google DeepMind (United Kingdom)', NULL),
    ('Ilya Sutskever', 'University of Toronto', NULL),
    ('Serge Belongie', 'Cornell University', NULL),
    ('Scott Reed', 'University of Michigan - Ann Arbor', NULL),
    ('Russ Bates', 'Google DeepMind (United Kingdom)', NULL),
    ('Geoffrey E. Hinton', 'Google (United States)', NULL),
    ('Koichiro Tamura', 'Tokyo Metropolitan University', NULL),
    ('Aditya Khosla', 'Massachusetts Institute of Technology', NULL),
    ('Jian Sun', 'Microsoft Research (United Kingdom)', NULL),
    ('Piotr Dollar', 'Microsoft (United States)', NULL),
    ('Jonathan Long', 'University of California, Berkeley', NULL),
    ('Nitish Srivastava', 'University of Toronto', NULL),
    ('Michalina Pacholska', 'Google DeepMind (United Kingdom)', NULL),
    ('James Hays', 'John Brown University', NULL),
    ('Yoshua Bengio', 'Universite de Montreal', NULL),
    ('Vincent Vanhoucke', 'IGlobal University', NULL),
    ('Tim Green', 'Google DeepMind (United Kingdom)', NULL),
    ('Hao Su', 'Stanford University', NULL),
    ('Michael S. Bernstein', 'Stanford University', NULL),
    ('Andrej Karpathy', 'Stanford University', NULL),
    ('Pushmeet Kohli', 'Google DeepMind (United Kingdom)', NULL),
    ('Michael Maire', 'California Institute of Technology', NULL),
    ('Alexandros Stamatakis', 'Karlsruhe Institute of Technology', NULL),
    ('Zhiheng Huang', 'Stanford University', NULL),
    ('Christopher D. Manning', 'Stanford University', NULL),
    ('Ruslan Salakhutdinov', 'University of Toronto', NULL),
    ('Andrew J. Ballard', 'Google DeepMind (United Kingdom)', NULL),
    ('Augustin Zidek', 'Google DeepMind (United Kingdom)', NULL),
    ('Sudhir Kumar', 'King Abdulaziz University', NULL),
    ('Daniel S. Peterson', 'Arizona State University', NULL),
    ('Philipp Fischer', 'University of Freiburg', NULL),
    ('Gao Huang', 'Cornell University', NULL),
    ('Anna Potapenko', 'Google DeepMind (United Kingdom)', NULL),
    ('Zhuang Liu', 'Tsinghua University', NULL),
    ('Laurens van der Maaten', 'Meta (Israel)', NULL),
    ('David Reiman', 'Google DeepMind (United Kingdom)', NULL),
    ('Karen Simonyan', 'University of Oxford', NULL),
    ('Ross Girshick', 'Meta (United States)', NULL),
    ('Andrew Zisserman', 'University of Oxford', NULL),
    ('Dumitru Erhan', 'Google (United States)', NULL),
    ('Alex Krizhevsky', 'University of Toronto', NULL),
    ('Tamas Berghammer', 'Google DeepMind (United Kingdom)', NULL),
    ('Deva Ramanan', 'UC Irvine Health', NULL),
    ('Oriol Vinyals', 'Google DeepMind (United Kingdom)', NULL),
    ('Wei Liu', 'University of North Carolina at Chapel Hill', NULL),
    ('Thomas Brox', 'University of Freiburg', NULL),
    ('Glen Stecher', 'Arizona State University', NULL),
    ('Andrew Rabinovich', 'Magic Leap (United States)', NULL),
    ('Demis Hassabis', 'Google DeepMind (United Kingdom)', NULL),
    ('Clemens Meyer', 'Google DeepMind (United Kingdom)', NULL),
    ('Jeffrey Pennington', 'Stanford University', NULL),
    ('Jia Deng', 'University of Michigan - Ann Arbor', NULL),
    ('Xiangyu Zhang', 'Microsoft Research (United Kingdom)', NULL),
    ('Ellen Clancy', 'Google DeepMind (United Kingdom)', NULL),
    ('Rishub Jain', 'Google DeepMind (United Kingdom)', NULL),
    ('Sanjeev Satheesh', 'Stanford University', NULL),
    ('Michael Figurnov', 'Google DeepMind (United Kingdom)', NULL),
    ('Alan Filipski', 'Arizona State University', NULL),
    ('Pierre Sermanet', 'IGlobal University', NULL),
    ('Pietro Perona', 'California Institute of Technology', NULL),
    ('Sean Ma', 'Stanford University', NULL),
    ('Olga Russakovsky', 'Stanford University', NULL),
    ('Alexander C. Berg', 'University of North Carolina at Chapel Hill', NULL),
    ('Tsung-Yi Lin', 'Cornell University', NULL),
    ('Jian Sun', 'Microsoft (United States)', NULL),
    ('Jonas Adler', 'Google DeepMind (United Kingdom)', NULL),
    ('Dragomir Anguelov', 'Google (United States)', NULL),
    ('Alex Bridgland', 'Google DeepMind (United Kingdom)', NULL),
    ('Evan Shelhamer', 'University of California, Berkeley', NULL),
    ('Koray Kavukcuoglu', 'Google DeepMind (United Kingdom)', NULL),
    ('Trevor Darrell', 'Berkeley College', NULL),
    ('Andrew Cowie', 'Google DeepMind (United Kingdom)', NULL),
    ('Stig Petersen', 'Google DeepMind (United Kingdom)', NULL),
    ('Kilian Q. Weinberger', 'Cornell University', NULL),
    ('Stanislav Nikolov', 'Google DeepMind (United Kingdom)', NULL),
    ('Christian Szegedy', 'Google (United States)', NULL),
    ('Simon Kohl', 'Google DeepMind (United Kingdom)', NULL),
    ('Kaiming He', 'Microsoft Research (United Kingdom)', NULL),
    ('Richard Socher', '', NULL),
    ('C. Lawrence Zitnick', 'Microsoft (United States)', NULL),
    ('Sebastian W. Bodenstein', 'Google DeepMind (United Kingdom)', NULL),
    ('Yann LeCun', 'Meta (United States)', NULL),
    ('Bernardino Romera-Paredes', 'Google DeepMind (United Kingdom)', NULL),
    ('Shaoqing Ren', 'Microsoft Research (United Kingdom)', NULL),
    ('David Silver', 'Google DeepMind (United Kingdom)', NULL),
    ('Michal Zielinski', 'Google DeepMind (United Kingdom)', NULL),
    ('Yangqing Jia', 'IGlobal University', NULL),
    ('Andrew F. Hayes', 'The Ohio State University', NULL),
    ('Olaf Ronneberger', 'University of Freiburg', NULL),
    ('Trevor Back', 'Google DeepMind (United Kingdom)', NULL),
    ('Andrew Senior', 'Google DeepMind (United Kingdom)', NULL),
    ('Martin Steinegger', 'Seoul National University', NULL),
    ('Li Fei-Fei', 'Stanford University', NULL);

-- ============================================================
-- KEYWORDS
-- ============================================================
INSERT INTO keywords (keyword_name) VALUES
    ('deep learning'),
    ('computer vision'),
    ('transformer'),
    ('image recognition'),
    ('object detection'),
    ('protein structure prediction'),
    ('bioinformatics'),
    ('optimization'),
    ('word embedding'),
    ('semantic segmentation'),
    ('evolutionary genetics'),
    ('benchmark dataset');

-- ============================================================
-- PAPERS
-- ============================================================
INSERT INTO papers (
    journal_id,
    field_id,
    api_source_id,
    publication_type,
    title,
    abstract,
    publication_year,
    doi,
    source_url,
    citation_count,
    visibility_status
) VALUES
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Deep Residual Learning for Image Recognition', NULL, 2016, '10.1109/cvpr.2016.90', 'https://doi.org/10.1109/cvpr.2016.90', 220052, 'VISIBLE'),
    (NULL, 1, 1, 'BOOK_CHAPTER', 'U-Net: Convolutional Networks for Biomedical Image Segmentation', NULL, 2015, '10.1007/978-3-319-24574-4_28', 'https://doi.org/10.1007/978-3-319-24574-4_28', 87895, 'VISIBLE'),
    (2, 1, 1, 'JOURNAL_ARTICLE', 'Deep learning', NULL, 2015, '10.1038/nature14539', 'https://doi.org/10.1038/nature14539', 80583, 'VISIBLE'),
    (NULL, 1, 1, 'REPOSITORY_ITEM', 'MizAR 60 for Mizar 50', NULL, 2023, '10.4230/lipics.itp.2023.19', 'https://drops.dagstuhl.de/entities/document/10.4230/LIPIcs.ITP.2023.19', 75682, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'ImageNet classification with deep convolutional neural networks', NULL, 2017, '10.1145/3065386', 'https://doi.org/10.1145/3065386', 75677, 'VISIBLE'),
    (NULL, 1, 1, 'PREPRINT', 'Very Deep Convolutional Networks for Large-Scale Image Recognition', NULL, 2014, '10.48550/arxiv.1409.1556', 'http://arxiv.org/abs/1409.1556', 75505, 'VISIBLE'),
    (6, 1, 1, 'JOURNAL_ARTICLE', 'Faster R-CNN: Towards Real-Time Object Detection with Region Proposal Networks', NULL, 2016, '10.1109/tpami.2016.2577031', 'https://doi.org/10.1109/tpami.2016.2577031', 53659, 'VISIBLE'),
    (7, 2, 1, 'JOURNAL_ARTICLE', 'MEGA6: Molecular Evolutionary Genetics Analysis Version 6.0', NULL, 2013, '10.1093/molbev/mst197', 'https://doi.org/10.1093/molbev/mst197', 47813, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'XGBoost', NULL, 2016, '10.1145/2939672.2939785', 'https://doi.org/10.1145/2939672.2939785', 47080, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Going deeper with convolutions', NULL, 2015, '10.1109/cvpr.2015.7298594', 'https://doi.org/10.1109/cvpr.2015.7298594', 46648, 'VISIBLE'),
    (NULL, 1, 1, 'REPOSITORY_ITEM', 'AI-Assisted Pipeline for Dynamic Generation of Trustworthy Health Supplement Content at Scale', NULL, 2018, '10.4230/lipics.cosit.2022.18', 'https://drops.dagstuhl.de/entities/document/10.4230/OASIcs.LDK.2019.21', 45559, 'VISIBLE'),
    (NULL, 1, 1, 'OTHER', 'Introduction to Mediation, Moderation, and Conditional Process Analysis: A Regression-Based Approach', NULL, 2013, NULL, 'http://bvbr.bib-bvb.de:8991/F?func=service&doc_library=BVB01&local_base=BVB01&doc_number=025778167&sequence=000003&line_number=0001&func_code=DB_RECORDS&service_type=MEDIA', 45131, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Densely Connected Convolutional Networks', NULL, 2017, '10.1109/cvpr.2017.243', 'https://doi.org/10.1109/cvpr.2017.243', 44228, 'VISIBLE'),
    (2, 2, 1, 'JOURNAL_ARTICLE', 'Highly accurate protein structure prediction with AlphaFold', NULL, 2021, '10.1038/s41586-021-03819-2', 'https://doi.org/10.1038/s41586-021-03819-2', 44128, 'VISIBLE'),
    (NULL, 1, 1, 'BOOK_CHAPTER', 'Microsoft COCO: Common Objects in Context', NULL, 2014, '10.1007/978-3-319-10602-1_48', 'https://doi.org/10.1007/978-3-319-10602-1_48', 41868, 'VISIBLE'),
    (8, 1, 1, 'JOURNAL_ARTICLE', 'ImageNet Large Scale Visual Recognition Challenge', NULL, 2015, '10.1007/s11263-015-0816-y', 'https://doi.org/10.1007/s11263-015-0816-y', 40009, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Fully convolutional networks for semantic segmentation', NULL, 2015, '10.1109/cvpr.2015.7298965', 'https://doi.org/10.1109/cvpr.2015.7298965', 36709, 'VISIBLE'),
    (NULL, 1, 1, 'OTHER', 'Dropout: a simple way to prevent neural networks from overfitting', NULL, 2014, NULL, 'http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.669.8604', 34247, 'VISIBLE'),
    (9, 2, 1, 'JOURNAL_ARTICLE', 'RAxML version 8: a tool for phylogenetic analysis and post-analysis of large phylogenies', NULL, 2014, '10.1093/bioinformatics/btu033', 'https://doi.org/10.1093/bioinformatics/btu033', 34084, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'GloVe: Global Vectors for Word Representation', NULL, 2014, '10.3115/v1/d14-1162', 'https://doi.org/10.3115/v1/d14-1162', 33579, 'VISIBLE');

-- ============================================================
-- PAPER AUTHORS
-- ============================================================
INSERT INTO paper_authors (paper_id, author_id, author_order) VALUES
    (1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4),
    (2, 5, 1), (2, 6, 2), (2, 7, 3),
    (3, 8, 1), (3, 9, 2), (3, 10, 3),
    (5, 11, 1), (5, 12, 2), (5, 10, 3),
    (6, 13, 1), (6, 14, 2),
    (7, 3, 1), (7, 1, 2), (7, 15, 3), (7, 16, 4),
    (8, 17, 1), (8, 18, 2), (8, 19, 3), (8, 20, 4), (8, 21, 5),
    (10, 22, 1), (10, 23, 2), (10, 24, 3), (10, 25, 4), (10, 26, 5),
    (12, 31, 1),
    (13, 32, 1), (13, 33, 2), (13, 34, 3), (13, 35, 4),
    (14, 36, 1), (14, 37, 2), (14, 38, 3), (14, 39, 4), (14, 5, 5),
    (15, 68, 1), (15, 69, 2), (15, 70, 3), (15, 71, 4), (15, 72, 5),
    (16, 76, 1), (16, 77, 2), (16, 78, 3), (16, 79, 4), (16, 80, 5),
    (17, 88, 1), (17, 89, 2), (17, 90, 3),
    (18, 91, 1), (18, 10, 2), (18, 11, 3), (18, 12, 4), (18, 92, 5),
    (19, 93, 1),
    (20, 94, 1), (20, 95, 2), (20, 96, 3);

-- ============================================================
-- PAPER KEYWORDS
-- ============================================================
INSERT INTO paper_keywords (paper_id, keyword_id) VALUES
    (1, 1), (1, 4),
    (2, 10), (2, 7),
    (3, 1),
    (5, 1), (5, 4), (5, 12),
    (6, 1), (6, 4),
    (7, 5), (7, 4),
    (8, 7), (8, 11),
    (9, 8),
    (10, 1), (10, 4),
    (13, 1), (13, 4),
    (14, 6), (14, 7),
    (15, 12), (15, 4),
    (16, 4), (16, 12),
    (17, 10), (17, 4),
    (18, 8), (18, 1),
    (19, 7), (19, 11),
    (20, 9);

-- ============================================================
-- TOPICS
-- ============================================================
INSERT INTO topics (topic_name, description) VALUES
    ('Artificial Intelligence', 'Research related to machine learning, neural networks, and reasoning.'),
    ('Computer Vision', 'Research related to image classification, object detection, and segmentation.'),
    ('Natural Language Processing', 'Research related to text classification, translation, and language modeling.'),
    ('Bioinformatics', 'Research related to sequence analysis and biological computing.');

-- ============================================================
-- PAPER TOPICS
-- ============================================================
INSERT INTO paper_topics (paper_id, topic_id) VALUES
    (1, 1), (1, 2),
    (2, 2), (2, 4),
    (3, 1),
    (4, 1),
    (5, 1), (5, 2),
    (6, 1),
    (7, 1),
    (8, 1),
    (9, 1),
    (10, 1),
    (11, 2),
    (12, 1),
    (13, 1), (13, 2),
    (14, 4),
    (15, 2),
    (16, 2),
    (17, 2),
    (18, 1),
    (19, 4),
    (20, 3);

-- ============================================================
-- ABSTRACT UPDATES FROM OPENALEX / PUBLIC SOURCES
-- ============================================================
UPDATE papers SET abstract = 'Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training of networks that are substantially deeper than those used previously. We explicitly reformulate the layers as learning residual functions with reference to the layer inputs, instead of learning unreferenced functions. We provide comprehensive empirical evidence showing that these residual networks are easier to optimize, and can gain accuracy from considerably increased depth. On the ImageNet dataset we evaluate residual nets with a depth of up to 152 layers - 8x deeper than VGG nets but still having lower complexity. An ensemble of these residual nets achieves 3.57% error on the ImageNet test set. This result won the 1st place on the ILSVRC 2015 classification task. We also present analysis on CIFAR-10 with 100 and 1000 layers. The depth of representations is of central importance for many visual recognition tasks. Solely due to our extremely deep representations, we obtain a 28% relative improvement on the COCO object detection dataset.' WHERE paper_id = 1;
UPDATE papers SET abstract = 'There is large consent that successful training of deep networks requires many thousand annotated training samples. In this paper, we present a network and training strategy that relies on the strong use of data augmentation to use the available annotated samples more efficiently. The architecture consists of a contracting path to capture context and a symmetric expanding path that enables precise localization. We show that such a network can be trained end-to-end from very few images and outperforms the prior best method on the ISBI challenge for segmentation of neuronal structures in electron microscopic stacks. Using the same network trained on transmitted light microscopy images we won the ISBI cell tracking challenge 2015 by a large margin. Moreover, the network is fast. Segmentation of a 512x512 image takes less than a second on a recent GPU.' WHERE paper_id = 2;
UPDATE papers SET abstract = 'Deep learning allows computational models that are composed of multiple processing layers to learn representations of data with multiple levels of abstraction. These methods have dramatically improved the state of the art in speech recognition, visual object recognition, object detection and many other domains such as drug discovery and genomics. Deep learning discovers intricate structure in large data sets by using backpropagation to indicate how a machine should change its internal parameters that are used to compute the representation in each layer from the representation in the previous layer. Deep convolutional nets have brought about breakthroughs in processing images, video, speech and audio, whereas recurrent nets have shone light on sequential data such as text and speech.' WHERE paper_id = 3;
UPDATE papers SET abstract = 'As a present to Mizar on its 50th anniversary, we develop an AI and theorem proving system that automatically proves about 60 percent of the Mizar theorems in the hammer setting. We also automatically prove 75 percent of the Mizar theorems when the automated provers are helped by using only the premises used in the human written Mizar proofs. We describe the methods and large scale experiments leading to these results. This includes in particular the E and Vampire provers, their ENIGMA and Deepire learning modifications, a number of learning based premise selection methods, and the incremental loop that interleaves growing a corpus of millions of ATP proofs with training increasingly strong AI and theorem proving systems on them.' WHERE paper_id = 4;
UPDATE papers SET abstract = 'We trained a large, deep convolutional neural network to classify the 1.2 million high resolution images in the ImageNet contest into 1000 different classes. On the test data, we achieved top-1 and top-5 error rates of 37.5 percent and 17.0 percent, which is considerably better than the previous state of the art. The neural network, which has 60 million parameters and 650,000 neurons, consists of five convolutional layers and three fully connected layers with a final 1000-way softmax. To make training faster, we used non saturating neurons and a very efficient GPU implementation of the convolution operation. To reduce overfitting in the fully connected layers we employed dropout.' WHERE paper_id = 5;
UPDATE papers SET abstract = 'In this work we investigate the effect of the convolutional network depth on its accuracy in the large scale image recognition setting. Our main contribution is a thorough evaluation of networks of increasing depth using an architecture with very small 3x3 convolution filters, which shows that a significant improvement on prior configurations can be achieved by pushing the depth to 16 to 19 weight layers. These findings were the basis of our ImageNet Challenge 2014 submission, where our team secured the first and second places in the localisation and classification tracks respectively. We also show that our representations generalise well to other datasets, where they achieve state of the art results.' WHERE paper_id = 6;
UPDATE papers SET abstract = 'State of the art object detection networks depend on region proposal algorithms to hypothesize object locations. Advances like SPPnet and Fast R-CNN have reduced the running time of these detection networks, exposing region proposal computation as a bottleneck. In this work, we introduce a Region Proposal Network that shares full image convolutional features with the detection network, thus enabling nearly cost free region proposals. An RPN is a fully convolutional network that simultaneously predicts object bounds and objectness scores at each position. We further merge RPN and Fast R-CNN into a single network by sharing their convolutional features.' WHERE paper_id = 7;
UPDATE papers SET abstract = 'The Molecular Evolutionary Genetics Analysis software has matured to contain a large collection of methods and tools of computational molecular evolution. Here, we describe new additions that make MEGA a more comprehensive tool for building timetrees of species, pathogens, and gene families using rapid relaxed clock methods. Methods for estimating divergence times and confidence intervals are implemented to use probability densities for calibration constraints for node dating and sequence sampling dates for tip dating analyses. These enhancements improve the user experience, quality of results, and the pace of biological discovery.' WHERE paper_id = 8;
UPDATE papers SET abstract = 'Tree boosting is a highly effective and widely used machine learning method. In this paper, we describe a scalable end to end tree boosting system called XGBoost, which is used widely by data scientists to achieve state of the art results on many machine learning challenges. We propose a novel sparsity aware algorithm for sparse data and weighted quantile sketch for approximate tree learning. More importantly, we provide insights on cache access patterns, data compression and sharding to build a scalable tree boosting system. By combining these insights, XGBoost scales beyond billions of examples using far fewer resources than existing systems.' WHERE paper_id = 9;
UPDATE papers SET abstract = 'We propose a deep convolutional neural network architecture codenamed Inception that achieves the new state of the art for classification and detection in the ImageNet Large Scale Visual Recognition Challenge 2014. The hallmark of this architecture is the improved utilization of the computing resources inside the network. By a carefully crafted design, we increased the depth and width of the network while keeping the computational budget constant. One particular incarnation used in our submission for ILSVRC14 is called GoogLeNet, a 22 layer deep network whose quality is assessed in the context of classification and detection.' WHERE paper_id = 10;
UPDATE papers SET abstract = 'Although geospatial question answering systems have received increasing attention in recent years, existing prototype systems struggle to properly answer qualitative spatial questions. In this work, we propose a framework for answering qualitative spatial questions, comprising a geoparser that extracts place semantic information from text, a reasoning system with a crisp reasoner, and answer extraction that refines the solution space and generates final answers. We present an experimental design to evaluate the framework for point based cardinal direction calculus relations using synthetic qualitative spatial questions.' WHERE paper_id = 11;
UPDATE papers SET abstract = 'This work presents the concepts and methods behind mediation, moderation, and conditional process analysis using a regression based approach. It introduces simple and multiple regression, direct and indirect effects, moderated relationships, conditional effects, and practical guidance for analyzing and reporting conditional process models with statistical software.' WHERE paper_id = 12;
UPDATE papers SET abstract = 'Recent work has shown that convolutional networks can be substantially deeper, more accurate, and efficient to train if they contain shorter connections between layers close to the input and those close to the output. In this paper, we introduce the Dense Convolutional Network, which connects each layer to every other layer in a feed forward fashion. DenseNets alleviate the vanishing gradient problem, strengthen feature propagation, encourage feature reuse, and substantially reduce the number of parameters. We evaluate the architecture on CIFAR-10, CIFAR-100, SVHN, and ImageNet.' WHERE paper_id = 13;
UPDATE papers SET abstract = 'Proteins are essential to life, and understanding their structure can facilitate a mechanistic understanding of their function. Through an enormous experimental effort, the structures of around 100,000 unique proteins have been determined, but this represents a small fraction of the billions of known protein sequences. Here we provide a computational method that can regularly predict protein structures with atomic accuracy even in cases in which no similar structure is known. We validated a redesigned version of AlphaFold in CASP14, demonstrating accuracy competitive with experimental structures in a majority of cases and greatly outperforming other methods.' WHERE paper_id = 14;
UPDATE papers SET abstract = 'We present a dataset with the goal of advancing the state of the art in object recognition by placing the question of object recognition in the broader context of scene understanding. This is achieved by gathering images of complex everyday scenes containing common objects in their natural context. Objects are labeled using per instance segmentations to aid in precise object localization. The dataset contains 91 object types with 2.5 million labeled instances in 328 thousand images. We provide baseline performance analysis for bounding box and segmentation detection results using a Deformable Parts Model.' WHERE paper_id = 15;
UPDATE papers SET abstract = 'The ImageNet Large Scale Visual Recognition Challenge is a benchmark in object category classification and detection on hundreds of object categories and millions of images. The challenge has been run annually from 2010 onward, attracting participation from more than fifty institutions. This paper describes the creation of this benchmark dataset and the advances in object recognition that have been possible as a result. We discuss the challenges of collecting large scale ground truth annotation, highlight key breakthroughs in categorical object recognition, and compare computer vision accuracy with human accuracy.' WHERE paper_id = 16;
UPDATE papers SET abstract = 'Convolutional networks are powerful visual models that yield hierarchies of features. We show that convolutional networks by themselves, trained end to end and pixels to pixels, exceed the state of the art in semantic segmentation. Our key insight is to build fully convolutional networks that take input of arbitrary size and produce correspondingly sized output with efficient inference and learning. We adapt contemporary classification networks into fully convolutional networks and define a skip architecture that combines semantic information from a deep coarse layer with appearance information from a shallow fine layer to produce accurate and detailed segmentations.' WHERE paper_id = 17;
UPDATE papers SET abstract = 'Deep neural nets with a large number of parameters are very powerful machine learning systems. However, overfitting is a serious problem in such networks. Dropout is a technique for addressing this problem by randomly dropping units along with their connections from the neural network during training. This prevents units from co adapting too much. At test time, it is easy to approximate the effect of averaging the predictions of many thinned networks by using a single unthinned network that has smaller weights. This significantly reduces overfitting and gives major improvements over other regularization methods.' WHERE paper_id = 18;
UPDATE papers SET abstract = 'Phylogenies are increasingly used in all fields of medical and biological research. Because of next generation sequencing, datasets used for phylogenetic analyses grow at an unprecedented pace. RAxML is a popular program for phylogenetic analyses of large datasets under maximum likelihood. Since the last RAxML paper in 2006, it has been continuously maintained and extended to accommodate growing input datasets and to serve the needs of the user community. The work describes notable new features and extensions of RAxML, including support for more data types, improved memory usage, and post analysis operations on sets of trees.' WHERE paper_id = 19;
UPDATE papers SET abstract = 'Recent methods for learning vector space representations of words have succeeded in capturing fine grained semantic and syntactic regularities using vector arithmetic, but the origin of these regularities has remained opaque. We analyze and make explicit the model properties needed for such regularities to emerge in word vectors. The result is a new global log bilinear regression model that combines the advantages of global matrix factorization and local context window methods. The model efficiently leverages statistical information by training only on the nonzero elements in a word word co occurrence matrix.' WHERE paper_id = 20;

-- ============================================================
-- DATA CONSISTENCY CLEANUP
-- journal_id should be present only for journal_article
-- ============================================================
UPDATE papers
SET journal_id = NULL
WHERE publication_type IN (
    'CONFERENCE_PAPER',
    'PREPRINT',
    'BOOK_CHAPTER',
    'REPOSITORY_ITEM',
    'OTHER'
);
-- ============================================================


USE [master]
GO
ALTER DATABASE [PublicationTracker] SET  READ_WRITE 
GO
