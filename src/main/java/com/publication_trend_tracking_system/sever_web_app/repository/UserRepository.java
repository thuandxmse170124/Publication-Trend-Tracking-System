package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Thống kê cá nhân cho Dashboard
    @Query(value = "SELECT COUNT(*) FROM folder_papers fp JOIN bookmark_folders bf ON fp.folder_id = bf.folder_id WHERE bf.user_id = :userId", nativeQuery = true)
    long countBookmarksByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM follow_topic WHERE user_id = :userId", nativeQuery = true)
    long countFollowedTopicsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM follow_author WHERE user_id = :userId", nativeQuery = true)
    long countFollowedAuthorsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM follow_journal WHERE user_id = :userId", nativeQuery = true)
    long countFollowedJournalsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0", nativeQuery = true)
    long countUnreadNotificationsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT TOP 3 search_query FROM search_history WHERE user_id = :userId ORDER BY searched_at DESC", nativeQuery = true)
    java.util.List<String> findRecentSearchesByUserId(@Param("userId") Long userId);
}