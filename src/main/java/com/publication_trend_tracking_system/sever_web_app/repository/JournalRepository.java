package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<Journal, Integer> {
    java.util.Optional<Journal> findFirstByNameIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Query(value = "SELECT TOP 50 j.name, COUNT(p.paper_id) FROM journals j JOIN papers p ON j.journal_id = p.journal_id GROUP BY j.name ORDER BY COUNT(p.paper_id) DESC", nativeQuery = true)
    java.util.List<Object[]> findTop50JournalNamesWithCount();
}
