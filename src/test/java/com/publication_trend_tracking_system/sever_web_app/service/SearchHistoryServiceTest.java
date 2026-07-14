package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SearchHistoryResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.SearchHistory;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.repository.SearchHistoryRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.SearchHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaperService paperService;

    @InjectMocks
    private SearchHistoryServiceImpl searchHistoryService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@example.com");
    }

    @Test
    void saveSearchHistory_NewKeyword_ShouldSave() {
        // Arrange
        String keyword = "AI";
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(searchHistoryRepository.findFirstByUserUserIdAndKeywordIgnoreCase(mockUser.getUserId(), keyword))
                .thenReturn(Optional.empty());
        when(searchHistoryRepository.countByUserUserId(mockUser.getUserId())).thenReturn(10L);

        // Act
        searchHistoryService.saveSearchHistory(keyword, mockUser.getEmail());

        // Assert
        verify(searchHistoryRepository, times(1)).save(any(SearchHistory.class));
    }

    @Test
    void getRecentSearchHistory_ShouldReturnList() {
        // Arrange
        SearchHistory sh = new SearchHistory();
        sh.setHistoryId(1L);
        sh.setKeyword("Deep Learning");
        sh.setSearchedAt(LocalDateTime.now());
        
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(searchHistoryRepository.findTop10ByUserUserIdOrderBySearchedAtDesc(mockUser.getUserId()))
                .thenReturn(Arrays.asList(sh));

        // Act
        List<SearchHistoryResponse> result = searchHistoryService.getRecentSearchHistory(mockUser.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Deep Learning", result.get(0).getKeyword());
    }

    @Test
    void getSuggestionsBasedOnHistory_ShouldReturnPapers() {
        // Arrange
        SearchHistory sh = new SearchHistory();
        sh.setKeyword("Machine Learning");
        
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(searchHistoryRepository.findTop10ByUserUserIdOrderBySearchedAtDesc(mockUser.getUserId()))
                .thenReturn(Arrays.asList(sh));
                
        PaperResponse paper = PaperResponse.builder().paperId(10L).title("ML Paper").build();
        Page<PaperResponse> page = new PageImpl<>(Arrays.asList(paper));
        
        when(paperService.searchPapers(
                eq("Machine Learning"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 5, Sort.by("citationCount").descending()))
        )).thenReturn(page);

        // Act
        List<PaperResponse> result = searchHistoryService.getSuggestionsBasedOnHistory(mockUser.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ML Paper", result.get(0).getTitle());
    }
}
