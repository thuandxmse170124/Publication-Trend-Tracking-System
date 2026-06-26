package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateFolderRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.SavePaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateFolderRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateNoteRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/folders")
    public ApiResponse<?> createFolder(
            @RequestBody CreateFolderRequest request,
            Authentication authentication) {

        String email =
                authentication.getName();

        bookmarkService.createFolder(
                request,
                email);

        return ApiResponse.builder()
                .code(1000)
                .message("Folder created successfully")
                .build();
    }
    @GetMapping("/folders")
    public ApiResponse<?> getMyFolders(
            Authentication authentication) {

        String email =
                authentication.getName();

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        bookmarkService
                                .getMyFolders(email))
                .build();
    }
    @PutMapping("/folders/{folderId}")
    public ApiResponse<?> updateFolder(
            @PathVariable Long folderId,
            @RequestBody UpdateFolderRequest request,
            Authentication authentication) {

        bookmarkService.updateFolder(
                folderId,
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Folder updated successfully")
                .build();
    }
    @DeleteMapping("/folders/{folderId}")
    public ApiResponse<?> deleteFolder(
            @PathVariable Long folderId,
            Authentication authentication) {

        bookmarkService.deleteFolder(
                folderId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Folder deleted successfully")
                .build();
    }
    @PostMapping("/folders/{folderId}/papers")
    public ApiResponse<?> savePaper(
            @PathVariable Long folderId,
            @RequestBody SavePaperRequest request,
            Authentication authentication) {

        bookmarkService.savePaper(
                folderId,
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Paper saved successfully")
                .build();
    }
    @DeleteMapping("/papers/{bookmarkId}")
    public ApiResponse<?> removePaper(
            @PathVariable Long bookmarkId,
            Authentication authentication) {

        bookmarkService.removePaper(
                bookmarkId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Paper removed successfully")
                .build();
    }
    @PutMapping("/papers/{bookmarkId}/note")
    public ApiResponse<?> updateNote(
            @PathVariable Long bookmarkId,
            @RequestBody UpdateNoteRequest request,
            Authentication authentication) {

        bookmarkService.updateNote(
                bookmarkId,
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Note updated successfully")
                .build();
    }
    @GetMapping("/folders/{folderId}/papers")
    public ApiResponse<?> getFolderPapers(
            @PathVariable Long folderId,
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        bookmarkService.getFolderPapers(
                                folderId,
                                authentication.getName()))
                .build();
    }
}