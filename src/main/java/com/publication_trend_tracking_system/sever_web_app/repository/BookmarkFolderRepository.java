package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkFolderRepository
        extends JpaRepository<BookmarkFolder, Long> {

    List<BookmarkFolder> findByUserUserId(
            Long userId);

    boolean existsByUserUserIdAndFolderName(
            Long userId,
            String folderName);
}