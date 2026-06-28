package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.FollowTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowTopicRepository
        extends JpaRepository<FollowTopic, Long> {

    List<FollowTopic> findByUserUserId(
            Long userId);

    boolean existsByUserUserIdAndTopicTopicId(
            Long userId,
            Integer topicId);

    void deleteByUserUserIdAndTopicTopicId(
            Long userId,
            Integer topicId);
}