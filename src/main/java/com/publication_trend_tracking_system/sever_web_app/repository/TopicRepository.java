package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    Optional<Topic> findByTopicName(String topicName);

    @Query(value = "SELECT t.topic_id, t.topic_name, COUNT(ft.user_id) as follower_count " +
                   "FROM topics t " +
                   "LEFT JOIN follow_topic ft ON t.topic_id = ft.topic_id " +
                   "GROUP BY t.topic_id, t.topic_name " +
                   "ORDER BY follower_count DESC " +
                   "OFFSET 0 ROWS FETCH NEXT 5 ROWS ONLY", nativeQuery = true)
    java.util.List<Object[]> findTop5TrendingTopics();

    @Query("SELECT COUNT(p) FROM Paper p JOIN p.topics t WHERE t.topicId = :topicId")
    long countPapersByTopicId(@Param("topicId") Integer topicId);
}
