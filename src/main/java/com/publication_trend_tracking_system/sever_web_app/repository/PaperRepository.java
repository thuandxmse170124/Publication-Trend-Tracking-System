package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

        boolean existsByDoi(String doi);

        long countByTopics_TopicId(
                        Integer topicId);

        java.util.Optional<Paper> findFirstByDoiIgnoreCase(String doi);

        Page<Paper> findByLastSyncJobIdOrderByUpdatedAtDesc(Long lastSyncJobId, Pageable pageable);

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

        @Query(value = "SELECT COUNT(*) FROM papers WHERE created_at >= DATEADD(month, DATEDIFF(month, 0, GETDATE()), 0) AND created_at < DATEADD(month, DATEDIFF(month, 0, GETDATE()) + 1, 0)", nativeQuery = true)
        long countPapersThisMonth();

        @Query(value = "SELECT COUNT(*) FROM papers WHERE created_at >= DATEADD(month, DATEDIFF(month, 0, GETDATE()) - 1, 0) AND created_at < DATEADD(month, DATEDIFF(month, 0, GETDATE()), 0)", nativeQuery = true)
        long countPapersLastMonth();

        @Query(value = "SELECT pt.topic_id, " +
                        "SUM(CASE WHEN p.created_at BETWEEN :currentStart AND :now THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN p.created_at BETWEEN :previousStart AND :currentStart THEN 1 ELSE 0 END) " +
                        "FROM papers p JOIN paper_topics pt ON p.paper_id = pt.paper_id " +
                        "GROUP BY pt.topic_id", nativeQuery = true)
        List<Object[]> getTopicTrendCounts(@Param("previousStart") LocalDateTime previousStart,
                                           @Param("currentStart") LocalDateTime currentStart,
                                           @Param("now") LocalDateTime now);

        @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse(p.publicationYear, COUNT(DISTINCT p)) "
                        +
                        "FROM Paper p " +
                        "LEFT JOIN p.authors a " +
                        "LEFT JOIN p.journal j " +
                        "LEFT JOIN p.field f " +
                        "LEFT JOIN p.topics t " +
                        "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperAbstract LIKE LOWER(CONCAT('%', :keyword, '%'))) "
                        +
                        "AND (:author IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))) " +
                        "AND (:journal IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :journal, '%'))) " +
                        "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
                        "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
                        "AND (:institution IS NULL OR LOWER(a.affiliation) LIKE LOWER(CONCAT('%', :institution, '%'))) "
                        +
                        "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
                        "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
                        "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
                        "AND (:topicId IS NULL OR t.topicId = :topicId) " +
                        "GROUP BY p.publicationYear " +
                        "ORDER BY p.publicationYear ASC")
        List<com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse> countPapersByYearWithFilters(
                        @Param("keyword") String keyword,
                        @Param("author") String author,
                        @Param("journal") String journal,
                        @Param("fromYear") Integer fromYear,
                        @Param("toYear") Integer toYear,
                        @Param("institution") String institution,
                        @Param("types") List<String> types,
                        @Param("isOpenAccess") Boolean isOpenAccess,
                        @Param("fieldId") Integer fieldId,
                        @Param("topicId") Integer topicId);

        @Query(value = "SELECT p.publication_year, COUNT(p.paper_id) FROM papers p WHERE p.publication_year IS NOT NULL AND CAST(p.publication_year AS VARCHAR) LIKE :search GROUP BY p.publication_year ORDER BY p.publication_year DESC", nativeQuery = true)
        java.util.List<Object[]> findDistinctYearsWithCount(
                        @Param("search") String search);

        @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse(k.keywordName, COUNT(p.paperId)) "
                        +
                        "FROM Paper p JOIN p.keywords k " +
                        "GROUP BY k.keywordName " +
                        "ORDER BY COUNT(p.paperId) DESC")
        java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse> findTopKeywords(
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse(j.name, COUNT(p.paperId)) "
                        +
                        "FROM Paper p JOIN p.journal j " +
                        "WHERE (:fieldId IS NULL OR p.field.fieldId = :fieldId) " +
                        "GROUP BY j.name " +
                        "ORDER BY COUNT(p.paperId) DESC")
        java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse> findTopJournalsByPaperCount(
                        @Param("fieldId") Integer fieldId, org.springframework.data.domain.Pageable pageable);

        @Query(value = "SELECT DISTINCT p FROM Paper p " +
                        "LEFT JOIN p.authors a " +
                        "LEFT JOIN p.journal j " +
                        "LEFT JOIN p.field f " +
                        "LEFT JOIN p.topics t " +
                        "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperAbstract LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperId = :idKeyword) "
                        +
                        "AND (:author IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))) " +
                        "AND (:journal IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :journal, '%'))) " +
                        "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
                        "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
                        "AND (:institution IS NULL OR LOWER(a.affiliation) LIKE LOWER(CONCAT('%', :institution, '%'))) "
                        +
                        "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
                        "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
                        "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
                        "AND (:topicId IS NULL OR t.topicId = :topicId) " +
                        "AND (p.visibilityStatus IN :visibilities)", countQuery = "SELECT COUNT(DISTINCT p) FROM Paper p "
                                        +
                                        "LEFT JOIN p.authors a " +
                                        "LEFT JOIN p.journal j " +
                                        "LEFT JOIN p.field f " +
                                        "LEFT JOIN p.topics t " +
                                        "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperAbstract LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperId = :idKeyword) "
                                        +
                                        "AND (:author IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))) "
                                        +
                                        "AND (:journal IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :journal, '%'))) "
                                        +
                                        "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
                                        "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
                                        "AND (:institution IS NULL OR LOWER(a.affiliation) LIKE LOWER(CONCAT('%', :institution, '%'))) "
                                        +
                                        "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
                                        "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
                                        "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
                                        "AND (:topicId IS NULL OR t.topicId = :topicId) " +
                                        "AND (p.visibilityStatus IN :visibilities)")
        Page<Paper> searchPapers(
                        @Param("keyword") String keyword,
                        @Param("idKeyword") Long idKeyword,
                        @Param("author") String author,
                        @Param("journal") String journal,
                        @Param("fromYear") Integer fromYear,
                        @Param("toYear") Integer toYear,
                        @Param("institution") String institution,
                        @Param("types") List<String> types,
                        @Param("isOpenAccess") Boolean isOpenAccess,
                        @Param("fieldId") Integer fieldId,
                        @Param("topicId") Integer topicId,
                        @Param("visibilities") List<com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus> visibilities,
                        Pageable pageable);

        @Query(value = "SELECT TOP 5 p.* FROM papers p " +
                        "JOIN paper_topics pt ON p.paper_id = pt.paper_id " +
                        "WHERE pt.topic_id = :topicId AND p.paper_id != :paperId " +
                        "AND p.publication_year IS NOT NULL AND p.publication_year > 0 " +
                        "ORDER BY (CAST(p.citation_count + 1 AS FLOAT) / NULLIF(ABS(YEAR(GETDATE()) - p.publication_year) + 1, 0)) DESC", nativeQuery = true)
        List<Paper> findRelatedPapers(@Param("paperId") Long paperId, @Param("topicId") Integer topicId);

        @Query("""
                            SELECT COUNT(DISTINCT p)
                            FROM Paper p
                            JOIN p.topics t
                            WHERE t.topicId = :topicId
                              AND p.createdAt >= :start
                              AND p.createdAt < :end
                        """)
        long countTopicPapersBetween(
                        @Param("topicId") Integer topicId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
