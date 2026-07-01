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

    public static class SyncStats {
        public int addedCount = 0;
        public int updatedCount = 0;
    }

    public static class ParsedPaperDTO {
        public String title;
        public String paperAbstract;
        public Integer year;
        public String doi;
        public String sourceUrl;
        public Integer citations;
        public String journalName;
        public Set<String> authorNames = new HashSet<>();
        public Set<String> topicNames = new HashSet<>();
    }

    private final SyncJobRepository syncJobRepository;
    private final ApiSourceRepository apiSourceRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final AuthorRepository authorRepository;
    private final KeywordRepository keywordRepository;
    private final TopicRepository topicRepository;
    private final ResearchFieldRepository researchFieldRepository;
    private final org.springframework.context.ApplicationContext applicationContext;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

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

        try {
            // Self-invocation to trigger @Async method
            applicationContext.getBean(SyncService.class).executeSyncJob(job.getSyncJobId(), sourceId, customQuery);
        } catch (org.springframework.core.task.TaskRejectedException ex) {
            job.setStatus("FAILED");
            job.setErrorMessage("Server is too busy. Sync queue is full.");
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);
            // Throw a runtime exception that GlobalExceptionHandler can catch, or a custom one
            throw new RuntimeException("Server is too busy. Sync queue is full. Please try again later.");
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setErrorMessage("Failed to submit sync job: " + ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);
            throw new RuntimeException("Failed to submit sync job.", ex);
        }

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
            Set<String> queries = new LinkedHashSet<>();
            if (customQuery != null && !customQuery.trim().isEmpty()) {
                queries.add(customQuery.trim());
            } else {
                // By default, query using topics name and keyword names
                List<Topic> activeTopics = topicRepository.findTop5TrendingTopics();
                for (Topic topic : activeTopics) {
                    queries.add(topic.getTopicName());
                }
                List<Object[]> topKeywords = keywordRepository.findTop50TrendingKeywordNamesWithCount();
                for (Object[] row : topKeywords) {
                    if (row.length > 0 && row[0] instanceof String) {
                        queries.add((String) row[0]);
                    }
                }

                // Default fallback with massive seed list for preload
                if (queries.isEmpty()) {
                    queries.addAll(List.of("Artificial Intelligence", "Machine Learning", "Data Science", "Computer Science", 
                            "Environmental Science", "Economics", "Medicine", "Biology", "Physics", "Chemistry",
                            "Mathematics", "Psychology", "Sociology", "Business", "Engineering", "Materials Science",
                            "History", "Political Science", "Philosophy", "Art"));
                }
            }

            int addedCount = 0;
            int updatedCount = 0;

            // 2. Query external API for each query (HTTP calls run outside transactional block)
            for (String query : queries) {
                log.info("Starting sync from {} for query: {}", source.getSourceName(), query);
                // Pagination loop: Fetch 4 pages per query (4 * 50 = 200 papers per topic)
                for (int page = 1; page <= 4; page++) {
                    try {
                        String url = buildApiUrl(source, query, page);
                        String responseBody = fetchFromApi(url);

                        if (responseBody != null && !responseBody.isBlank()) {
                            SyncStats stats = new SyncStats();
                            // Save results in transaction helper
                            applicationContext.getBean(SyncServiceImpl.class).saveResultsInTransaction(responseBody, source, query, stats);
                            addedCount += stats.addedCount;
                            updatedCount += stats.updatedCount;
                        }
                    } catch (Exception ex) {
                        log.error("Failed to sync query: " + query + " at page " + page + ". Continuing to next page...", ex);
                    } finally {
                        // Bổ sung Rate Limit delay để tránh HTTP 429
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                // Prevent rate limiting (HTTP 429) from external APIs
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
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
    public void saveResultsInTransaction(String responseBody, ApiSource source, String searchQuery, SyncStats stats) {
        try {
            // Find keyword entity or create it
            Keyword searchKeyword = keywordRepository.findFirstByKeywordNameIgnoreCase(searchQuery)
                    .orElseGet(() -> keywordRepository.save(Keyword.builder().keywordName(searchQuery).build()));

            // Find topic entity if matches
            Topic topic = topicRepository.findFirstByTopicNameIgnoreCase(searchQuery).orElse(null);

            // Find or create ResearchField
            ResearchField researchField = researchFieldRepository.findFirstByFieldNameIgnoreCase(searchQuery)
                    .orElseGet(() -> researchFieldRepository.save(ResearchField.builder().fieldName(searchQuery).build()));

            if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
                parseAndSaveOpenAlex(responseBody, source, topic, searchKeyword, researchField, stats);
            } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
                parseAndSaveSemanticScholar(responseBody, source, topic, searchKeyword, researchField, stats);
            }

            // Flush and clear L1 cache to prevent OOM during massive syncs
            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            log.error("Error saving data in transactional helper", e);
            throw new RuntimeException("DB transaction error during sync: " + e.getMessage(), e);
        }
    }

    private String buildApiUrl(ApiSource source, String query, int page) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
            return source.getBaseUrl() + "/works?search=" + encodedQuery + "&per-page=50&page=" + page;
        } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
            int offset = (page - 1) * 50;
            return source.getBaseUrl() + "/v1/paper/search?query=" + encodedQuery + "&limit=50&offset=" + offset + "&fields=title,abstract,authors,journal,year,externalIds,citationCount,fieldsOfStudy";
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

    private void parseAndSaveOpenAlex(String jsonResponse, ApiSource source, Topic topic, Keyword searchKeyword, ResearchField researchField, SyncStats stats) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode results = root.path("results");
        if (results.isArray()) {
            List<ParsedPaperDTO> parsedPapers = new ArrayList<>();
            for (JsonNode work : results) {
                ParsedPaperDTO dto = new ParsedPaperDTO();
                dto.title = work.path("title").asText(null);
                if (dto.title == null || dto.title.isBlank()) continue;
                String doiUrl = work.path("doi").asText(null);
                dto.doi = doiUrl != null && doiUrl.startsWith("https://doi.org/") ? doiUrl.substring(16) : doiUrl;
                dto.year = work.path("publication_year").asInt(LocalDateTime.now().getYear());
                dto.citations = work.path("cited_by_count").asInt(0);
                JsonNode abstractNode = work.path("abstract_inverted_index");
                dto.paperAbstract = (!abstractNode.isMissingNode() && abstractNode.isObject()) ? reconstructAbstractFromJson(abstractNode) : "";
                dto.sourceUrl = work.path("id").asText("");
                dto.journalName = work.path("primary_location").path("source").path("display_name").asText(null);
                JsonNode authorships = work.path("authorships");
                if (authorships.isArray()) {
                    for (JsonNode authorship : authorships) {
                        String authorName = authorship.path("author").path("display_name").asText(null);
                        if (authorName != null && !authorName.isBlank()) dto.authorNames.add(authorName.trim());
                    }
                }
                JsonNode concepts = work.path("concepts");
                if (concepts.isArray()) {
                    for (JsonNode concept : concepts) {
                        if (concept.path("level").asInt(99) <= 1) {
                            String conceptName = concept.path("display_name").asText(null);
                            if (conceptName != null && !conceptName.isBlank()) dto.topicNames.add(conceptName.trim());
                        }
                    }
                }
                parsedPapers.add(dto);
            }
            batchSavePapers(parsedPapers, source, topic, searchKeyword, researchField, stats);
        }
    }

    private void parseAndSaveSemanticScholar(String jsonResponse, ApiSource source, Topic topic, Keyword searchKeyword, ResearchField researchField, SyncStats stats) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode data = root.path("data");
        if (data.isArray()) {
            List<ParsedPaperDTO> parsedPapers = new ArrayList<>();
            for (JsonNode paperNode : data) {
                ParsedPaperDTO dto = new ParsedPaperDTO();
                dto.title = paperNode.path("title").asText(null);
                if (dto.title == null || dto.title.isBlank()) continue;
                dto.paperAbstract = paperNode.path("abstract").asText("");
                dto.year = paperNode.path("year").asInt(LocalDateTime.now().getYear());
                dto.citations = paperNode.path("citationCount").asInt(0);
                dto.doi = paperNode.path("externalIds").path("DOI").asText(null);
                if (dto.doi == null || dto.doi.isBlank()) dto.doi = paperNode.path("externalIds").path("doi").asText(null);
                dto.sourceUrl = "https://www.semanticscholar.org/paper/" + paperNode.path("paperId").asText("");
                dto.journalName = paperNode.path("journal").path("name").asText(null);
                JsonNode authorsNode = paperNode.path("authors");
                if (authorsNode.isArray()) {
                    for (JsonNode author : authorsNode) {
                        String authorName = author.path("name").asText(null);
                        if (authorName != null && !authorName.isBlank()) dto.authorNames.add(authorName.trim());
                    }
                }
                JsonNode fieldsOfStudy = paperNode.path("fieldsOfStudy");
                if (fieldsOfStudy.isArray()) {
                    for (JsonNode field : fieldsOfStudy) {
                        String fieldName = field.asText(null);
                        if (fieldName != null && !fieldName.isBlank()) dto.topicNames.add(fieldName.trim());
                    }
                }
                parsedPapers.add(dto);
            }
            batchSavePapers(parsedPapers, source, topic, searchKeyword, researchField, stats);
        }
    }

    private void batchSavePapers(List<ParsedPaperDTO> dtoList, ApiSource source, Topic topic, Keyword searchKeyword, ResearchField researchField, SyncStats stats) {
        if (dtoList.isEmpty()) return;

        Set<String> allDois = new HashSet<>();
        Set<String> allTitles = new HashSet<>();
        Set<String> allJournals = new HashSet<>();
        Set<String> allAuthors = new HashSet<>();
        Set<String> allTopics = new HashSet<>();

        for (ParsedPaperDTO dto : dtoList) {
            if (dto.doi != null && !dto.doi.isBlank()) allDois.add(dto.doi.trim());
            allTitles.add(dto.title.trim());
            if (dto.journalName != null && !dto.journalName.isBlank()) allJournals.add(dto.journalName.trim());
            allAuthors.addAll(dto.authorNames);
            allTopics.addAll(dto.topicNames);
        }

        Map<String, Paper> existingPapersByDoi = new HashMap<>();
        if (!allDois.isEmpty()) {
            for (Paper p : paperRepository.findAllByDoiInIgnoreCase(allDois)) {
                if (p.getDoi() != null) existingPapersByDoi.put(p.getDoi().toLowerCase(), p);
            }
        }
        Map<String, Paper> existingPapersByTitle = new HashMap<>();
        for (Paper p : paperRepository.findAllByTitleInIgnoreCase(allTitles)) {
            existingPapersByTitle.put(p.getTitle().toLowerCase(), p);
        }

        Map<String, Journal> existingJournalsMap = new HashMap<>();
        if (!allJournals.isEmpty()) {
            for (Journal j : journalRepository.findAllByNameInIgnoreCase(allJournals)) {
                existingJournalsMap.put(j.getName().toLowerCase(), j);
            }
        }

        Map<String, Author> existingAuthorsMap = new HashMap<>();
        if (!allAuthors.isEmpty()) {
            for (Author a : authorRepository.findAllByFullNameInIgnoreCase(allAuthors)) {
                existingAuthorsMap.put(a.getFullName().toLowerCase(), a);
            }
        }

        Map<String, Topic> existingTopicsMap = new HashMap<>();
        if (!allTopics.isEmpty()) {
            for (Topic t : topicRepository.findAllByTopicNameInIgnoreCase(allTopics)) {
                existingTopicsMap.put(t.getTopicName().toLowerCase(), t);
            }
        }

        List<Paper> papersToSave = new ArrayList<>();
        List<Journal> journalsToSave = new ArrayList<>();
        List<Author> authorsToSave = new ArrayList<>();
        List<Topic> topicsToSave = new ArrayList<>();

        for (ParsedPaperDTO dto : dtoList) {
            Paper paper = null;
            if (dto.doi != null && !dto.doi.isBlank() && existingPapersByDoi.containsKey(dto.doi.trim().toLowerCase())) {
                paper = existingPapersByDoi.get(dto.doi.trim().toLowerCase());
            } else if (existingPapersByTitle.containsKey(dto.title.trim().toLowerCase())) {
                paper = existingPapersByTitle.get(dto.title.trim().toLowerCase());
            }

            boolean isNew = (paper == null);
            if (isNew) {
                paper = new Paper();
                paper.setDoi(dto.doi != null && !dto.doi.isBlank() ? dto.doi.trim() : null);
                paper.setTitle(dto.title.trim());
                paper.setPaperAbstract(dto.paperAbstract);
                paper.setPublicationYear(dto.year);
                paper.setSourceUrl(dto.sourceUrl);
                paper.setCitationCount(dto.citations);
                paper.setApiSource(source);
                paper.setPublicationType(PaperPublicationType.JOURNAL_ARTICLE);
                paper.setVisibilityStatus(PaperVisibilityStatus.VISIBLE);
                paper.setField(researchField);
                
                if (paper.getDoi() != null) existingPapersByDoi.put(paper.getDoi().toLowerCase(), paper);
                existingPapersByTitle.put(paper.getTitle().toLowerCase(), paper);
                stats.addedCount++;
            } else {
                if (dto.paperAbstract != null && !dto.paperAbstract.isBlank()) paper.setPaperAbstract(dto.paperAbstract);
                paper.setCitationCount(dto.citations);
                paper.setTitle(dto.title.trim());
                paper.setSourceUrl(dto.sourceUrl);
                stats.updatedCount++;
            }

            if (dto.journalName != null && !dto.journalName.isBlank()) {
                String jName = dto.journalName.trim();
                Journal journal = existingJournalsMap.get(jName.toLowerCase());
                if (journal == null) {
                    journal = Journal.builder().name(jName).build();
                    journalsToSave.add(journal);
                    existingJournalsMap.put(jName.toLowerCase(), journal);
                }
                paper.setJournal(journal);
            }

            Set<Author> paperAuthors = new HashSet<>();
            for (String aName : dto.authorNames) {
                Author author = existingAuthorsMap.get(aName.toLowerCase());
                if (author == null) {
                    author = Author.builder().fullName(aName).build();
                    authorsToSave.add(author);
                    existingAuthorsMap.put(aName.toLowerCase(), author);
                }
                paperAuthors.add(author);
            }
            paper.setAuthors(paperAuthors);

            Set<Keyword> keywords = new HashSet<>(paper.getKeywords() != null ? paper.getKeywords() : new HashSet<>());
            if (searchKeyword != null) keywords.add(searchKeyword);
            paper.setKeywords(keywords);

            Set<Topic> topicsSet = new HashSet<>(paper.getTopics() != null ? paper.getTopics() : new HashSet<>());
            if (topic != null) topicsSet.add(topic);
            for (String tName : dto.topicNames) {
                Topic t = existingTopicsMap.get(tName.toLowerCase());
                if (t == null) {
                    t = Topic.builder().topicName(tName).build();
                    topicsToSave.add(t);
                    existingTopicsMap.put(tName.toLowerCase(), t);
                }
                topicsSet.add(t);
            }
            paper.setTopics(topicsSet);

            papersToSave.add(paper);
        }

        if (!journalsToSave.isEmpty()) journalRepository.saveAll(journalsToSave);
        if (!authorsToSave.isEmpty()) authorRepository.saveAll(authorsToSave);
        if (!topicsToSave.isEmpty()) topicRepository.saveAll(topicsToSave);
        paperRepository.saveAll(papersToSave);
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
