package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Integer> {
    Optional<Keyword> findFirstByKeywordNameIgnoreCase(String keywordName);

    @org.springframework.data.jpa.repository.Query(value = "SELECT TOP 50 k.keyword_name, COUNT(pk.paper_id) FROM keywords k JOIN paper_keywords pk ON k.keyword_id = pk.keyword_id GROUP BY k.keyword_name ORDER BY COUNT(pk.paper_id) DESC", nativeQuery = true)
    java.util.List<Object[]> findTop50KeywordNamesWithCount();
}
