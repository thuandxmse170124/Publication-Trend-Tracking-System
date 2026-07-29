package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.SimilarPaperDTO;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SimilarityResponseDTO;
import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.SimilarityService;
import com.publication_trend_tracking_system.sever_web_app.service.UserSubscriptionService;
import com.publication_trend_tracking_system.sever_web_app.util.CosineSimilarityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityServiceImpl implements SimilarityService {

    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionService userSubscriptionService;

    @Override
    @Transactional(readOnly = true)
    public SimilarityResponseDTO analyzeSimilarity(MultipartFile file) {
        validatePremium();

        String extractedText = extractTextFromPdf(file);
        
        if (extractedText == null || extractedText.isBlank()) {
            throw new RuntimeException("Could not extract text from the provided PDF file.");
        }

        // 1. Extract top keywords from uploaded paper
        List<String> topKeywords = CosineSimilarityUtil.extractTopKeywords(extractedText, 20);

        // 2. Fetch a pool of papers to compare against. 
        // For performance, we'll just fetch the top 1000 most recently published/cited papers.
        // In a real production system with Elasticsearch, this would be a full-text search query.
        Page<Paper> recentPapers = paperRepository.findAll(
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "publicationYear", "citationCount"))
        );

        List<SimilarPaperDTO> similarPapers = new ArrayList<>();

        // 3. Calculate similarity score for each paper
        for (Paper paper : recentPapers.getContent()) {
            String paperContent = paper.getTitle() + " " + (paper.getPaperAbstract() != null ? paper.getPaperAbstract() : "");
            double score = CosineSimilarityUtil.calculateCosineSimilarity(extractedText, paperContent);
            
            if (score > 0.0) {
                SimilarPaperDTO dto = SimilarPaperDTO.builder()
                        .paperId(String.valueOf(paper.getPaperId()))
                        .title(paper.getTitle())
                        .publicationYear(paper.getPublicationYear())
                        .citationCount(paper.getCitationCount())
                        .authors(paper.getAuthors().stream().map(Author::getFullName).collect(Collectors.toList()))
                        .doi(paper.getDoi())
                        .sourceUrl(paper.getSourceUrl())
                        .similarityScore(score)
                        .similarityPercentage(String.format("%.1f%%", score * 100))
                        .build();
                similarPapers.add(dto);
            }
        }

        // 4. Sort by score descending and take top 5
        similarPapers.sort(Comparator.comparing(SimilarPaperDTO::getSimilarityScore).reversed());
        List<SimilarPaperDTO> top5 = similarPapers.stream().limit(5).collect(Collectors.toList());

        return SimilarityResponseDTO.builder()
                .uploadedFileName(file.getOriginalFilename())
                .extractedKeywords(topKeywords)
                .similarPapers(top5)
                .totalMatches(top5.size())
                .build();
    }

    private void validatePremium() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!userSubscriptionService.isPremium(user.getUserId())) {
            throw new AppException(ErrorCode.PREMIUM_REQUIRED);
        }
    }

    private String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            // Optional: Limit to first 2 pages for speed since abstracts/introductions are usually there
            stripper.setEndPage(2);
            return stripper.getText(document);
            
        } catch (Exception e) {
            log.error("Error reading PDF file", e);
            return null;
        }
    }
}
