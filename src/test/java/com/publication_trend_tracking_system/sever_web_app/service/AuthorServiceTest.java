package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.repository.AuthorRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.AuthorServiceImpl;
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
public class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private Author author;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .authorId(1L)
                .fullName("John Doe")
                .affiliation("MIT")
                .orcid("0000-0001-2345-6789")
                .build();
    }

    @Test
    void getAuthorById_Success() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        AuthorResponse response = authorService.getAuthorById(1L);

        assertNotNull(response);
        assertEquals(author.getAuthorId(), response.getAuthorId());
        assertEquals(author.getFullName(), response.getFullName());
        assertEquals(author.getAffiliation(), response.getAffiliation());
        assertEquals(author.getOrcid(), response.getOrcid());
        verify(authorRepository, times(1)).findById(1L);
    }

    @Test
    void getAuthorById_NotFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> authorService.getAuthorById(1L));
        verify(authorRepository, times(1)).findById(1L);
    }
}
