package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository
        extends JpaRepository<Author, Long> {
}