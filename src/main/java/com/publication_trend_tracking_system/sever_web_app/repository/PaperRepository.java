package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    boolean existsByDoi(String doi);
    long countByTopics_TopicId(
            Integer topicId);
    long countByAuthors_AuthorId(Long authorId);
    long countByJournal_JournalId(Integer journalId);
    long countByPublicationYear(Integer publicationYear);

    java.util.Optional<Paper> findFirstByDoiIgnoreCase(String doi);

    // Papers touched by a given sync job (Implementation Plan v3: job detail view).
    // Reflects only the most recent sync per paper, not full history.
    Page<Paper> findByLastSyncJobIdOrderByUpdatedAtDesc(Long lastSyncJobId, Pageable pageable);

    // last_sync_job_id is a plain column, not a foreign key, so deleting a job would otherwise
    // leave papers pointing at an id that no longer resolves in the job-detail view.
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Paper p SET p.lastSyncJobId = NULL WHERE p.lastSyncJobId = :jobId")
    void clearLastSyncJobId(@Param("jobId") Long jobId);

    java.util.Optional<Paper> findByDoiIgnoreCase(String doi);
    List<Paper> findAllByDoiInIgnoreCase(java.util.Set<String> dois);
    List<Paper> findAllByTitleInIgnoreCase(java.util.Set<String> titles);

    java.util.List<Paper> findByTitleIgnoreCase(String title);

    List<Paper> findTop100ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
    List<Paper> findTop100ByOrderByCreatedAtDesc();

    List<Paper> findTop10ByTopics_TopicIdOrderByCreatedAtDesc(Integer topicId);
    List<Paper> findByTopics_TopicId(Integer topicId);

    @Query(value = "SELECT TOP 10 p.* FROM papers p " +
                   "JOIN paper_topics pt ON p.paper_id = pt.paper_id " +
                   "JOIN follow_topics ft ON pt.topic_id = ft.topic_id " +
                   "WHERE ft.user_id = :userId " +
                   "AND p.paper_id NOT IN (SELECT bp.paper_id FROM bookmark_papers bp WHERE bp.user_id = :userId) " +
                   "ORDER BY NEWID()", nativeQuery = true)
    List<Paper> findRandomRecommendedPapersForUser(@Param("userId") Long userId);

    // Year-over-year, based on the paper's actual publication_year rather than when we happened
    // to sync/insert it — using created_at here would make "growth" just reflect our own sync
    // schedule instead of real publication trends.
    @Query(value = "SELECT pt.topic_id, " +
                   "SUM(CASE WHEN p.publication_year = :currentYear THEN 1 ELSE 0 END), " +
                   "SUM(CASE WHEN p.publication_year = :previousYear THEN 1 ELSE 0 END) " +
                   "FROM papers p JOIN paper_topics pt ON p.paper_id = pt.paper_id " +
                   "GROUP BY pt.topic_id", nativeQuery = true)
    List<Object[]> getTopicTrendCounts(@Param("previousYear") int previousYear,
                                       @Param("currentYear") int currentYear);

    @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse(p.publicationYear, COUNT(DISTINCT p)) " +
           "FROM Paper p " +
           "LEFT JOIN p.authors a " +
           "LEFT JOIN p.journal j " +
           "LEFT JOIN p.field f " +
           "LEFT JOIN p.topics t " +
           // LOWER() dropped for the same reason as searchPapers below: the collation is already
           // case-insensitive, so it only added a full lowercased copy of every title and abstract.
           "WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') OR p.paperAbstract LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:author IS NULL OR a.fullName LIKE CONCAT('%', :author, '%')) " +
           "AND (:journal IS NULL OR j.name LIKE CONCAT('%', :journal, '%')) " +
           "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
           "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
           "AND (:institution IS NULL OR a.affiliation LIKE CONCAT('%', :institution, '%')) " +
           "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
           "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
           "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
           "AND (:topicId IS NULL OR t.topicId = :topicId) " +
           "GROUP BY p.publicationYear " +
           "ORDER BY p.publicationYear ASC")
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse> countPapersByYearWithFilters(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("author") String author,
            @org.springframework.data.repository.query.Param("journal") String journal,
            @org.springframework.data.repository.query.Param("fromYear") Integer fromYear,
            @org.springframework.data.repository.query.Param("toYear") Integer toYear,
            @org.springframework.data.repository.query.Param("institution") String institution,
            @org.springframework.data.repository.query.Param("types") List<String> types,
            @org.springframework.data.repository.query.Param("isOpenAccess") Boolean isOpenAccess,
            @org.springframework.data.repository.query.Param("fieldId") Integer fieldId,
            @org.springframework.data.repository.query.Param("topicId") Integer topicId
    );

    @Query(value = "SELECT p.publication_year, COUNT(p.paper_id) FROM papers p WHERE p.publication_year IS NOT NULL AND CAST(p.publication_year AS VARCHAR) LIKE :search GROUP BY p.publication_year ORDER BY p.publication_year DESC", nativeQuery = true)
    java.util.List<Object[]> findDistinctYearsWithCount(@org.springframework.data.repository.query.Param("search") String search);

    @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse(k.keywordName, COUNT(p.paperId)) " +
           "FROM Paper p JOIN p.keywords k " +
           "GROUP BY k.keywordName " +
           "ORDER BY COUNT(p.paperId) DESC")
    java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse> findTopKeywords(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse(j.name, COUNT(p.paperId)) " +
           "FROM Paper p JOIN p.journal j " +
           "WHERE (:fieldId IS NULL OR p.field.fieldId = :fieldId) " +
           "GROUP BY j.name " +
           "ORDER BY COUNT(p.paperId) DESC")
    java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse> findTopJournalsByPaperCount(@Param("fieldId") Integer fieldId, org.springframework.data.domain.Pageable pageable);


    // Fix JPA count bug with DISTINCT
    // Authors and topics are both @ManyToMany (multiple per paper) — LEFT JOINing both at once,
    // as an earlier version of this query did, fans a paper with M authors and N topics out into
    // M*N intermediate rows before DISTINCT collapses them back down. That's fine when neither
    // alias is actually filtered on, but once the keyword clause needs to evaluate topicName on
    // every row (to also match papers by their assigned Topic's name, not just title/abstract),
    // the planner can no longer skip the fan-out and a single search went from milliseconds to
    // ~29s. EXISTS subqueries check membership without joining the collection into the row set,
    // so there's no fan-out regardless of how many authors/topics a paper has.
    // Two separate queries rather than one with a "search the abstract too" flag.
    //
    // Abstracts are nvarchar(MAX) and total about 58 MB across the table, so a "contains" match on
    // them costs ~700 ms versus ~38 ms for titles. The obvious approach — a single query with
    // "OR (:searchAbstract = TRUE AND abstract LIKE ...)" — is far worse than it looks: the flag is
    // a bind parameter, so the planner cannot fold it away and builds one plan that keeps the
    // abstract scan in place no matter what is passed. Measured at 104 seconds for a keyword
    // matching nothing. Splitting the queries lets each get a plan suited to the work it does, and
    // the default one never mentions the column at all.
    //
    // No LOWER() anywhere: every column searched here uses SQL_Latin1_General_CP1_CI_AS, a
    // case-insensitive collation, so LIKE already ignores case. Wrapping columns in LOWER() only
    // forced a lowercased copy of every title and abstract to be built before comparing — 1,882 ms
    // versus 696 ms for identical results.
    //
    // DISTINCT is off: the only remaining joins are journal and field, both many-to-one, so they
    // cannot duplicate a paper. It was needed back when authors and topics were joined in directly.
    //
    // The topic/author/institution predicates are uncorrelated IN subqueries rather than correlated
    // EXISTS. An EXISTS that references p re-runs per candidate paper, so a keyword matching no
    // title made SQL Server evaluate it against all ~25,000 rows: measured at 5,245 ms versus 169 ms
    // for the IN form, which resolves the matching ids once. Rare keywords were the slow ones, which
    // is the opposite of what anyone expects while demoing.
    @Query(value = "SELECT p FROM Paper p " +
             "LEFT JOIN p.journal j " +
             "LEFT JOIN p.field f " +
             "WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') " +
                                       "       OR p.paperId IN (SELECT tp.paperId FROM Paper tp JOIN tp.topics tt WHERE tt.topicName LIKE CONCAT('%', :keyword, '%'))) " +
             "AND (:author IS NULL OR p.paperId IN (SELECT ap.paperId FROM Paper ap JOIN ap.authors aa WHERE aa.fullName LIKE CONCAT('%', :author, '%'))) " +
             "AND (:journal IS NULL OR j.name LIKE CONCAT('%', :journal, '%')) " +
             "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
             "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
             "AND (:institution IS NULL OR p.paperId IN (SELECT ip.paperId FROM Paper ip JOIN ip.authors ia WHERE ia.affiliation LIKE CONCAT('%', :institution, '%'))) " +
             "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
             "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
             "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
             "AND (:topicId IS NULL OR EXISTS (SELECT 1 FROM p.topics tt WHERE tt.topicId = :topicId)) " +
             "AND (:visibility IS NULL OR p.visibilityStatus = :visibility)",
           countQuery = "SELECT COUNT(p) FROM Paper p " +
             "LEFT JOIN p.journal j " +
             "LEFT JOIN p.field f " +
             "WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') " +
                                       "       OR p.paperId IN (SELECT tp.paperId FROM Paper tp JOIN tp.topics tt WHERE tt.topicName LIKE CONCAT('%', :keyword, '%'))) " +
             "AND (:author IS NULL OR p.paperId IN (SELECT ap.paperId FROM Paper ap JOIN ap.authors aa WHERE aa.fullName LIKE CONCAT('%', :author, '%'))) " +
             "AND (:journal IS NULL OR j.name LIKE CONCAT('%', :journal, '%')) " +
             "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
             "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
             "AND (:institution IS NULL OR p.paperId IN (SELECT ip.paperId FROM Paper ip JOIN ip.authors ia WHERE ia.affiliation LIKE CONCAT('%', :institution, '%'))) " +
             "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
             "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
             "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
             "AND (:topicId IS NULL OR EXISTS (SELECT 1 FROM p.topics tt WHERE tt.topicId = :topicId)) " +
             "AND (:visibility IS NULL OR p.visibilityStatus = :visibility)")
    Page<Paper> searchPapers(
            @Param("keyword") String keyword,
            @Param("author") String author,
            @Param("journal") String journal,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            @Param("institution") String institution,
            @Param("types") List<String> types,
            @Param("isOpenAccess") Boolean isOpenAccess,
            @Param("fieldId") Integer fieldId,
            @Param("topicId") Integer topicId,
            /** VISIBLE / HIDDEN, or null for both. Callers below admin must pass VISIBLE.
             *  Typed rather than a String: comparing via CAST(... AS string) converts the
             *  column on every row and the query stops returning at all. */
            @Param("visibility") com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus visibility,
            Pageable pageable
    );


    // Keyword search runs as native SQL because the match has to be a UNION.
    //
    // As an OR — "title LIKE %kw% OR paperId IN (topic subquery)" — SQL Server evaluates the topic
    // subquery once per candidate row: 24,907 scans of paper_topics and 615,467 logical reads for a
    // query that needs 716. Rewriting the same match as a UNION of two id sets brings it to 716
    // reads and 122 ms. Neither dropping the ":kw IS NULL OR" wrapper nor making both sides IN
    // subqueries helped; only the UNION did. JPQL has no UNION, hence native.
    String KEYWORD_FILTER =
            " AND p.paper_id IN ( "
            + "   SELECT paper_id FROM papers WHERE title LIKE '%' + :keyword + '%' "
            + "   UNION "
            + "   SELECT pt.paper_id FROM paper_topics pt JOIN topics t ON t.topic_id = pt.topic_id "
            + "     WHERE t.topic_name LIKE '%' + :keyword + '%' "
            + " ) ";

    String KEYWORD_FILTER_WITH_ABSTRACT =
            " AND p.paper_id IN ( "
            + "   SELECT paper_id FROM papers WHERE title LIKE '%' + :keyword + '%' "
            + "   UNION "
            + "   SELECT paper_id FROM papers WHERE abstract LIKE '%' + :keyword + '%' "
            + "   UNION "
            + "   SELECT pt.paper_id FROM paper_topics pt JOIN topics t ON t.topic_id = pt.topic_id "
            + "     WHERE t.topic_name LIKE '%' + :keyword + '%' "
            + " ) ";

    /** The filters shared with the JPQL variants, in native form. */
    String COMMON_FILTER =
            " FROM papers p "
            + " LEFT JOIN journals j ON j.journal_id = p.journal_id "
            + " LEFT JOIN research_fields f ON f.field_id = p.field_id "
            + " WHERE (:author IS NULL OR p.paper_id IN (SELECT pa.paper_id FROM paper_authors pa "
            + "        JOIN authors a ON a.author_id = pa.author_id WHERE a.full_name LIKE '%' + :author + '%')) "
            + " AND (:journal IS NULL OR j.name LIKE '%' + :journal + '%') "
            + " AND (:fromYear IS NULL OR p.publication_year >= :fromYear) "
            + " AND (:toYear IS NULL OR p.publication_year <= :toYear) "
            + " AND (:institution IS NULL OR p.paper_id IN (SELECT pa2.paper_id FROM paper_authors pa2 "
            + "        JOIN authors a2 ON a2.author_id = pa2.author_id WHERE a2.affiliation LIKE '%' + :institution + '%')) "
            + " AND (:types IS NULL OR p.publication_type IN (:types)) "
            + " AND (:isOpenAccess IS NULL OR p.is_open_access = :isOpenAccess) "
            + " AND (:fieldId IS NULL OR f.field_id = :fieldId) "
            + " AND (:topicId IS NULL OR EXISTS (SELECT 1 FROM paper_topics pt2 WHERE pt2.paper_id = p.paper_id AND pt2.topic_id = :topicId)) "
            + " AND (:visibility IS NULL OR p.visibility_status = :visibility) ";

    @Query(value = "SELECT p.* " + COMMON_FILTER + KEYWORD_FILTER,
           countQuery = "SELECT COUNT(*) " + COMMON_FILTER + KEYWORD_FILTER,
           nativeQuery = true)
    Page<Paper> searchPapersByKeyword(
            @Param("keyword") String keyword,
            @Param("author") String author,
            @Param("journal") String journal,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            @Param("institution") String institution,
            @Param("types") List<String> types,
            @Param("isOpenAccess") Boolean isOpenAccess,
            @Param("fieldId") Integer fieldId,
            @Param("topicId") Integer topicId,
            @Param("visibility") String visibility,
            Pageable pageable
    );

    @Query(value = "SELECT p.* " + COMMON_FILTER + KEYWORD_FILTER_WITH_ABSTRACT,
           countQuery = "SELECT COUNT(*) " + COMMON_FILTER + KEYWORD_FILTER_WITH_ABSTRACT,
           nativeQuery = true)
    Page<Paper> searchPapersByKeywordIncludingAbstract(
            @Param("keyword") String keyword,
            @Param("author") String author,
            @Param("journal") String journal,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            @Param("institution") String institution,
            @Param("types") List<String> types,
            @Param("isOpenAccess") Boolean isOpenAccess,
            @Param("fieldId") Integer fieldId,
            @Param("topicId") Integer topicId,
            @Param("visibility") String visibility,
            Pageable pageable
    );

    /** Same filters, but also scans paper abstracts. Much slower — opt in only. */
    @Query(value = "SELECT p FROM Paper p " +
             "LEFT JOIN p.journal j " +
             "LEFT JOIN p.field f " +
             "WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') " +
             "       OR p.paperAbstract LIKE CONCAT('%', :keyword, '%') " +
                                       "       OR p.paperId IN (SELECT tp.paperId FROM Paper tp JOIN tp.topics tt WHERE tt.topicName LIKE CONCAT('%', :keyword, '%'))) " +
             "AND (:author IS NULL OR p.paperId IN (SELECT ap.paperId FROM Paper ap JOIN ap.authors aa WHERE aa.fullName LIKE CONCAT('%', :author, '%'))) " +
             "AND (:journal IS NULL OR j.name LIKE CONCAT('%', :journal, '%')) " +
             "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
             "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
             "AND (:institution IS NULL OR p.paperId IN (SELECT ip.paperId FROM Paper ip JOIN ip.authors ia WHERE ia.affiliation LIKE CONCAT('%', :institution, '%'))) " +
             "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
             "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
             "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
             "AND (:topicId IS NULL OR EXISTS (SELECT 1 FROM p.topics tt WHERE tt.topicId = :topicId)) " +
             "AND (:visibility IS NULL OR p.visibilityStatus = :visibility)",
           countQuery = "SELECT COUNT(p) FROM Paper p " +
             "LEFT JOIN p.journal j " +
             "LEFT JOIN p.field f " +
             "WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') " +
             "       OR p.paperAbstract LIKE CONCAT('%', :keyword, '%') " +
                                       "       OR p.paperId IN (SELECT tp.paperId FROM Paper tp JOIN tp.topics tt WHERE tt.topicName LIKE CONCAT('%', :keyword, '%'))) " +
             "AND (:author IS NULL OR p.paperId IN (SELECT ap.paperId FROM Paper ap JOIN ap.authors aa WHERE aa.fullName LIKE CONCAT('%', :author, '%'))) " +
             "AND (:journal IS NULL OR j.name LIKE CONCAT('%', :journal, '%')) " +
             "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
             "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
             "AND (:institution IS NULL OR p.paperId IN (SELECT ip.paperId FROM Paper ip JOIN ip.authors ia WHERE ia.affiliation LIKE CONCAT('%', :institution, '%'))) " +
             "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
             "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
             "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
             "AND (:topicId IS NULL OR EXISTS (SELECT 1 FROM p.topics tt WHERE tt.topicId = :topicId)) " +
             "AND (:visibility IS NULL OR p.visibilityStatus = :visibility)")
    Page<Paper> searchPapersIncludingAbstract(
            @Param("keyword") String keyword,
            @Param("author") String author,
            @Param("journal") String journal,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            @Param("institution") String institution,
            @Param("types") List<String> types,
            @Param("isOpenAccess") Boolean isOpenAccess,
            @Param("fieldId") Integer fieldId,
            @Param("topicId") Integer topicId,
            /** VISIBLE / HIDDEN, or null for both. Callers below admin must pass VISIBLE.
             *  Typed rather than a String: comparing via CAST(... AS string) converts the
             *  column on every row and the query stops returning at all. */
            @Param("visibility") com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus visibility,
            Pageable pageable
    );
    @Query(value = "SELECT TOP 5 p.* FROM papers p " +
                   "JOIN paper_topics pt ON p.paper_id = pt.paper_id " +
                   "WHERE pt.topic_id = :topicId AND p.paper_id != :paperId " +
                   "AND p.publication_year IS NOT NULL AND p.publication_year > 0 " +
                   "ORDER BY (CAST(p.citation_count + 1 AS FLOAT) / NULLIF(ABS(YEAR(GETDATE()) - p.publication_year) + 1, 0)) DESC", nativeQuery = true)
    List<Paper> findRelatedPapers(@Param("paperId") Long paperId, @Param("topicId") Integer topicId);

    // Year-over-year by publication_year — see getTopicTrendCounts above for why created_at
    // (sync-insertion time) is the wrong basis for a publication trend.
    @Query("""
        SELECT COUNT(DISTINCT p)
        FROM Paper p
        JOIN p.topics t
        WHERE t.topicId = :topicId
          AND p.publicationYear = :year
    """)
        long countTopicPapersByYear(
                @Param("topicId") Integer topicId,
                @Param("year") int year);
}
