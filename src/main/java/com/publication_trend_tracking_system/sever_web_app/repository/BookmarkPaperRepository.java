package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BookmarkPaperRepository
        extends JpaRepository<BookmarkPaper, Long> {

    List<BookmarkPaper> findByFolderFolderId(
            Long folderId);

    boolean existsByFolderFolderIdAndPaperId(
            Long folderId,
            Long paperId);

    Optional<BookmarkPaper> findByBookmarkId(
            Long bookmarkId);

    List<BookmarkPaper> findByUserUserIdAndFolderIsNull(
            Long userId);

    boolean existsByUserUserIdAndPaperId(
            Long userId,
            Long paperId);

    @Modifying
    @Transactional
    void deleteByUserUserIdAndPaperIdAndFolderIsNull(
            Long userId,
            Long paperId);
    boolean existsByUserUserIdAndPaperIdAndFolderIsNull(
            Long userId,
            Long paperId);
}