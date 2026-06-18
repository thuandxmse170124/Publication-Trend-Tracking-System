package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.JournalResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Journal;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.repository.JournalRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.JournalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JournalServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @InjectMocks
    private JournalServiceImpl journalService;

    private Journal journal;

    @BeforeEach
    void setUp() {
        journal = Journal.builder()
                .journalId(1)
                .name("Nature")
                .issn("1476-4687")
                .publisher("Springer Nature")
                .status("active")
                .build();
    }

    @Test
    void getJournalById_Success() {
        when(journalRepository.findById(1)).thenReturn(Optional.of(journal));

        JournalResponse response = journalService.getJournalById(1);

        assertNotNull(response);
        assertEquals(journal.getJournalId(), response.getJournalId());
        assertEquals(journal.getName(), response.getName());
        assertEquals(journal.getIssn(), response.getIssn());
        assertEquals(journal.getPublisher(), response.getPublisher());
        assertEquals(journal.getStatus(), response.getStatus());
        verify(journalRepository, times(1)).findById(1);
    }

    @Test
    void getJournalById_NotFound() {
        when(journalRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> journalService.getJournalById(1));
        verify(journalRepository, times(1)).findById(1);
    }
}
