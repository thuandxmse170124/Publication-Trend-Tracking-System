package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkPaperRepository
        extends JpaRepository<BookmarkPaper, Long> {

    List<BookmarkPaper> findByFolderFolderId(
            Long folderId);
    existsByFolderFolderIdAndPaperId(
            Long folderId,
            Long paperId)
    Optional<BookmarkPaper> findByBookmarkId(
            Long bookmarkId);
}