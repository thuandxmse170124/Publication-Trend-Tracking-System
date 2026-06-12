package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.FollowJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowJournalRepository
        extends JpaRepository<FollowJournal, Long> {

    List<FollowJournal> findByUserUserId(
            Long userId);

    boolean existsByUserUserIdAndJournalId(
            Long userId,
            String journalId);

    void deleteByUserUserIdAndJournalId(
            Long userId,
            String journalId);
}