package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    Optional<Topic> findFirstByTopicNameIgnoreCase(String topicName);
    Optional<Topic> findByTopicNameIgnoreCase(String topicName);
    java.util.List<Topic> findAllByTopicNameInIgnoreCase(java.util.Collection<String> topicNames);
    Optional<Topic> findByOpenalexId(String openalexId);

    // Legacy-only name match for taxonomy seeding: restricted to rows with no openalexId yet, so
    // two distinct official OpenAlex topics whose names differ only by case (e.g. T11690
    // "Advanced battery technologies research" vs T10663 "Advanced Battery Technologies
    // Research") can never collide and overwrite each other once either already has an ID.
    Optional<Topic> findByTopicNameIgnoreCaseAndOpenalexIdIsNull(String topicName);

    // Official taxonomy only (excludes legacy ad-hoc topics with no openalexId)
    java.util.List<Topic> findAllByOpenalexIdIsNotNull();
    long countByOpenalexIdIsNotNull();

    // Backfill resumability: official topics not yet swept by the full-history sync job
    java.util.List<Topic> findAllByOpenalexIdIsNotNullAndLastSyncedAtIsNull();

    // Scheduled background sync (Implementation Plan v3 optimization): only re-check topics that
    // already have at least one paper, instead of sweeping the entire 4,510-topic taxonomy on
    // every scheduled run. The full sweep stays available via "Sync All" for admins.
    @Query(value = "SELECT DISTINCT t.* FROM topics t " +
                   "WHERE t.openalex_id IS NOT NULL " +
                   "AND EXISTS (SELECT 1 FROM paper_topics pt WHERE pt.topic_id = t.topic_id)",
           nativeQuery = true)
    java.util.List<Topic> findAllOfficialTopicsWithExistingPapers();

    @Query(value = "SELECT t.topic_id, t.topic_name, t.description, " +
                   "(SELECT COUNT(*) FROM follow_topics ft WHERE ft.topic_id = t.topic_id) as follower_count, " +
                   "(SELECT COUNT(*) FROM paper_topics pt WHERE pt.topic_id = t.topic_id) as paper_count " +
                   "FROM topics t " +
                   "ORDER BY follower_count DESC " +
                   "OFFSET 0 ROWS FETCH NEXT 5 ROWS ONLY", nativeQuery = true)
    java.util.List<Object[]> findTop5TrendingTopicsRaw();

    @Query(value = "SELECT t.topic_id as topicId, t.topic_name as topicName, t.description as description, COUNT(pt.paper_id) as paperCount " +
                   "FROM topics t " +
                   "LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id " +
                   "GROUP BY t.topic_id, t.topic_name, t.description, t.trend_score ",
           countQuery = "SELECT COUNT(topic_id) FROM topics",
           nativeQuery = true)
    org.springframework.data.domain.Page<Object[]> findAllTopicsWithPaperCount(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(p) FROM Paper p JOIN p.topics t WHERE t.topicId = :topicId")
    long countPapersByTopicId(@Param("topicId") Integer topicId);

    @Query(value = "SELECT t.topic_id, COUNT(pt.paper_id) FROM topics t LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id WHERE t.topic_id IN :topicIds GROUP BY t.topic_id", nativeQuery = true)
    java.util.List<Object[]> countPapersByTopicIds(@Param("topicIds") java.util.List<Integer> topicIds);

    @Query(value = "SELECT TOP 5 CAST(t.topic_name AS NVARCHAR(255)) FROM topics t " +
                   "LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id " +
                   "LEFT JOIN papers p ON pt.paper_id = p.paper_id " +
                   "GROUP BY t.topic_id, t.topic_name " +
                   "ORDER BY SUM(ISNULL(CAST(p.citation_count + 1 AS FLOAT) / (ABS(YEAR(GETDATE()) - p.publication_year) + 1), 0)) DESC", nativeQuery = true)
    java.util.List<String> findTop5TrendingTopicNames();

    @Query(value = "SELECT TOP 5 t.* FROM topics t " +
                   "LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id " +
                   "LEFT JOIN papers p ON pt.paper_id = p.paper_id " +
                   "GROUP BY t.topic_id, t.topic_name, t.description, t.trend_score, t.openalex_id, t.subfield_id, t.last_synced_at " +
                   "ORDER BY SUM(ISNULL(CAST(p.citation_count + 1 AS FLOAT) / (ABS(YEAR(GETDATE()) - p.publication_year) + 1), 0)) DESC", nativeQuery = true)
    java.util.List<Topic> findTop5TrendingTopics();

    @Query(value = "SELECT TOP 5 t.* FROM topics t " +
                   "LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id " +
                   "LEFT JOIN papers p ON pt.paper_id = p.paper_id " +
                   "WHERE p.field_id = :fieldId " +
                   "GROUP BY t.topic_id, t.topic_name, t.description, t.trend_score, t.openalex_id, t.subfield_id, t.last_synced_at " +
                   "ORDER BY SUM(ISNULL(CAST(p.citation_count + 1 AS FLOAT) / (ABS(YEAR(GETDATE()) - p.publication_year) + 1), 0)) DESC", nativeQuery = true)
    java.util.List<Topic> findTop5PersonalizedTrendingTopics(@Param("fieldId") Integer fieldId);

    // Time Decay: Boost recent trends
    @Query(value = "SELECT TOP 50 t.topic_id, t.topic_name, SUM(ISNULL(CAST(p.citation_count + 1 AS FLOAT) / (ABS(YEAR(GETDATE()) - p.publication_year) + 1), 0)) AS TrendScore " +
                   "FROM topics t LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id LEFT JOIN papers p ON pt.paper_id = p.paper_id " +
                   "WHERE t.topic_name LIKE :search " +
                   "GROUP BY t.topic_id, t.topic_name, t.description, t.trend_score, t.openalex_id, t.subfield_id, t.last_synced_at " +
                   "ORDER BY TrendScore DESC", nativeQuery = true)
    java.util.List<Object[]> findTop50TopicsWithCount(@Param("search") String search);

    @Query(value = "SELECT TOP 50 t.topic_id, t.topic_name, SUM(ISNULL(CAST(p.citation_count + 1 AS FLOAT) / (ABS(YEAR(GETDATE()) - p.publication_year) + 1), 0)) AS TrendScore " +
            "FROM topics t " +
            "JOIN paper_topics pt ON t.topic_id = pt.topic_id " +
            "JOIN papers p ON pt.paper_id = p.paper_id " +
            "WHERE p.publication_year IS NOT NULL " +
            "GROUP BY t.topic_id, t.topic_name " +
            "ORDER BY TrendScore DESC", nativeQuery = true)
    java.util.List<Object[]> findTop50TrendingTopicsWithScore();
}
