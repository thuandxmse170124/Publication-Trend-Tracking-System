package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateFolderRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.SavePaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateFolderRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateNoteRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.BookmarkPaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FolderResponse;
import java.util.List;
public interface BookmarkService {

    void createFolder(
            CreateFolderRequest request,
            String email);

    void updateFolder(
            Long folderId,
            UpdateFolderRequest request,
            String email);

    void deleteFolder(
            Long folderId,
            String email);

    List<FolderResponse> getMyFolders(
            String email);

    void savePaper(
            Long folderId,
            SavePaperRequest request,
            String email);
    void removePaper(
            Long bookmarkId,
            String email);
    void updateNote(
            Long bookmarkId,
            UpdateNoteRequest request,
            String email);
    List<BookmarkPaperResponse>
    getFolderPapers(
            Long folderId,
            String email);
    boolean isBookmarked(
            Long paperId,
            String email);
}