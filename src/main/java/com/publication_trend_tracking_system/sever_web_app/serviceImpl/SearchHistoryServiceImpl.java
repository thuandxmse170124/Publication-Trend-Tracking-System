package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SearchHistoryResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.SearchHistory;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.SearchHistoryRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PaperService;
import com.publication_trend_tracking_system.sever_web_app.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;
    private final PaperService paperService;

    private static final int MAX_HISTORY_PER_USER = 50;

    @Override
    @Transactional
    public void saveSearchHistory(String keyword, String email) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        String cleanKeyword = keyword.trim();
        User user = getUserByEmail(email);

        // Check if keyword already exists
        Optional<SearchHistory> existingHistory = searchHistoryRepository.findFirstByUserUserIdAndKeywordIgnoreCase(user.getUserId(), cleanKeyword);
        
        if (existingHistory.isPresent()) {
            // Update time
            SearchHistory history = existingHistory.get();
            history.setSearchedAt(LocalDateTime.now());
            searchHistoryRepository.save(history);
            return;
        }

        // Check limit
        long count = searchHistoryRepository.countByUserUserId(user.getUserId());
        if (count >= MAX_HISTORY_PER_USER) {
            searchHistoryRepository.findFirstByUserUserIdOrderBySearchedAtAsc(user.getUserId())
                    .ifPresent(searchHistoryRepository::delete);
        }

        // Save new
        SearchHistory newHistory = SearchHistory.builder()
                .user(user)
                .keyword(cleanKeyword)
                .searchedAt(LocalDateTime.now())
                .build();
                
        searchHistoryRepository.save(newHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getRecentSearchHistory(String email) {
        User user = getUserByEmail(email);
        List<SearchHistory> histories = searchHistoryRepository.findTop10ByUserUserIdOrderBySearchedAtDesc(user.getUserId());
        
        return histories.stream()
                .map(h -> SearchHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .keyword(h.getKeyword())
                        .searchedAt(h.getSearchedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponse> getSuggestionsBasedOnHistory(String email) {
        User user = getUserByEmail(email);
        
        // Lấy 5 từ khóa gần nhất để gợi ý
        List<SearchHistory> recentHistories = searchHistoryRepository.findTop10ByUserUserIdOrderBySearchedAtDesc(user.getUserId());
        
        if (recentHistories.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> keywords = recentHistories.stream()
                .map(SearchHistory::getKeyword)
                .limit(5)
                .toList();

        Map<Long, PaperResponse> uniquePapers = new LinkedHashMap<>();
        
        // Với mỗi keyword, lấy top 5 bài báo
        for (String kw : keywords) {
            Page<PaperResponse> paperPage = paperService.searchPapers(
                    kw, null, null, null, null, null, null, null, null, null,
                    PageRequest.of(0, 5, Sort.by("citationCount").descending())
            );
            
            for (PaperResponse pr : paperPage.getContent()) {
                uniquePapers.putIfAbsent(pr.getPaperId(), pr);
            }
        }
        
        List<PaperResponse> suggestions = new ArrayList<>(uniquePapers.values());
        
        // Trộn nhẹ hoặc cắt bớt nếu nhiều quá, ở đây trả về max 20 bài báo
        return suggestions.stream().limit(20).toList();
    }

    @Override
    @Transactional
    public void deleteSearchHistory(Long historyId, String email) {
        User user = getUserByEmail(email);
        SearchHistory history = searchHistoryRepository.findById(historyId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // You could add HISTORY_NOT_FOUND

        if (!history.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        searchHistoryRepository.delete(history);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
