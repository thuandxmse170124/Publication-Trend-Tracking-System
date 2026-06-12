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

    @Query("SELECT DISTINCT p FROM Paper p " +
           "LEFT JOIN p.authors a " +
           "LEFT JOIN p.journal j " +
           "LEFT JOIN p.field f " +
           "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.paperAbstract) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:author IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))) " +
           "AND (:journal IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :journal, '%'))) " +
           "AND (:year IS NULL OR p.publicationYear = :year) " +
           "AND (:fieldId IS NULL OR f.fieldId = :fieldId)")
    Page<Paper> searchPapers(
            @Param("keyword") String keyword,
            @Param("author") String author,
            @Param("journal") String journal,
            @Param("year") Integer year,
            @Param("fieldId") Integer fieldId,
            Pageable pageable
    );
}
