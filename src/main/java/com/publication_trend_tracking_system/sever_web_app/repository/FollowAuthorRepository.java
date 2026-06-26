package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.FollowAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowAuthorRepository
        extends JpaRepository<FollowAuthor, Long> {

    List<FollowAuthor> findByUserUserId(
            Long userId);

    boolean existsByUserUserIdAndAuthorAuthorId(
            Long userId,
            Long authorId);

    void deleteByUserUserIdAndAuthorAuthorId(
            Long userId,
            Long authorId);
}