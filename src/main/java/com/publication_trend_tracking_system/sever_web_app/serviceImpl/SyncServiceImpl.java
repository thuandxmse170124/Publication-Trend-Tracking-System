package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.*;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperPublicationType;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncServiceImpl implements SyncService {

    private final SyncJobRepository syncJobRepository;
    private final ApiSourceRepository apiSourceRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final AuthorRepository authorRepository;
    private final KeywordRepository keywordRepository;
    private final TopicRepository topicRepository;
    private final org.springframework.context.ApplicationContext applicationContext;

    public RestTemplate restTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
            .setConnectTimeout(java.time.Duration.ofSeconds(5))
            .setReadTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @Override
    public SyncJobResponse syncFromSource(Integer sourceId, Long userId, String customQuery) {
        ApiSource source = apiSourceRepository.findById(sourceId)
                .orElseThrow(() -> new AppException(ErrorCode.API_SOURCE_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(source.getStatus())) {
            throw new AppException(ErrorCode.API_SOURCE_INACTIVE);
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        // 1. Create a SyncJob with RUNNING status
        SyncJob job = syncJobRepository.save(SyncJob.builder()
                .apiSource(source)
                .triggeredBy(user)
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .build());

        // Self-invocation to trigger @Async method
        applicationContext.getBean(SyncService.class).executeSyncJob(job.getSyncJobId(), sourceId, customQuery);

        return toResponse(job);
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void executeSyncJob(Long jobId, Integer sourceId, String customQuery) {
        SyncJob job = syncJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        ApiSource source = apiSourceRepository.findById(sourceId).orElse(null);
        if (source == null) return;

        try {
            // Determine search queries
            List<String> queries = new ArrayList<>();
            if (customQuery != null && !customQuery.trim().isEmpty()) {
                queries.add(customQuery.trim());
            } else {
                // By default, query using topics name and keyword names
                List<Topic> activeTopics = topicRepository.findTop20TrendingTopicEntities();
                for (Topic topic : activeTopics) {
                    queries.add(topic.getTopicName());
                }
                List<Keyword> activeKeywords = keywordRepository.findTop20TrendingKeywords();
                for (Keyword keyword : activeKeywords) {
                    queries.add(keyword.getKeywordName());
                }

                // Default fallback
                if (queries.isEmpty()) {
                    queries.addAll(List.of("Artificial Intelligence", "Machine Learning", "Data Science"));
                }
            }

            int addedCount = 0;
            int updatedCount = 0;

            // 2. Query external API for each query (HTTP calls run outside transactional block)
            for (String query : queries) {
                try {
                    log.info("Starting sync from {} for query: {}", source.getSourceName(), query);
                    String url = buildApiUrl(source, query);
                    String responseBody = fetchFromApi(url);

                    if (responseBody != null && !responseBody.isBlank()) {
                        int[] counts = new int[2]; // [0] = added, [1] = updated
                        // Save results in transaction helper
                        applicationContext.getBean(SyncServiceImpl.class).saveResultsInTransaction(responseBody, source, query, counts);
                        addedCount += counts[0];
                        updatedCount += counts[1];
                    }
                    // Bổ sung Rate Limit delay để tránh HTTP 429
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    log.error("Failed to sync query: " + query + ". Continuing to next...", ex);
                }
            }

            // 3. Update SyncJob with SUCCESS status
            job.setStatus("SUCCESS");
            job.setAddedCount(addedCount);
            job.setUpdatedCount(updatedCount);
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);

            // 4. Update ApiSource last_synced_at
            source.setLastSyncedAt(LocalDateTime.now());
            apiSourceRepository.save(source);

        } catch (Exception ex) {
            log.error("Failed to run sync job id: " + job.getSyncJobId(), ex);
            job.setStatus("FAILED");
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SyncJobResponse> getSyncLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SyncJob> jobs = syncJobRepository.findAllByOrderByStartedAtDesc(pageable);
        return jobs.map(this::toResponse);
    }

    @Override
    public SyncJobResponse retrySyncJob(Long jobId, Long userId) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.SYNC_JOB_NOT_FOUND));
        
        // Retrying means running the sync again for the same source
        return syncFromSource(job.getApiSource().getSourceId(), userId, null);
    }

    @Transactional
    public void saveResultsInTransaction(String responseBody, ApiSource source, String searchQuery, int[] counts) {
        try {
            // Find keyword entity or create it
            Keyword searchKeyword = keywordRepository.findByKeywordNameIgnoreCase(searchQuery)
                    .orElseGet(() -> keywordRepository.save(Keyword.builder().keywordName(searchQuery).build()));

            // Find topic entity if matches
            Topic topic = topicRepository.findByTopicNameIgnoreCase(searchQuery).orElse(null);

            if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
                parseAndSaveOpenAlex(responseBody, source, topic, searchKeyword, counts);
            } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
                parseAndSaveSemanticScholar(responseBody, source, topic, searchKeyword, counts);
            }
        } catch (Exception e) {
            log.error("Error saving data in transactional helper", e);
            throw new RuntimeException("DB transaction error during sync: " + e.getMessage(), e);
        }
    }

    private String buildApiUrl(ApiSource source, String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
            return source.getBaseUrl() + "/works?search=" + encodedQuery + "&per-page=50";
        } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
            return source.getBaseUrl() + "/v1/paper/search?query=" + encodedQuery + "&limit=50&fields=title,abstract,authors,journal,year,externalIds,citationCount";
        }
        throw new IllegalArgumentException("Unsupported source name: " + source.getSourceName());
    }

    private String fetchFromApi(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception ex) {
            log.error("HTTP fetch failed for url: " + url, ex);
            throw new RuntimeException("HTTP fetch failed: " + ex.getMessage(), ex);
        }
    }

    private void parseAndSaveOpenAlex(String jsonResponse, ApiSource source, Topic topic, Keyword searchKeyword, int[] counts) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode results = root.path("results");
        if (results.isArray()) {
            for (JsonNode work : results) {
                String title = work.path("title").asText(null);
                if (title == null || title.isBlank()) continue;

                String doiUrl = work.path("doi").asText(null);
                String doi = doiUrl;
                if (doi != null && doi.startsWith("https://doi.org/")) {
                    doi = doi.substring("https://doi.org/".length());
                }

                Integer year = work.path("publication_year").asInt(LocalDateTime.now().getYear());
                Integer citations = work.path("cited_by_count").asInt(0);

                JsonNode abstractNode = work.path("abstract_inverted_index");
                String paperAbstract = "";
                if (!abstractNode.isMissingNode() && abstractNode.isObject()) {
                    paperAbstract = reconstructAbstractFromJson(abstractNode);
                }

                String sourceUrl = work.path("id").asText("");

                String journalName = work.path("primary_location").path("source").path("display_name").asText(null);

                Set<String> authorNames = new HashSet<>();
                JsonNode authorships = work.path("authorships");
                if (authorships.isArray()) {
                    for (JsonNode authorship : authorships) {
                        String authorName = authorship.path("author").path("display_name").asText(null);
                        if (authorName != null && !authorName.isBlank()) {
                            authorNames.add(authorName);
                        }
                    }
                }

                saveOrUpdatePaper(title, paperAbstract, year, doi, sourceUrl, citations, journalName, authorNames, topic, searchKeyword, source, counts);
            }
        }
    }

    private void parseAndSaveSemanticScholar(String jsonResponse, ApiSource source, Topic topic, Keyword searchKeyword, int[] counts) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode data = root.path("data");
        if (data.isArray()) {
            for (JsonNode paperNode : data) {
                String title = paperNode.path("title").asText(null);
                if (title == null || title.isBlank()) continue;

                String paperAbstract = paperNode.path("abstract").asText("");
                Integer year = paperNode.path("year").asInt(LocalDateTime.now().getYear());
                Integer citations = paperNode.path("citationCount").asInt(0);

                String doi = paperNode.path("externalIds").path("DOI").asText(null);
                if (doi == null || doi.isBlank()) {
                    doi = paperNode.path("externalIds").path("doi").asText(null);
                }

                String sourceUrl = "https://www.semanticscholar.org/paper/" + paperNode.path("paperId").asText("");

                String journalName = paperNode.path("journal").path("name").asText(null);

                Set<String> authorNames = new HashSet<>();
                JsonNode authorsNode = paperNode.path("authors");
                if (authorsNode.isArray()) {
                    for (JsonNode author : authorsNode) {
                        String authorName = author.path("name").asText(null);
                        if (authorName != null && !authorName.isBlank()) {
                            authorNames.add(authorName);
                        }
                    }
                }

                saveOrUpdatePaper(title, paperAbstract, year, doi, sourceUrl, citations, journalName, authorNames, topic, searchKeyword, source, counts);
            }
        }
    }

    private void saveOrUpdatePaper(String title, String paperAbstract, Integer year, String doi, String sourceUrl,
                                    Integer citations, String journalName, Set<String> authorNames,
                                    Topic topic, Keyword searchKeyword, ApiSource source, int[] counts) {
        Paper paper = null;
        if (doi != null && !doi.isBlank()) {
            paper = paperRepository.findByDoiIgnoreCase(doi.trim()).orElse(null);
        } else {
            paper = paperRepository.findByTitleIgnoreCase(title.trim()).stream().findFirst().orElse(null);
        }

        boolean isNew = (paper == null);
        if (isNew) {
            paper = new Paper();
            paper.setDoi(doi != null && !doi.isBlank() ? doi.trim() : null);
            paper.setTitle(title.trim());
            paper.setPaperAbstract(paperAbstract);
            paper.setPublicationYear(year);
            paper.setSourceUrl(sourceUrl);
            paper.setCitationCount(citations);
            paper.setApiSource(source);
            paper.setPublicationType(PaperPublicationType.JOURNAL_ARTICLE);
            paper.setVisibilityStatus(PaperVisibilityStatus.VISIBLE);
            counts[0]++; // addedCount
        } else {
            if (paperAbstract != null && !paperAbstract.isBlank()) {
                paper.setPaperAbstract(paperAbstract);
            }
            paper.setCitationCount(citations);
            paper.setTitle(title.trim());
            paper.setSourceUrl(sourceUrl);
            counts[1]++; // updatedCount
        }

        if (journalName != null && !journalName.isBlank()) {
            Journal journal = journalRepository.findByNameIgnoreCase(journalName.trim())
                    .orElseGet(() -> journalRepository.save(Journal.builder().name(journalName.trim()).build()));
            paper.setJournal(journal);
        }

        Set<Author> authors = new HashSet<>();
        for (String authorName : authorNames) {
            Author author = authorRepository.findByFullNameIgnoreCase(authorName.trim())
                    .orElseGet(() -> authorRepository.save(Author.builder().fullName(authorName.trim()).build()));
            authors.add(author);
        }
        paper.setAuthors(authors);

        Set<Keyword> keywords = new HashSet<>(paper.getKeywords() != null ? paper.getKeywords() : new HashSet<>());
        if (searchKeyword != null) {
            keywords.add(searchKeyword);
        }
        paper.setKeywords(keywords);

        Set<Topic> topics = new HashSet<>(paper.getTopics() != null ? paper.getTopics() : new HashSet<>());
        if (topic != null) {
            topics.add(topic);
        }
        paper.setTopics(topics);

        paperRepository.save(paper);
    }

    private String reconstructAbstractFromJson(JsonNode abstractNode) {
        int maxIndex = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = abstractNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode positions = field.getValue();
            if (positions.isArray()) {
                for (JsonNode pos : positions) {
                    int p = pos.asInt();
                    if (p > maxIndex) {
                        maxIndex = p;
                    }
                }
            }
        }
        String[] words = new String[maxIndex + 1];
        fields = abstractNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String word = field.getKey();
            JsonNode positions = field.getValue();
            if (positions.isArray()) {
                for (JsonNode pos : positions) {
                    int p = pos.asInt();
                    if (p >= 0 && p < words.length) {
                        words[p] = word;
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w != null) {
                sb.append(w).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private SyncJobResponse toResponse(SyncJob job) {
        return SyncJobResponse.builder()
                .syncJobId(job.getSyncJobId())
                .sourceId(job.getApiSource().getSourceId())
                .sourceName(job.getApiSource().getSourceName())
                .triggeredByEmail(job.getTriggeredBy() != null ? job.getTriggeredBy().getEmail() : "SYSTEM")
                .status(job.getStatus())
                .addedCount(job.getAddedCount())
                .updatedCount(job.getUpdatedCount())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }
}
