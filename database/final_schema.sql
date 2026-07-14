USE [master]
GO
/****** Object:  Database [PublicationTracker]    Script Date: 07/07/2026 11:24:01 ******/
CREATE DATABASE [PublicationTracker]
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
ALTER DATABASE [PublicationTracker] SET QUERY_STORE = ON
GO
ALTER DATABASE [PublicationTracker] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [PublicationTracker]
GO
/****** Object:  Table [dbo].[authors]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[papers]    Script Date: 07/07/2026 11:24:02 ******/
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
	[is_open_access] [bit] NULL,
PRIMARY KEY CLUSTERED 
(
	[paper_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[paper_authors]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  View [dbo].[v_papers_detail]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[api_sources]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[bookmark_folders]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[bookmark_folders](
	[folder_id] [bigint] IDENTITY(1,1) NOT NULL,
	[created_at] [datetime2](6) NULL,
	[folder_name] [varchar](255) NOT NULL,
	[updated_at] [datetime2](6) NULL,
	[user_id] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[folder_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[bookmark_papers]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[bookmark_papers](
	[bookmark_id] [bigint] IDENTITY(1,1) NOT NULL,
	[note] [varchar](255) NULL,
	[paper_id] [bigint] NOT NULL,
	[saved_at] [datetime2](6) NULL,
	[folder_id] [bigint] NULL,
	[user_id] [bigint] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[bookmark_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[follow_authors]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[follow_authors](
	[follow_id] [bigint] IDENTITY(1,1) NOT NULL,
	[author_id] [bigint] NOT NULL,
	[followed_at] [datetime2](6) NULL,
	[user_id] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[follow_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[follow_journals]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[follow_journals](
	[follow_id] [bigint] IDENTITY(1,1) NOT NULL,
	[followed_at] [datetime2](6) NULL,
	[journal_id] [int] NULL,
	[user_id] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[follow_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[follow_topics]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[follow_topics](
	[follow_id] [bigint] IDENTITY(1,1) NOT NULL,
	[followed_at] [datetime2](6) NULL,
	[topic_id] [int] NULL,
	[user_id] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[follow_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[journals]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[keywords]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[notifications]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[notifications](
	[notification_id] [bigint] IDENTITY(1,1) NOT NULL,
	[created_at] [datetime2](6) NULL,
	[is_read] [bit] NULL,
	[message] [varchar](255) NOT NULL,
	[title] [varchar](255) NOT NULL,
	[user_id] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[notification_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[paper_keywords]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[paper_topics]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[password_reset_tokens]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[password_reset_tokens](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [expiry_time] [datetime2](6) NOT NULL,
    [otp_code] [varchar](255) NULL,
    [user_id] [bigint] NULL,

    CONSTRAINT [PK_PASSWORD_RESET_TOKENS]
        PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[pending_registrations]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[report_tickets]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[report_tickets](
	[report_id] [bigint] IDENTITY(1,1) NOT NULL,
	[created_at] [datetime2](6) NULL,
	[reason] [varchar](255) NOT NULL,
	[paper_id] [bigint] NULL,
	[user_id] [bigint] NULL,
	[status] [varchar](255) NULL,
	[admin_response] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[report_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[research_fields]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[roles]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[search_history]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[sync_jobs]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[sync_jobs](
	[sync_job_id] [bigint] IDENTITY(1,1) NOT NULL,
	[source_id] [int] NOT NULL,
	[triggered_by] [bigint] NULL,
	[status] [varchar](50) NOT NULL,
	[added_count] [int] NOT NULL,
	[updated_count] [int] NOT NULL,
	[error_message] [varchar](max) NULL,
	[started_at] [datetime2](7) NOT NULL,
	[finished_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED 
(
	[sync_job_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[topics]    Script Date: 07/07/2026 11:24:02 ******/
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
/****** Object:  Table [dbo].[users]    Script Date: 07/07/2026 11:24:02 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[users](
	[user_id] [bigint] IDENTITY(1,1) NOT NULL,
	[full_name] [nvarchar](255) NOT NULL,
	[email] [varchar](255) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[role_id] [int] NOT NULL,
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
SET ANSI_PADDING ON
GO
/****** Object:  Index [uq_authors_orcid]    Script Date: 07/07/2026 11:24:02 ******/
CREATE UNIQUE NONCLUSTERED INDEX [uq_authors_orcid] ON [dbo].[authors]
(
	[orcid] ASC
)
WHERE ([orcid] IS NOT NULL AND [deleted_at] IS NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_citation]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_citation] ON [dbo].[papers]
(
	[citation_count] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_field_id]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_field_id] ON [dbo].[papers]
(
	[field_id] ASC
)
WHERE ([field_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_journal_id]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_journal_id] ON [dbo].[papers]
(
	[journal_id] ASC
)
WHERE ([journal_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_source_id]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_source_id] ON [dbo].[papers]
(
	[api_source_id] ASC
)
WHERE ([api_source_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [idx_papers_type]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_type] ON [dbo].[papers]
(
	[publication_type] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [idx_papers_visibility]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_visibility] ON [dbo].[papers]
(
	[visibility_status] ASC
)
WHERE ([deleted_at] IS NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_papers_year]    Script Date: 07/07/2026 11:24:02 ******/
CREATE NONCLUSTERED INDEX [idx_papers_year] ON [dbo].[papers]
(
	[publication_year] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [uq_papers_doi]    Script Date: 07/07/2026 11:24:02 ******/
CREATE UNIQUE NONCLUSTERED INDEX [uq_papers_doi] ON [dbo].[papers]
(
	[doi] ASC
)
WHERE ([doi] IS NOT NULL AND [deleted_at] IS NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [idx_sh_user_time]    Script Date: 07/07/2026 11:24:02 ******/
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
ALTER TABLE [dbo].[sync_jobs] ADD  DEFAULT ((0)) FOR [added_count]
GO
ALTER TABLE [dbo].[sync_jobs] ADD  DEFAULT ((0)) FOR [updated_count]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT ('ACTIVE') FOR [status]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[bookmark_folders]  WITH CHECK ADD  CONSTRAINT [FKp19wjyaxkkhsgu2ec0btsh9uw] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[bookmark_folders] CHECK CONSTRAINT [FKp19wjyaxkkhsgu2ec0btsh9uw]
GO
ALTER TABLE [dbo].[bookmark_papers]  WITH CHECK ADD  CONSTRAINT [FK5o3gj3ufsowsf5qcylb94mgv0] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[bookmark_papers] CHECK CONSTRAINT [FK5o3gj3ufsowsf5qcylb94mgv0]
GO
ALTER TABLE [dbo].[bookmark_papers]  WITH CHECK ADD  CONSTRAINT [FK9vistc2atyh72hoi76m34a131] FOREIGN KEY([folder_id])
REFERENCES [dbo].[bookmark_folders] ([folder_id])
GO
ALTER TABLE [dbo].[bookmark_papers] CHECK CONSTRAINT [FK9vistc2atyh72hoi76m34a131]
GO
ALTER TABLE [dbo].[follow_authors]  WITH CHECK ADD  CONSTRAINT [FKldibaa3ao1rpdnq5chug616ly] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[follow_authors] CHECK CONSTRAINT [FKldibaa3ao1rpdnq5chug616ly]
GO
ALTER TABLE [dbo].[follow_authors]  WITH CHECK ADD  CONSTRAINT [FKo3g7g3jf2jpowui6w3v0raxd6] FOREIGN KEY([author_id])
REFERENCES [dbo].[authors] ([author_id])
GO
ALTER TABLE [dbo].[follow_authors] CHECK CONSTRAINT [FKo3g7g3jf2jpowui6w3v0raxd6]
GO
ALTER TABLE [dbo].[follow_journals]  WITH CHECK ADD  CONSTRAINT [FKfovhj79ofsg6m1pocauktlvcl] FOREIGN KEY([journal_id])
REFERENCES [dbo].[journals] ([journal_id])
GO
ALTER TABLE [dbo].[follow_journals] CHECK CONSTRAINT [FKfovhj79ofsg6m1pocauktlvcl]
GO
ALTER TABLE [dbo].[follow_journals]  WITH CHECK ADD  CONSTRAINT [FKi109h1n5b80vxtu18ayvcefj] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[follow_journals] CHECK CONSTRAINT [FKi109h1n5b80vxtu18ayvcefj]
GO
ALTER TABLE [dbo].[follow_topics]  WITH CHECK ADD  CONSTRAINT [FKndlcsa3e5ife57yty1uf8ysyn] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[follow_topics] CHECK CONSTRAINT [FKndlcsa3e5ife57yty1uf8ysyn]
GO
ALTER TABLE [dbo].[follow_topics]  WITH CHECK ADD  CONSTRAINT [FKtbq1a20uhfxe52o4beba22trw] FOREIGN KEY([topic_id])
REFERENCES [dbo].[topics] ([topic_id])
GO
ALTER TABLE [dbo].[follow_topics] CHECK CONSTRAINT [FKtbq1a20uhfxe52o4beba22trw]
GO
ALTER TABLE [dbo].[notifications]  WITH CHECK ADD  CONSTRAINT [FK9y21adhxn0ayjhfocscqox7bh] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[notifications] CHECK CONSTRAINT [FK9y21adhxn0ayjhfocscqox7bh]
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
ALTER TABLE [dbo].[password_reset_tokens]
WITH CHECK ADD CONSTRAINT [FK_PASSWORD_RESET_USER]
FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO

ALTER TABLE [dbo].[password_reset_tokens]
CHECK CONSTRAINT [FK_PASSWORD_RESET_USER]
GO
ALTER TABLE [dbo].[report_tickets]  WITH CHECK ADD  CONSTRAINT [FKq5tgviivradu4hvi2dyok4mkc] FOREIGN KEY([paper_id])
REFERENCES [dbo].[papers] ([paper_id])
GO
ALTER TABLE [dbo].[report_tickets] CHECK CONSTRAINT [FKq5tgviivradu4hvi2dyok4mkc]
GO
ALTER TABLE [dbo].[report_tickets]  WITH CHECK ADD  CONSTRAINT [FKrqucywc88liyfmop01laofdpu] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[report_tickets] CHECK CONSTRAINT [FKrqucywc88liyfmop01laofdpu]
GO
ALTER TABLE [dbo].[search_history]  WITH CHECK ADD  CONSTRAINT [fk_sh_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[search_history] CHECK CONSTRAINT [fk_sh_user]
GO
ALTER TABLE [dbo].[sync_jobs]  WITH CHECK ADD  CONSTRAINT [FK_sync_jobs_source] FOREIGN KEY([source_id])
REFERENCES [dbo].[api_sources] ([source_id])
GO
ALTER TABLE [dbo].[sync_jobs] CHECK CONSTRAINT [FK_sync_jobs_source]
GO
ALTER TABLE [dbo].[sync_jobs]  WITH CHECK ADD  CONSTRAINT [FK_sync_jobs_user] FOREIGN KEY([triggered_by])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[sync_jobs] CHECK CONSTRAINT [FK_sync_jobs_user]
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[roles] ([role_id])
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[roles] ([role_id])
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[roles] ([role_id])
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[roles] ([role_id])
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

-- =========================================
-- MERGED FROM thuan.sql
-- =========================================
CREATE TABLE [dbo].[discounts](
	[discount_id] [bigint] IDENTITY(1,1) NOT NULL,
	[discount_name] [nvarchar](255) NOT NULL,
	[discount_percent] [float] NULL,
	[from_date] [datetime] NOT NULL,
	[to_date] [datetime] NOT NULL,
	[is_active] [bit] NOT NULL,
	[created_at] [datetime] NOT NULL,
	[updated_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED 
(
	[discount_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

CREATE TABLE [dbo].[premiums](
	[premium_id] [bigint] IDENTITY(1,1) NOT NULL,
	[package_name] [varchar](255) NULL,
	[amount] [numeric](38, 2) NULL,
	[duration_days] [int] NOT NULL,
	[description] [varchar](255) NULL,
	[is_active] [bit] NOT NULL,
	[created_at] [datetime] NOT NULL,
	[updated_at] [datetime] NOT NULL,
	[discount_id] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[premium_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[package_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

CREATE TABLE [dbo].[invoices](
	[invoice_id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[premium_id] [bigint] NOT NULL,
	[discount_id] [bigint] NULL,
	[original_amount] [numeric](38, 2) NULL,
	[discount_amount] [numeric](38, 2) NULL,
	[final_amount] [numeric](38, 2) NULL,
	[status] [varchar](255) NULL,
	[created_at] [datetime] NOT NULL,
	[discount_percent] [decimal](5, 2) NOT NULL,
	[package_name] [nvarchar](255) NOT NULL,
	[duration_days] [int] NOT NULL,
	[paid_at] [datetime2](7) NULL,
	[order_code] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[invoice_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

CREATE TABLE [dbo].[user_subscriptions](
	[subscription_id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[premium_id] [bigint] NOT NULL,
	[start_date] [datetime] NOT NULL,
	[end_date] [datetime] NOT NULL,
	[status] [varchar](255) NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[subscription_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- DEFAULT VALUES
ALTER TABLE [dbo].[discounts] ADD DEFAULT ((1)) FOR [is_active]
GO

ALTER TABLE [dbo].[discounts] ADD DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[invoices] ADD DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[invoices] ADD CONSTRAINT [DF_invoice_discount_percent] DEFAULT ((0)) FOR [discount_percent]
GO

ALTER TABLE [dbo].[invoices] ADD CONSTRAINT [DF_invoice_package_name] DEFAULT ('') FOR [package_name]
GO

ALTER TABLE [dbo].[invoices] ADD CONSTRAINT [DF_invoice_duration_days] DEFAULT ((0)) FOR [duration_days]
GO

ALTER TABLE [dbo].[premiums] ADD DEFAULT ((1)) FOR [is_active]
GO

ALTER TABLE [dbo].[premiums] ADD DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[premiums] ADD DEFAULT (getdate()) FOR [updated_at]
GO

ALTER TABLE [dbo].[user_subscriptions] ADD DEFAULT (getdate()) FOR [created_at]
GO

-- FOREIGN KEYS
ALTER TABLE [dbo].[invoices] WITH CHECK ADD CONSTRAINT [FK_INVOICE_USER]
FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[invoices] WITH CHECK ADD CONSTRAINT [FK_INVOICE_PREMIUM]
FOREIGN KEY([premium_id]) REFERENCES [dbo].[premiums] ([premium_id])
GO
ALTER TABLE [dbo].[invoices] WITH CHECK ADD CONSTRAINT [FK_INVOICE_DISCOUNT]
FOREIGN KEY([discount_id]) REFERENCES [dbo].[discounts] ([discount_id])
GO
ALTER TABLE [dbo].[premiums] WITH CHECK ADD CONSTRAINT [FK_PREMIUM_DISCOUNT]
FOREIGN KEY([discount_id]) REFERENCES [dbo].[discounts] ([discount_id])
GO
ALTER TABLE [dbo].[user_subscriptions] WITH CHECK ADD CONSTRAINT [FK_SUB_USER]
FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[user_subscriptions] WITH CHECK ADD CONSTRAINT [FK_SUB_PREMIUM]
FOREIGN KEY([premium_id]) REFERENCES [dbo].[premiums] ([premium_id])
GO

-- BASIC AUTHENTICATION ROLES
IF NOT EXISTS (SELECT 1 FROM [dbo].[roles] WHERE [role_name] = 'MEMBER')
    INSERT INTO [dbo].[roles] ([role_name]) VALUES ('MEMBER');
GO

IF NOT EXISTS (SELECT 1 FROM [dbo].[roles] WHERE [role_name] = 'ADMIN')
    INSERT INTO [dbo].[roles] ([role_name]) VALUES ('ADMIN');
GO

USE [master]
GO
ALTER DATABASE [PublicationTracker] SET  READ_WRITE 
GO
