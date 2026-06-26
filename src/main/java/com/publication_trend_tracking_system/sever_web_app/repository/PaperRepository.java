package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    boolean existsByDoi(String doi);

    List<Paper> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    List<Paper> findAllByOrderByCreatedAtDesc();

    List<Paper> findTop10ByTopics_TopicIdOrderByCreatedAtDesc(Integer topicId);

    @Query("SELECT DISTINCT p FROM Paper p " +
           "LEFT JOIN p.authors a " +
           "LEFT JOIN p.journal j " +
           "LEFT JOIN p.field f " +
           "LEFT JOIN p.topics t " +
           "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperAbstract LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:author IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))) " +
           "AND (:journal IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :journal, '%'))) " +
           "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
           "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
           "AND (:institution IS NULL OR LOWER(a.affiliation) LIKE LOWER(CONCAT('%', :institution, '%'))) " +
           "AND (:types IS NULL OR CAST(p.publicationType AS string) IN :types) " +
           "AND (:isOpenAccess IS NULL OR p.isOpenAccess = :isOpenAccess) " +
           "AND (:fieldId IS NULL OR f.fieldId = :fieldId) " +
           "AND (:topicId IS NULL OR t.topicId = :topicId)")
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
            Pageable pageable
    );

    @Query("SELECT new com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse(p.publicationYear, COUNT(p)) " +
           "FROM Paper p " +
           "LEFT JOIN p.authors a " +
           "LEFT JOIN p.journal j " +
           "LEFT JOIN p.field f " +
           "LEFT JOIN p.topics t " +
           "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.paperAbstract LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:author IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))) " +
           "AND (:journal IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :journal, '%'))) " +
           "AND (:fromYear IS NULL OR p.publicationYear >= :fromYear) " +
           "AND (:toYear IS NULL OR p.publicationYear <= :toYear) " +
           "AND (:institution IS NULL OR LOWER(a.affiliation) LIKE LOWER(CONCAT('%', :institution, '%'))) " +
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
            @Param("topicId") Integer topicId
    );

    @Query(value = "SELECT COUNT(*) FROM papers WHERE YEAR(created_at) = YEAR(GETDATE()) AND MONTH(created_at) = MONTH(GETDATE())", nativeQuery = true)
    long countPapersThisMonth();

    @Query(value = "SELECT COUNT(*) FROM papers WHERE YEAR(created_at) = YEAR(DATEADD(month, -1, GETDATE())) AND MONTH(created_at) = MONTH(DATEADD(month, -1, GETDATE()))", nativeQuery = true)
    long countPapersLastMonth();

    @Query(value = "SELECT p.publication_year, COUNT(p.paper_id) FROM papers p WHERE p.publication_year IS NOT NULL GROUP BY p.publication_year ORDER BY p.publication_year DESC", nativeQuery = true)
    java.util.List<Object[]> findDistinctYearsWithCount();
}
