package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import com.publication_trend_tracking_system.sever_web_app.repository.AuthorRepository;
import com.publication_trend_tracking_system.sever_web_app.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    public AuthorResponse getAuthorById(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found"));
        return toResponse(author);
    }

    private AuthorResponse toResponse(Author author) {
        return AuthorResponse.builder()
                .authorId(author.getAuthorId())
                .fullName(author.getFullName())
                .affiliation(author.getAffiliation())
                .orcid(author.getOrcid())
                .build();
    }
}
