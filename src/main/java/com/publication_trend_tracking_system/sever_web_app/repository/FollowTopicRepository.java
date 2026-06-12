package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.FollowTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowTopicRepository
        extends JpaRepository<FollowTopic, Long> {

    List<FollowTopic> findByUserUserId(
            Long userId);

    boolean existsByUserUserIdAndTopicId(
            Long userId,
            String topicId);

    void deleteByUserUserIdAndTopicId(
            Long userId,
            String topicId);
}