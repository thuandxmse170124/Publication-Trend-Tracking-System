package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findFirstByFullNameIgnoreCase(String fullName);
    java.util.List<Author> findAllByFullNameInIgnoreCase(java.util.Collection<String> fullNames);
}
