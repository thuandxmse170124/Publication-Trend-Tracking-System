package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop10ByUserUserIdOrderBySearchedAtDesc(Long userId);

    long countByUserUserId(Long userId);

    Optional<SearchHistory> findFirstByUserUserIdOrderBySearchedAtAsc(Long userId);
    
    // Check if keyword already exists for this user to avoid spamming the same keyword
    Optional<SearchHistory> findFirstByUserUserIdAndKeywordIgnoreCase(Long userId, String keyword);
}
