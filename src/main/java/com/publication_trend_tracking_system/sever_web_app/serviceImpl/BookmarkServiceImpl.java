package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateFolderRequest;
import com.publication_trend_tracking_system.sever_web_app.repository.BookmarkPaperRepository;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateNoteRequest;
import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkFolder;
import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkPaper;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.repository.BookmarkFolderRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FolderResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.request.SavePaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateFolderRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl
        implements BookmarkService {

    private final BookmarkPaperRepository
            bookmarkPaperRepository;
    private final BookmarkFolderRepository
            bookmarkFolderRepository;
    private final PaperRepository
            paperRepository;
    private final UserRepository
            userRepository;

    @Override
    public void createFolder(
            CreateFolderRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        if (request.getFolderName() == null
                || request.getFolderName()
                .trim()
                .isEmpty()) {

            throw new AppException(
                    ErrorCode.FOLDER_NAME_EMPTY);
        }

        if (bookmarkFolderRepository
                .existsByUserUserIdAndFolderName(
                        user.getUserId(),
                        request.getFolderName())) {

            throw new AppException(
                    ErrorCode.FOLDER_ALREADY_EXISTS);
        }

        BookmarkFolder folder =
                BookmarkFolder.builder()
                        .folderName(
                                request.getFolderName())
                        .user(user)
                        .build();

        bookmarkFolderRepository.save(folder);
    }

    @Override
    public List<FolderResponse> getMyFolders(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return bookmarkFolderRepository
                .findByUserUserId(
                        user.getUserId())
                .stream()
                .map(folder ->
                        FolderResponse.builder()
                                .folderId(
                                        folder.getFolderId())
                                .folderName(
                                        folder.getFolderName())
                                .build())
                .toList();
    }

    @Override
    public void updateFolder(
            Long folderId,
            UpdateFolderRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        BookmarkFolder folder =
                bookmarkFolderRepository
                        .findById(folderId)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new AppException(
                    ErrorCode.UNAUTHORIZED);
        }
        if (request.getFolderName() == null
                || request.getFolderName()
                .trim()
                .isEmpty()) {

            throw new AppException(
                    ErrorCode.FOLDER_NAME_EMPTY);
        }
        if (bookmarkFolderRepository
                .existsByUserUserIdAndFolderName(
                        user.getUserId(),
                        request.getFolderName())) {

            throw new AppException(
                    ErrorCode.FOLDER_ALREADY_EXISTS);
        }
        folder.setFolderName(
                request.getFolderName());

        bookmarkFolderRepository.save(folder);
    }
    @Override
    public void deleteFolder(
            Long folderId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        BookmarkFolder folder =
                bookmarkFolderRepository
                        .findById(folderId)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new AppException(
                    ErrorCode.UNAUTHORIZED);
        }

        bookmarkFolderRepository.delete(folder);
    }
    @Override
    public void savePaper(
            Long folderId,
            SavePaperRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        BookmarkFolder folder =
                bookmarkFolderRepository
                        .findById(folderId)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new AppException(
                    ErrorCode.UNAUTHORIZED);
        }
                paperRepository
                        .findById(
                                request.getPaperId())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.PAPER_NOT_FOUND));
        boolean exists =
                bookmarkPaperRepository
                        .existsByFolderFolderIdAndPaperId(
                                folderId,
                                request.getPaperId());

        if (exists) {

            throw new AppException(
                    ErrorCode.PAPER_ALREADY_SAVED);
        }

        BookmarkPaper paper =
                BookmarkPaper.builder()
                        .paperId(
                                request.getPaperId())
                        .note(
                                request.getNote())
                        .folder(folder)
                        .build();

        bookmarkPaperRepository.save(paper);
    }
    @Override
    public void removePaper(
            Long bookmarkId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        BookmarkPaper paper =
                bookmarkPaperRepository
                        .findById(bookmarkId)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.SAVED_PAPER_NOT_FOUND));

        if (!paper.getFolder()
                .getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new RuntimeException(
                    "You do not own this bookmark");
        }

        bookmarkPaperRepository.delete(paper);
    }
    @Override
    public void updateNote(
            Long bookmarkId,
            UpdateNoteRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        BookmarkPaper paper =
                bookmarkPaperRepository
                        .findById(bookmarkId)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.SAVED_PAPER_NOT_FOUND));

        if (!paper.getFolder()
                .getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new AppException(
                    ErrorCode.UNAUTHORIZED);
        }

        paper.setNote(
                request.getNote());

        bookmarkPaperRepository.save(paper);
    }
}