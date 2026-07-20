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
import com.publication_trend_tracking_system.sever_web_app.service.NotificationService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncServiceImpl implements SyncService {

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
        public Set<String> keywordNames = new HashSet<>();
        public LocalDate publicationDate;
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
    private final NotificationService notificationService;
    private final java.util.concurrent.ConcurrentHashMap<Long, Boolean> stopFlags = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void stopSyncJob(Long jobId) {
        SyncJob job = syncJobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.SYNC_JOB_NOT_FOUND));
        if ("RUNNING".equalsIgnoreCase(job.getStatus())) {
            stopFlags.put(jobId, true);
        }
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public RestTemplate restTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
            .setConnectTimeout(java.time.Duration.ofSeconds(5))
            .setReadTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @Override
    public SyncJobResponse syncAll(Integer sourceId, Long userId, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange) {
        return syncFromSource(sourceId, userId, null, timeRange);
    }

    @Override
    public SyncJobResponse syncFromSource(Integer sourceId, Long userId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange) {
        ApiSource source = apiSourceRepository.findById(sourceId)
                .orElseThrow(() -> new AppException(ErrorCode.API_SOURCE_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(source.getStatus())) {
            throw new AppException(ErrorCode.API_SOURCE_INACTIVE);
        }

        if (syncJobRepository.existsByApiSource_SourceIdAndStatus(sourceId, "RUNNING")) {
            throw new AppException(ErrorCode.SYNC_JOB_ALREADY_RUNNING);
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        SyncJob job = syncJobRepository.save(SyncJob.builder()
                .apiSource(source)
                .triggeredBy(user)
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .build());

        try {
            applicationContext.getBean(SyncService.class).executeSyncJob(job.getSyncJobId(), sourceId, customQuery, timeRange);
        } catch (org.springframework.core.task.TaskRejectedException ex) {
            job.setStatus("FAILED");
            job.setErrorMessage("Server is too busy. Sync queue is full.");
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);
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
    public void executeSyncJob(Long jobId, Integer sourceId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange) {
        SyncJob job = syncJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        ApiSource source = apiSourceRepository.findById(sourceId).orElse(null);
        if (source == null) return;

        LocalDate cutoffDate = null;
        if (timeRange != null) {
            switch (timeRange) {
                case DAY: cutoffDate = LocalDate.now().minusDays(1); break;
                case WEEK: cutoffDate = LocalDate.now().minusWeeks(1); break;
                case MONTH: cutoffDate = LocalDate.now().minusMonths(1); break;
                case ALL: cutoffDate = null; break;
            }
        }

        try {
            Set<String> queries = new LinkedHashSet<>();
            if (customQuery != null && !customQuery.trim().isEmpty()) {
                queries.add(customQuery.trim());
            } else {
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
                if (queries.isEmpty()) {
                    queries.addAll(List.of("Artificial Intelligence", "Machine Learning", "Data Science", "Computer Science", 
                            "Environmental Science", "Economics", "Medicine", "Biology", "Physics", "Chemistry"));
                }
            }

            int addedCount = 0;
            int updatedCount = 0;
            List<Paper> newPapers = new ArrayList<>();

            for (String query : queries) {
                log.info("Starting sync from {} for query: {}", source.getSourceName(), query);
                int page = 1;
                while (true) {
                    if (stopFlags.getOrDefault(jobId, false)) {
                        log.warn("Sync job {} was manually stopped.", jobId);
                        job.setStatus("CANCELED");
                        job.setFinishedAt(LocalDateTime.now());
                        job.setErrorMessage("Manually stopped by Admin");
                        syncJobRepository.save(job);
                        stopFlags.remove(jobId);
                        return;
                    }

                    String url = buildApiUrl(source, query, page, cutoffDate);
                    String responseBody = fetchFromApi(url);

                    if (responseBody == null || responseBody.isBlank()) {
                        break;
                    }

                    int[] counts = new int[2];
                    boolean continuePagination = applicationContext
                            .getBean(SyncServiceImpl.class)
                            .saveResultsInTransaction(
                                    responseBody,
                                    source,
                                    query,
                                    counts,
                                    newPapers,
                                    cutoffDate);
                    addedCount += counts[0];
                    updatedCount += counts[1];

                    if (!continuePagination) {
                        log.info("Short-circuiting pagination for query {} as we reached items older than cutoffDate.", query);
                        break;
                    }

                    if (counts[0] == 0 && counts[1] == 0) {
                        break;
                    }

                    page++;
                    try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }

            job.setStatus("SUCCESS");
            job.setAddedCount(addedCount);
            job.setUpdatedCount(updatedCount);
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);

            source.setLastSyncedAt(LocalDateTime.now());
            apiSourceRepository.save(source);
            
            if (!newPapers.isEmpty()) {
                log.info("Creating notifications for {} new papers", newPapers.size());
                notificationService.notifyUsersForNewPapers(newPapers);
            }

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
        return syncFromSource(job.getApiSource().getSourceId(), userId, null, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange.ALL);
    }

    @Transactional
    public boolean saveResultsInTransaction(String responseBody, ApiSource source, String searchQuery, int[] counts, List<Paper> newPapers, LocalDate cutoffDate) {
        try {
            Keyword searchKeyword = keywordRepository.findFirstByKeywordNameIgnoreCase(searchQuery)
                    .orElseGet(() -> keywordRepository.save(Keyword.builder().keywordName(searchQuery).build()));

            Topic topic = topicRepository.findFirstByTopicNameIgnoreCase(searchQuery).orElse(null);

            ResearchField researchField = researchFieldRepository.findFirstByFieldNameIgnoreCase(searchQuery)
                    .orElseGet(() -> researchFieldRepository.save(ResearchField.builder().fieldName(searchQuery).build()));

            boolean continuePagination = true;
            if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
                continuePagination = parseAndSaveOpenAlex(responseBody, source, topic, searchKeyword, researchField, counts, newPapers, cutoffDate);
            } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
                continuePagination = parseAndSaveSemanticScholar(responseBody, source, topic, searchKeyword, researchField, counts, newPapers, cutoffDate);
            }

            entityManager.flush();
            entityManager.clear();
            
            return continuePagination;
        } catch (Exception e) {
            log.error("Error saving data in transactional helper", e);
            throw new RuntimeException("DB transaction error during sync: " + e.getMessage(), e);
        }
    }

    private String buildApiUrl(ApiSource source, String query, int page, LocalDate cutoffDate) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
            String url = source.getBaseUrl() + "/works?search=" + encodedQuery + "&per-page=200&page=" + page;
            if (cutoffDate != null) {
                url += "&filter=from_publication_date:" + cutoffDate.toString();
            }
            url += "&sort=publication_date:desc";
            return url;
        } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
            int offset = (page - 1) * 50;
            String url = source.getBaseUrl() + "/v1/paper/search?query=" + encodedQuery + "&limit=50&offset=" + offset + "&fields=title,abstract,authors,journal,year,externalIds,citationCount,fieldsOfStudy";
            if (cutoffDate != null) {
                url += "&year=" + cutoffDate.getYear() + "-";
                url += ",publicationDate";
            }
            url += "&sort=publicationDate:desc";
            return url;
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
            return null;
        }
    }

    private boolean parseAndSaveOpenAlex(
            String jsonResponse,
            ApiSource source,
            Topic topic,
            Keyword searchKeyword,
            ResearchField researchField,
            int[] counts,
            List<Paper> newPapers,
            LocalDate cutoffDate) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode results = root.path("results");
        if (results.isArray()) {
            for (JsonNode work : results) {
                String pubDateStr = work.path("publication_date").asText(null);
                LocalDate pubDate = null;
                if (pubDateStr != null && !pubDateStr.isBlank()) {
                    try {
                        pubDate = LocalDate.parse(pubDateStr);
                    } catch (DateTimeParseException ignored) {}
                }
                
                if (cutoffDate != null && pubDate != null && pubDate.isBefore(cutoffDate)) {
                    return false; // Reached older papers
                }

                ParsedPaperDTO dto = new ParsedPaperDTO();
                dto.title = work.path("title").asText(null);
                if (dto.title == null || dto.title.isBlank()) continue;
                if (dto.title.length() > 250) dto.title = dto.title.substring(0, 247) + "...";
                
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
                        int level = concept.path("level").asInt(99);
                        String conceptName = concept.path("display_name").asText(null);
                        if (conceptName != null && !conceptName.isBlank()) {
                            if (level <= 1) {
                                dto.topicNames.add(conceptName.trim());
                            } else {
                                dto.keywordNames.add(conceptName.trim());
                            }
                        }
                    }
                }
                JsonNode keywordsNode = work.path("keywords");
                if (keywordsNode.isArray()) {
                    for (JsonNode keywordNode : keywordsNode) {
                        String kwName = keywordNode.path("display_name").asText(null);
                        if (kwName != null && !kwName.isBlank()) {
                            dto.keywordNames.add(kwName.trim());
                        }
                    }
                }

                saveOrUpdatePaper(dto, topic, searchKeyword, researchField, source, counts, newPapers);
            }
        }
        return true;
    }

    private boolean parseAndSaveSemanticScholar(
            String jsonResponse,
            ApiSource source,
            Topic topic,
            Keyword searchKeyword,
            ResearchField researchField,
            int[] counts,
            List<Paper> newPapers,
            LocalDate cutoffDate) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode data = root.path("data");
        if (data.isArray()) {
            for (JsonNode paperNode : data) {
                String pubDateStr = paperNode.path("publicationDate").asText(null);
                LocalDate pubDate = null;
                if (pubDateStr != null && !pubDateStr.isBlank()) {
                    try {
                        pubDate = LocalDate.parse(pubDateStr);
                    } catch (DateTimeParseException ignored) {}
                }
                int year = paperNode.path("year").asInt(0);

                if (cutoffDate != null) {
                    if (pubDate != null) {
                        if (pubDate.isBefore(cutoffDate)) return false;
                    } else if (year > 0 && year < cutoffDate.getYear()) {
                        return false;
                    }
                }

                ParsedPaperDTO dto = new ParsedPaperDTO();
                dto.title = paperNode.path("title").asText(null);
                if (dto.title == null || dto.title.isBlank()) continue;
                if (dto.title.length() > 250) dto.title = dto.title.substring(0, 247) + "...";
                dto.paperAbstract = paperNode.path("abstract").asText("");
                dto.year = year > 0 ? year : LocalDateTime.now().getYear();
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

                saveOrUpdatePaper(dto, topic, searchKeyword, researchField, source, counts, newPapers);
            }
        }
        return true;
    }

    private void saveOrUpdatePaper(ParsedPaperDTO dto, Topic topic, Keyword searchKeyword, ResearchField researchField, ApiSource source, int[] counts, List<Paper> newPapers) {
        Paper paper = null;
        if (dto.doi != null && !dto.doi.isBlank()) {
            paper = paperRepository.findByDoiIgnoreCase(dto.doi.trim()).orElse(null);
        }
        if (paper == null) {
            paper = paperRepository.findByTitleIgnoreCase(dto.title.trim()).stream().findFirst().orElse(null);
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
            counts[0]++;
        } else {
            if (dto.paperAbstract != null && !dto.paperAbstract.isBlank()) paper.setPaperAbstract(dto.paperAbstract);
            paper.setCitationCount(dto.citations);
            paper.setTitle(dto.title.trim());
            paper.setSourceUrl(dto.sourceUrl);
            counts[1]++;
        }

        if (dto.journalName != null && !dto.journalName.isBlank()) {
            String jName = dto.journalName.trim();
            Journal journal = journalRepository.findFirstByNameIgnoreCase(jName).orElse(null);
            if (journal == null) {
                journal = journalRepository.save(Journal.builder().name(jName).build());
            }
            paper.setJournal(journal);
        }

        Set<Author> paperAuthors = new HashSet<>();
        for (String aName : dto.authorNames) {
            Author author = authorRepository.findFirstByFullNameIgnoreCase(aName).orElse(null);
            if (author == null) {
                author = authorRepository.save(Author.builder().fullName(aName).build());
            }
            paperAuthors.add(author);
        }
        paper.setAuthors(paperAuthors);

        Set<Keyword> keywords = new HashSet<>(paper.getKeywords() != null ? paper.getKeywords() : new HashSet<>());
        for (String kwName : dto.keywordNames) {
            Keyword kw = keywordRepository.findFirstByKeywordNameIgnoreCase(kwName).orElse(null);
            if (kw == null) {
                kw = keywordRepository.save(Keyword.builder().keywordName(kwName).build());
            }
            keywords.add(kw);
        }
        paper.setKeywords(keywords);

        Set<Topic> topicsSet = new HashSet<>(paper.getTopics() != null ? paper.getTopics() : new HashSet<>());
        if (topic != null) topicsSet.add(topic);
        for (String tName : dto.topicNames) {
            Topic t = topicRepository.findFirstByTopicNameIgnoreCase(tName).orElse(null);
            if (t == null) {
                t = topicRepository.save(Topic.builder().topicName(tName).build());
            }
            topicsSet.add(t);
        }
        paper.setTopics(topicsSet);

        Paper savedPaper = paperRepository.save(paper);
        if (isNew) {
            newPapers.add(savedPaper);
        }
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
