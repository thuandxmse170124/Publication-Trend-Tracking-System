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
        // Canonical OpenAlex topic IDs (short form, e.g. "T10068") parsed from the work's
        // "topics" array. Replaces the old concepts-level<=1 name matching.
        public Set<String> topicOpenalexIds = new HashSet<>();
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
    private final com.publication_trend_tracking_system.sever_web_app.config.OpenAlexKeyRotator openAlexKeyRotator;
    private final com.publication_trend_tracking_system.sever_web_app.repository.SchedulerSettingsRepository schedulerSettingsRepository;

    private static final Integer SCHEDULER_SETTINGS_ID = 1;

    @Override
    @Transactional(readOnly = true)
    public boolean isSchedulerEnabled() {
        return schedulerSettingsRepository.findById(SCHEDULER_SETTINGS_ID)
                .map(com.publication_trend_tracking_system.sever_web_app.entity.SchedulerSettings::isEnabled)
                .orElse(true); // default ON until an admin explicitly toggles it
    }

    @Override
    @Transactional
    public boolean setSchedulerEnabled(boolean enabled) {
        com.publication_trend_tracking_system.sever_web_app.entity.SchedulerSettings settings = schedulerSettingsRepository
                .findById(SCHEDULER_SETTINGS_ID)
                .orElseGet(() -> com.publication_trend_tracking_system.sever_web_app.entity.SchedulerSettings.builder()
                        .id(SCHEDULER_SETTINGS_ID)
                        .build());
        settings.setEnabled(enabled);
        schedulerSettingsRepository.save(settings);
        return enabled;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @org.springframework.transaction.annotation.Transactional
    public void cleanupZombieJobs() {
        syncJobRepository.resetRunningJobsToFailed();
        log.info("Cleaned up any zombie sync jobs left from previous server runs.");
    }

    @Override
    public void stopSyncJob(Long jobId) {
        SyncJob job = syncJobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.SYNC_JOB_NOT_FOUND));
        if ("RUNNING".equalsIgnoreCase(job.getStatus())) {
            job.setStatus("CANCELED");
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorMessage("Manually stopped by Admin");
            syncJobRepository.save(job);
        }
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public RestTemplate restTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
            .setConnectTimeout(java.time.Duration.ofSeconds(5))
            .setReadTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @Override
    public SyncJobResponse syncAll(Integer sourceId, Long userId, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate) {
        return syncFromSource(sourceId, userId, null, timeRange, fromDate, toDate);
    }

    @Override
    public void triggerScheduledSync(Integer sourceId) {
        ApiSource source = apiSourceRepository.findById(sourceId)
                .orElseThrow(() -> new AppException(ErrorCode.API_SOURCE_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(source.getStatus())) {
            throw new AppException(ErrorCode.API_SOURCE_INACTIVE);
        }
        if (syncJobRepository.existsByApiSource_SourceIdAndStatus(sourceId, "RUNNING")) {
            throw new AppException(ErrorCode.SYNC_JOB_ALREADY_RUNNING);
        }

        SyncJob job = syncJobRepository.save(SyncJob.builder()
                .apiSource(source)
                .triggeredBy(null)
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .build());

        try {
            applicationContext.getBean(SyncService.class).executeScheduledSyncJob(job.getSyncJobId(), sourceId);
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setErrorMessage("Failed to submit scheduled sync job: " + ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.save(job);
            throw new RuntimeException("Failed to submit scheduled sync job.", ex);
        }
    }

    @Override
    public SyncJobResponse syncFromSource(Integer sourceId, Long userId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate) {
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
            applicationContext.getBean(SyncService.class).executeSyncJob(job.getSyncJobId(), sourceId, customQuery, timeRange, fromDate, toDate);
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
    public void executeSyncJob(Long jobId, Integer sourceId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate) {
        doExecuteSyncJob(jobId, sourceId, customQuery, timeRange, fromDate, toDate, false);
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void executeScheduledSyncJob(Long jobId, Integer sourceId) {
        doExecuteSyncJob(jobId, sourceId, null, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange.DAY, null, null, true);
    }

    // onlyExistingTopics=true (scheduled background sync): only re-checks topics that already
    // have papers, instead of sweeping the full taxonomy — see triggerScheduledSync().
    private void doExecuteSyncJob(Long jobId, Integer sourceId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate, boolean onlyExistingTopics) {
        SyncJob job = syncJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        ApiSource source = apiSourceRepository.findById(sourceId).orElse(null);
        if (source == null) return;

        LocalDate cutoffDate = null;
        LocalDate endDate = null;
        try {
            if (fromDate != null && !fromDate.isBlank()) {
                cutoffDate = LocalDate.parse(fromDate);
            }
            if (toDate != null && !toDate.isBlank()) {
                endDate = LocalDate.parse(toDate);
            }
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse fromDate or toDate, ignoring them", e);
        }

        if (cutoffDate == null && timeRange != null) {
            switch (timeRange) {
                case DAY: cutoffDate = LocalDate.now().minusDays(1); break;
                case WEEK: cutoffDate = LocalDate.now().minusWeeks(1); break;
                case MONTH: cutoffDate = LocalDate.now().minusMonths(1); break;
                case ALL: cutoffDate = null; break;
            }
        }

        try {
            // Implementation Plan v3: OpenAlex "Sync All" (no custom query) now iterates the
            // official 4,516-topic taxonomy by ID (structured filter) instead of free-text
            // search, so coverage isn't biased toward whatever ad-hoc topics/keywords already
            // exist in the DB. Custom-query syncs and Semantic Scholar keep the original
            // free-text query behavior untouched below.
            boolean structuredOpenAlex = "OpenAlex".equalsIgnoreCase(source.getSourceName())
                    && (customQuery == null || customQuery.trim().isEmpty());

            Set<String> queries = new LinkedHashSet<>();
            List<Topic> officialTopics = new ArrayList<>();
            if (structuredOpenAlex) {
                officialTopics = onlyExistingTopics
                        ? topicRepository.findAllOfficialTopicsWithExistingPapers()
                        : topicRepository.findAllByOpenalexIdIsNotNull();
                // So the frontend can show "processed / total official topics" progress
                job.setTotalTopicsCount(officialTopics.size());
                job.setProcessedTopicsCount(0);
                syncJobRepository.save(job);
            } else if (customQuery != null && !customQuery.trim().isEmpty()) {
                // Comma-separated custom queries (e.g. "AI, machine learning") each run as their
                // own independent search, same as the multi-query fallback below.
                for (String part : customQuery.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        queries.add(trimmed);
                    }
                }
            } else {
                // Cover ALL topics (or, for scheduled runs, only topics that already have papers)
                List<Topic> allTopics = onlyExistingTopics
                        ? topicRepository.findAllOfficialTopicsWithExistingPapers()
                        : topicRepository.findAll();
                for (Topic topic : allTopics) {
                    if (topic.getTopicName() != null && !topic.getTopicName().isBlank()) {
                        queries.add(topic.getTopicName().trim());
                    }
                }
                List<Object[]> topKeywords = keywordRepository.findTop50TrendingKeywordNamesWithCount();
                for (Object[] row : topKeywords) {
                    if (row.length > 0 && row[0] instanceof String) {
                        queries.add(((String) row[0]).trim());
                    }
                }
                if (queries.isEmpty()) {
                    queries.addAll(List.of("Artificial Intelligence", "Machine Learning", "Data Science", "Computer Science",
                            "Environmental Science", "Economics", "Medicine", "Biology", "Physics", "Chemistry"));
                }
            }

            if (!structuredOpenAlex) {
                // Progress for the free-text path (customQuery = 1 query; Semantic Scholar /
                // all-topics fallback = many): reuses the same total/processed counters as the
                // structured OpenAlex path above, just counting queries instead of topics.
                job.setTotalTopicsCount(queries.size());
                job.setProcessedTopicsCount(0);
                syncJobRepository.save(job);
            }

            // Thread-safe ID caches to prevent duplicate entity insertion across concurrent threads
            // Stores IDs instead of full entity objects to avoid detached entity issues across transactions
            java.util.concurrent.ConcurrentHashMap<String, Long> authorIdCache = new java.util.concurrent.ConcurrentHashMap<>();
            java.util.concurrent.ConcurrentHashMap<String, Integer> topicIdCache = new java.util.concurrent.ConcurrentHashMap<>();
            java.util.concurrent.ConcurrentHashMap<String, Integer> keywordIdCache = new java.util.concurrent.ConcurrentHashMap<>();
            java.util.concurrent.ConcurrentHashMap<String, Integer> journalIdCache = new java.util.concurrent.ConcurrentHashMap<>();
            java.util.concurrent.ConcurrentHashMap<String, Integer> fieldIdCache = new java.util.concurrent.ConcurrentHashMap<>();

            java.util.concurrent.atomic.AtomicInteger addedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger updatedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger failedQueryCount = new java.util.concurrent.atomic.AtomicInteger(0);
            List<Paper> newPapers = java.util.Collections.synchronizedList(new ArrayList<>());
            Set<String> processedDois = java.util.concurrent.ConcurrentHashMap.newKeySet();
            
            final LocalDate finalCutoffDate = cutoffDate;
            final LocalDate finalEndDate = endDate;
            // Fetch 50 papers per topic for Sync All (covers 100% of all topics in DB), 300 papers for custom query
            final int maxPapersPerQuery = (customQuery != null && !customQuery.trim().isEmpty()) ? 300 : 50;
            
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(5);
            List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

            for (String query : queries) {
                java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    log.info("Starting sync from {} for query: {}", source.getSourceName(), query);
                    int page = 1;
                    int fetchedForQuery = 0;
                    int pageSize = "OpenAlex".equalsIgnoreCase(source.getSourceName()) ? 200 : 50;

                    while (fetchedForQuery < maxPapersPerQuery) {
                        SyncJob currentJobStatus = syncJobRepository.findById(jobId).orElse(null);
                        if (currentJobStatus != null && "CANCELED".equalsIgnoreCase(currentJobStatus.getStatus())) {
                            log.warn("Sync job {} was manually stopped.", jobId);
                            return;
                        }

                        String url = null;
                        String responseBody = null;
                        try {
                            url = buildApiUrl(source, query, page, finalCutoffDate, finalEndDate);
                            responseBody = fetchFromApi(url);
                        } catch (Exception e) {
                            log.error("Error calling API for query {}", query, e);
                            failedQueryCount.incrementAndGet();
                            break;
                        }

                        if (responseBody == null || responseBody.isBlank()) {
                            break;
                        }

                        int[] counts = new int[2];
                        List<Paper> batchNewPapers = new ArrayList<>();
                        try {
                            boolean continuePagination = applicationContext
                                    .getBean(SyncServiceImpl.class)
                                    .saveResultsInTransaction(
                                            responseBody,
                                            source,
                                            query,
                                            counts,
                                            batchNewPapers,
                                            finalCutoffDate,
                                            processedDois,
                                            authorIdCache,
                                            topicIdCache,
                                            keywordIdCache,
                                            journalIdCache,
                                            fieldIdCache,
                                            jobId);

                            newPapers.addAll(batchNewPapers);
                            addedCount.addAndGet(counts[0]);
                            updatedCount.addAndGet(counts[1]);
                            fetchedForQuery += pageSize;

                            // Skip older papers
                            if (!continuePagination) {
                                log.info("Short-circuiting pagination for query {} as we reached items older than cutoffDate.", query);
                                break;
                            }

                            // Break loop if no new or updated records were found in this batch
                            if (counts[0] == 0 && counts[1] == 0) {
                                break;
                            }
                        } catch (Exception e) {
                            log.error("Failed to save data batch for query {}. Stopping pagination for this query.", query, e);
                            failedQueryCount.incrementAndGet();
                            break;
                        }

                        page++;
                        // Prevent HTTP 429
                        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                    if (fetchedForQuery >= maxPapersPerQuery) {
                        log.info("Reached max paper budget ({}) for query: {}", maxPapersPerQuery, query);
                    }

                    try {
                        syncJobRepository.incrementProcessedTopicsCount(jobId);
                    } catch (Exception e) {
                        log.warn("Failed to increment processed query count for job {}", jobId, e);
                    }
                }, executor);
                futures.add(future);
            }

            for (Topic officialTopic : officialTopics) {
                final Integer officialTopicId = officialTopic.getTopicId();
                final String officialTopicOpenalexId = officialTopic.getOpenalexId();
                final String officialTopicLabel = officialTopicOpenalexId + " (" + officialTopic.getTopicName() + ")";

                java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    log.info("Starting structured sync from {} for topic: {}", source.getSourceName(), officialTopicLabel);
                    int page = 1;
                    int fetchedForQuery = 0;
                    int pageSize = 200; // OpenAlex only in this branch

                    while (fetchedForQuery < maxPapersPerQuery) {
                        SyncJob currentJobStatus = syncJobRepository.findById(jobId).orElse(null);
                        if (currentJobStatus != null && "CANCELED".equalsIgnoreCase(currentJobStatus.getStatus())) {
                            log.warn("Sync job {} was manually stopped.", jobId);
                            return;
                        }

                        String url;
                        String responseBody;
                        try {
                            url = buildStructuredOpenAlexUrl(source, officialTopicOpenalexId, page, finalCutoffDate, finalEndDate);
                            responseBody = fetchFromApi(url);
                        } catch (Exception e) {
                            log.error("Error calling API for topic {}", officialTopicLabel, e);
                            failedQueryCount.incrementAndGet();
                            break;
                        }

                        if (responseBody == null || responseBody.isBlank()) {
                            break;
                        }

                        int[] counts = new int[2];
                        List<Paper> batchNewPapers = new ArrayList<>();
                        try {
                            boolean continuePagination = applicationContext
                                    .getBean(SyncServiceImpl.class)
                                    .saveResultsInTransaction(
                                            responseBody,
                                            source,
                                            officialTopicId,
                                            counts,
                                            batchNewPapers,
                                            finalCutoffDate,
                                            processedDois,
                                            authorIdCache,
                                            topicIdCache,
                                            keywordIdCache,
                                            journalIdCache,
                                            fieldIdCache,
                                            jobId);

                            newPapers.addAll(batchNewPapers);
                            addedCount.addAndGet(counts[0]);
                            updatedCount.addAndGet(counts[1]);
                            fetchedForQuery += pageSize;

                            if (!continuePagination) {
                                log.info("Short-circuiting pagination for topic {} as we reached items older than cutoffDate.", officialTopicLabel);
                                break;
                            }
                            if (counts[0] == 0 && counts[1] == 0) {
                                break;
                            }
                        } catch (Exception e) {
                            log.error("Failed to save data batch for topic {}. Stopping pagination for this topic.", officialTopicLabel, e);
                            failedQueryCount.incrementAndGet();
                            break;
                        }

                        page++;
                        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                    if (fetchedForQuery >= maxPapersPerQuery) {
                        log.info("Reached max paper budget ({}) for topic: {}", maxPapersPerQuery, officialTopicLabel);
                    }

                    // Backfill resumability: mark this topic as swept so a later re-run only
                    // needs to process topics that haven't been synced yet.
                    try {
                        topicRepository.findById(officialTopicId).ifPresent(t -> {
                            t.setLastSyncedAt(LocalDateTime.now());
                            topicRepository.save(t);
                        });
                    } catch (Exception e) {
                        log.warn("Failed to update lastSyncedAt for topic {}", officialTopicLabel, e);
                    }

                    // Atomic UPDATE, not read-modify-write: many topic futures increment this
                    // concurrently for the same job.
                    try {
                        syncJobRepository.incrementProcessedTopicsCount(jobId);
                    } catch (Exception e) {
                        log.warn("Failed to increment processed topic count for job {}", jobId, e);
                    }
                }, executor);
                futures.add(future);
            }

            try {
                // Đợi tất cả các luồng chạy xong
                java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
            } finally {
                executor.shutdown();
            }

            // FIX #4: PARTIAL_SUCCESS when some queries failed but data was still saved
            int totalQueries = structuredOpenAlex ? officialTopics.size() : queries.size();
            int failed = failedQueryCount.get();
            if (failed > 0 && (addedCount.get() > 0 || updatedCount.get() > 0)) {
                job.setStatus("PARTIAL_SUCCESS");
                job.setErrorMessage(failed + "/" + totalQueries + " queries failed (rate limit or API error). Data from successful queries was saved.");
            } else if (failed > 0 && addedCount.get() == 0 && updatedCount.get() == 0) {
                job.setStatus("FAILED");
                job.setErrorMessage("All " + failed + " queries failed. No data was saved.");
            } else {
                job.setStatus("SUCCESS");
            }
            job.setAddedCount(addedCount.get());
            job.setUpdatedCount(updatedCount.get());
            job.setFinishedAt(LocalDateTime.now());
            // job.processedTopicsCount in memory has been stale at its initial value (0) since
            // before the futures ran — they update it via a separate atomic UPDATE query, not
            // through this entity. Refresh it here so this final save doesn't clobber their work.
            syncJobRepository.findById(job.getSyncJobId())
                    .ifPresent(fresh -> job.setProcessedTopicsCount(fresh.getProcessedTopicsCount()));
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
            // Extract meaningful error message
            String errorMsg = ex.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                Throwable cause = ex.getCause();
                errorMsg = cause != null ? cause.getMessage() : ex.getClass().getSimpleName();
            }
            // Truncate to fit DB column (max 500 chars)
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 497) + "...";
            }
            job.setErrorMessage(errorMsg);
            job.setFinishedAt(LocalDateTime.now());
            syncJobRepository.findById(job.getSyncJobId())
                    .ifPresent(fresh -> job.setProcessedTopicsCount(fresh.getProcessedTopicsCount()));
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
    @Transactional(readOnly = true)
    public Page<com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobPaperResponse> getSyncJobPapers(Long jobId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return paperRepository.findByLastSyncJobIdOrderByUpdatedAtDesc(jobId, pageable)
                .map(p -> com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobPaperResponse.builder()
                        .paperId(p.getPaperId())
                        .title(p.getTitle())
                        .action(p.getLastSyncAction())
                        .journalName(p.getJournal() != null ? p.getJournal().getName() : null)
                        .publicationYear(p.getPublicationYear())
                        .doi(p.getDoi())
                        .updatedAt(p.getUpdatedAt())
                        .build());
    }

    @Override
    public SyncJobResponse retrySyncJob(Long jobId, Long userId) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.SYNC_JOB_NOT_FOUND));
        return syncFromSource(job.getApiSource().getSourceId(), userId, null, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange.ALL, null, null);
    }

    @Transactional
    public boolean saveResultsInTransaction(String responseBody, ApiSource source, String searchQuery, int[] counts, List<Paper> newPapers, LocalDate cutoffDate, Set<String> processedDois,
            java.util.concurrent.ConcurrentHashMap<String, Long> authorIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> topicIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> keywordIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> journalIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> fieldIdCache,
            Long jobId) {
        try {
            // Use cache for keyword/topic/field ID lookup
            String searchKey = searchQuery.trim().toLowerCase();
            Integer keywordId = findOrCreateId(keywordIdCache, searchKey,
                    () -> keywordRepository.findFirstByKeywordNameIgnoreCase(searchQuery).map(Keyword::getKeywordId),
                    () -> keywordRepository.save(Keyword.builder().keywordName(searchQuery).build()).getKeywordId());
            Keyword searchKeyword = entityManager.getReference(Keyword.class, keywordId);

            Integer topicId = findCachedId(topicIdCache, searchKey,
                    () -> topicRepository.findFirstByTopicNameIgnoreCase(searchQuery).map(Topic::getTopicId));
            Topic topic = topicId != null ? entityManager.getReference(Topic.class, topicId) : null;

            Integer fieldId = findOrCreateId(fieldIdCache, searchKey,
                    () -> researchFieldRepository.findFirstByFieldNameIgnoreCase(searchQuery).map(ResearchField::getFieldId),
                    () -> researchFieldRepository.save(ResearchField.builder().fieldName(searchQuery).build()).getFieldId());
            ResearchField researchField = entityManager.getReference(ResearchField.class, fieldId);

            boolean continuePagination = true;
            if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
                continuePagination = parseAndSaveOpenAlex(responseBody, source, topic, searchKeyword, researchField, counts, newPapers, cutoffDate, processedDois, authorIdCache, topicIdCache, keywordIdCache, journalIdCache, jobId);
            } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
                continuePagination = parseAndSaveSemanticScholar(responseBody, source, topic, searchKeyword, researchField, counts, newPapers, cutoffDate, processedDois, authorIdCache, topicIdCache, keywordIdCache, journalIdCache, jobId);
            }

            // Prevent OOM during bulk insert
            entityManager.flush();
            entityManager.clear();
            
            return continuePagination;
        } catch (Exception e) {
            log.error("Error saving data in transactional helper", e);
            throw new RuntimeException("DB transaction error during sync: " + e.getMessage(), e);
        }
    }

    // Structured-filter overload (Implementation Plan v3): used only for OpenAlex "Sync All"
    // against the official topic taxonomy, where the topic is already known by ID — no
    // name-based lookup/auto-create needed. searchKeyword tagging is skipped (unused downstream
    // in saveOrUpdatePaper); the paper's ResearchField is derived from the topic's official
    // Field tier instead of an ad-hoc name.
    @Transactional
    public boolean saveResultsInTransaction(String responseBody, ApiSource source, Integer officialTopicId, int[] counts, List<Paper> newPapers, LocalDate cutoffDate, Set<String> processedDois,
            java.util.concurrent.ConcurrentHashMap<String, Long> authorIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> topicIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> keywordIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> journalIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> fieldIdCache,
            Long jobId) {
        try {
            Topic officialTopic = topicRepository.findById(officialTopicId).orElse(null);
            if (officialTopic == null) {
                return true; // topic vanished mid-run (unlikely); skip gracefully without breaking the job
            }

            // Fetch/create the ResearchField directly (not via the shared fieldIdCache +
            // entityManager.getReference): many topics share the same official Field name, and
            // caching an optimistically-created ID here previously went stale whenever the
            // transaction that first cached it rolled back for an unrelated reason later in the
            // same call — leaving other topics to reference a row that was never actually
            // committed, which broke the papers -> research_fields FK on insert.
            TopicField topicField = officialTopic.getSubfield() != null ? officialTopic.getSubfield().getField() : null;
            String fieldName = topicField != null ? topicField.getDisplayName() : officialTopic.getTopicName();
            ResearchField researchField = researchFieldRepository.findFirstByFieldNameIgnoreCase(fieldName)
                    .orElseGet(() -> researchFieldRepository.save(ResearchField.builder().fieldName(fieldName).build()));

            boolean continuePagination = parseAndSaveOpenAlex(responseBody, source, officialTopic, null, researchField, counts, newPapers, cutoffDate, processedDois, authorIdCache, topicIdCache, keywordIdCache, journalIdCache, jobId);

            entityManager.flush();
            entityManager.clear();

            return continuePagination;
        } catch (Exception e) {
            log.error("Error saving data in transactional helper for official topic id {}", officialTopicId, e);
            throw new RuntimeException("DB transaction error during structured sync: " + e.getMessage(), e);
        }
    }

    private String buildApiUrl(ApiSource source, String query, int page, LocalDate cutoffDate, LocalDate endDate) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
            // FIX #5: Use fixed mailto for OpenAlex polite pool identification
            String url = source.getBaseUrl() + "/works?search=" + encodedQuery + "&per-page=200&page=" + page + "&mailto=ptt.sync@university.edu";
            
            if (cutoffDate != null || endDate != null) {
                String filterFrom = cutoffDate != null ? cutoffDate.toString() : "";
                String filterTo = endDate != null ? endDate.toString() : "";
                
                if (!filterFrom.isEmpty() && !filterTo.isEmpty()) {
                    url += "&filter=from_publication_date:" + filterFrom + ",to_publication_date:" + filterTo;
                } else if (!filterFrom.isEmpty()) {
                    url += "&filter=from_publication_date:" + filterFrom;
                } else if (!filterTo.isEmpty()) {
                    url += "&filter=to_publication_date:" + filterTo;
                }
            }
            url += "&sort=publication_date:desc";
            return url;
        } else if ("Semantic Scholar".equalsIgnoreCase(source.getSourceName())) {
            int offset = (page - 1) * 50;
            String url = source.getBaseUrl() + "/v1/paper/search?query=" + encodedQuery + "&limit=50&offset=" + offset + "&fields=title,abstract,authors,journal,year,externalIds,citationCount,fieldsOfStudy";
            
            if (cutoffDate != null || endDate != null) {
                String fromYear = cutoffDate != null ? String.valueOf(cutoffDate.getYear()) : "";
                String toYear = endDate != null ? String.valueOf(endDate.getYear()) : "";
                url += "&year=" + fromYear + "-" + toYear;
                if (cutoffDate != null && endDate == null) {
                     url += ",publicationDate";
                }
            }
            url += "&sort=publicationDate:desc";
            return url;
        }
        throw new IllegalArgumentException("Unsupported source name: " + source.getSourceName());
    }

    // Structured filter (Implementation Plan v3): used only for OpenAlex "Sync All" runs against
    // the official topic taxonomy. Iterates by canonical topic ID instead of free-text search=,
    // so coverage matches the seeded 4,516-topic taxonomy exactly. Custom-query syncs and
    // Semantic Scholar keep using buildApiUrl() above, unchanged.
    private String buildStructuredOpenAlexUrl(ApiSource source, String topicOpenalexId, int page, LocalDate cutoffDate, LocalDate endDate) {
        StringBuilder filter = new StringBuilder("topics.id:").append(topicOpenalexId);
        if (cutoffDate != null) {
            filter.append(",from_publication_date:").append(cutoffDate);
        }
        if (endDate != null) {
            filter.append(",to_publication_date:").append(endDate);
        }
        return source.getBaseUrl() + "/works?filter=" + filter
                + "&sort=publication_date:desc&per-page=200&page=" + page + "&mailto=ptt.sync@university.edu";
    }

    private String extractShortId(String fullUri) {
        if (fullUri == null || fullUri.isBlank()) return null;
        int idx = fullUri.lastIndexOf('/');
        return idx >= 0 ? fullUri.substring(idx + 1) : fullUri;
    }

    // Deliberately NOT ConcurrentHashMap.computeIfAbsent(): its mapping function runs while the
    // map holds an internal per-bin lock, and ours does a blocking JDBC call. Under concurrent
    // sync threads this deadlocked the whole pool — one thread stuck waiting on a slow DB
    // round-trip while others blocked on the Java-level monitor for the same/colliding bin.
    // This does the DB work outside any lock and only briefly touches the map via putIfAbsent.
    private <ID> ID findOrCreateId(
            java.util.concurrent.ConcurrentHashMap<String, ID> cache,
            String cacheKey,
            java.util.function.Supplier<java.util.Optional<ID>> finder,
            java.util.function.Supplier<ID> creator) {
        ID cached = cache.get(cacheKey);
        if (cached != null) return cached;

        ID id = finder.get().orElseGet(() -> {
            try {
                return creator.get();
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Lost a race with another thread inserting the same unique name/value;
                // the row now exists, so re-fetch it instead of failing this paper.
                return finder.get().orElseThrow(() -> e);
            }
        });

        ID existing = cache.putIfAbsent(cacheKey, id);
        return existing != null ? existing : id;
    }

    // Read-only counterpart (no creator): used for lookups that should NOT auto-create a row
    // (e.g. matching a canonical Topic by name/ID). Absence is never cached, matching the
    // previous computeIfAbsent behavior (a null mapping result is never recorded).
    private <ID> ID findCachedId(
            java.util.concurrent.ConcurrentHashMap<String, ID> cache,
            String cacheKey,
            java.util.function.Supplier<java.util.Optional<ID>> finder) {
        ID cached = cache.get(cacheKey);
        if (cached != null) return cached;

        ID id = finder.get().orElse(null);
        if (id != null) {
            ID existing = cache.putIfAbsent(cacheKey, id);
            return existing != null ? existing : id;
        }
        return null;
    }

    private static long lastApiCallTime = 0;
    private synchronized void rateLimitApi() {
        long now = System.currentTimeMillis();
        long diff = now - lastApiCallTime;
        if (diff < 200) { // Max 5 requests per second globally
            try {
                Thread.sleep(200 - diff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastApiCallTime = System.currentTimeMillis();
    }

    private String fetchFromApi(String url) {
        boolean isOpenAlex = url.contains("api.openalex.org");
        // Extra headroom so there's an attempt left after rotating through every configured key
        int maxRetries = isOpenAlex ? Math.max(3, 1 + openAlexKeyRotator.keyCount()) : 3;
        for (int i = 0; i < maxRetries; i++) {
            String requestUrl = url;
            if (isOpenAlex) {
                String key = openAlexKeyRotator.getCurrentKey();
                if (key != null) {
                    requestUrl += "&api_key=" + key;
                }
            }
            try {
                rateLimitApi(); // Prevent 429 Too Many Requests
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);
                return response.getBody();
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                String responseText = e.getResponseBodyAsString();
                log.error("API rate limit exceeded (429) on attempt {}: {}", i + 1, responseText != null && !responseText.isBlank() ? responseText : e.getMessage());
                // If OpenAlex's daily budget is exhausted for this key, rotate to the next
                // configured key (if any) and retry immediately instead of failing the job.
                if (isOpenAlex && responseText != null && responseText.contains("Insufficient budget")) {
                    if (openAlexKeyRotator.hasMoreKeys()) {
                        openAlexKeyRotator.rotateToNextKey();
                        continue;
                    }
                    throw new RuntimeException("OpenAlex daily API quota/budget exceeded on all configured keys. Resets at midnight UTC. Try Semantic Scholar or try again later.", e);
                }
                if (i == maxRetries - 1) {
                    throw new RuntimeException("API Rate Limit Exceeded (429) after " + maxRetries + " attempts.", e);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                log.warn("API request failed (attempt {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i == maxRetries - 1) {
                    throw new RuntimeException("API request failed after " + maxRetries + " attempts: " + e.getMessage(), e);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        return null;
    }

    private boolean parseAndSaveOpenAlex(
            String jsonResponse,
            ApiSource source,
            Topic topic,
            Keyword searchKeyword,
            ResearchField researchField,
            int[] counts,
            List<Paper> newPapers,
            LocalDate cutoffDate,
            Set<String> processedDois,
            java.util.concurrent.ConcurrentHashMap<String, Long> authorIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> topicIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> keywordIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> journalIdCache,
            Long jobId) throws Exception {
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
                // FIX #2: Don't default to current year — null is better for trend accuracy
                int rawYear = work.path("publication_year").asInt(0);
                dto.year = rawYear > 0 ? rawYear : null;
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
                // Concepts (legacy taxonomy) are kept only for keyword extraction (level > 1).
                // Topic assignment now comes from the "topics" array (canonical taxonomy) below.
                JsonNode concepts = work.path("concepts");
                if (concepts.isArray()) {
                    for (JsonNode concept : concepts) {
                        int level = concept.path("level").asInt(99);
                        String conceptName = concept.path("display_name").asText(null);
                        if (level > 1 && conceptName != null && !conceptName.isBlank()) {
                            dto.keywordNames.add(conceptName.trim());
                        }
                    }
                }
                JsonNode topicsArray = work.path("topics");
                if (topicsArray.isArray()) {
                    for (JsonNode topicEntry : topicsArray) {
                        String openalexTopicId = extractShortId(topicEntry.path("id").asText(null));
                        if (openalexTopicId != null) {
                            dto.topicOpenalexIds.add(openalexTopicId);
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

                saveOrUpdatePaper(dto, topic, searchKeyword, researchField, source, counts, newPapers, processedDois, authorIdCache, topicIdCache, keywordIdCache, journalIdCache, jobId);
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
            LocalDate cutoffDate,
            Set<String> processedDois,
            java.util.concurrent.ConcurrentHashMap<String, Long> authorIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> topicIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> keywordIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> journalIdCache,
            Long jobId) throws Exception {
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
                // FIX #2: Don't default to current year — null is better for trend accuracy
                dto.year = year > 0 ? year : null;
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

                saveOrUpdatePaper(dto, topic, searchKeyword, researchField, source, counts, newPapers, processedDois, authorIdCache, topicIdCache, keywordIdCache, journalIdCache, jobId);
            }
        }
        return true;
    }

    // Thread-safe ID caching + entityManager.getReference() to attached entities in active transaction
    private void saveOrUpdatePaper(ParsedPaperDTO dto, Topic topic, Keyword searchKeyword, ResearchField researchField, ApiSource source, int[] counts, List<Paper> newPapers, Set<String> processedDois,
            java.util.concurrent.ConcurrentHashMap<String, Long> authorIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> topicIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> keywordIdCache,
            java.util.concurrent.ConcurrentHashMap<String, Integer> journalIdCache,
            Long jobId) {
        if (dto.doi != null && !dto.doi.isBlank()) {
            if (!processedDois.add(dto.doi.trim().toLowerCase())) return;
        } else if (dto.title != null && !dto.title.isBlank()) {
            if (!processedDois.add(dto.title.trim().toLowerCase())) return;
        }

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
            if (dto.doi != null && !dto.doi.isBlank() && (paper.getDoi() == null || paper.getDoi().isBlank())) {
                paper.setDoi(dto.doi.trim());
            }
            if (dto.paperAbstract != null && !dto.paperAbstract.isBlank()) paper.setPaperAbstract(dto.paperAbstract);
            paper.setCitationCount(dto.citations);
            paper.setTitle(dto.title.trim());
            paper.setSourceUrl(dto.sourceUrl);
            counts[1]++;
        }

        paper.setLastSyncJobId(jobId);
        paper.setLastSyncAction(isNew ? "ADDED" : "UPDATED");

        // Journal ID lookup + reference
        if (dto.journalName != null && !dto.journalName.isBlank()) {
            String jKey = dto.journalName.trim().toLowerCase();
            Integer journalId = findOrCreateId(journalIdCache, jKey,
                    () -> journalRepository.findFirstByNameIgnoreCase(dto.journalName.trim()).map(Journal::getJournalId),
                    () -> journalRepository.save(Journal.builder().name(dto.journalName.trim()).build()).getJournalId());
            paper.setJournal(entityManager.getReference(Journal.class, journalId));
        }

        // Author ID lookup + reference
        Set<Author> paperAuthors = new HashSet<>();
        for (String aName : dto.authorNames) {
            String aKey = aName.trim().toLowerCase();
            Long authorId = findOrCreateId(authorIdCache, aKey,
                    () -> authorRepository.findFirstByFullNameIgnoreCase(aName.trim()).map(Author::getAuthorId),
                    () -> authorRepository.save(Author.builder().fullName(aName.trim()).build()).getAuthorId());
            paperAuthors.add(entityManager.getReference(Author.class, authorId));
        }
        paper.setAuthors(paperAuthors);

        // Keyword ID lookup + reference
        Set<Keyword> keywords = new HashSet<>(paper.getKeywords() != null ? paper.getKeywords() : new HashSet<>());
        for (String kwName : dto.keywordNames) {
            String kwKey = kwName.trim().toLowerCase();
            Integer kwId = findOrCreateId(keywordIdCache, kwKey,
                    () -> keywordRepository.findFirstByKeywordNameIgnoreCase(kwName.trim()).map(Keyword::getKeywordId),
                    () -> keywordRepository.save(Keyword.builder().keywordName(kwName.trim()).build()).getKeywordId());
            keywords.add(entityManager.getReference(Keyword.class, kwId));
        }
        paper.setKeywords(keywords);

        // Topic ID lookup + reference — canonical topics only, matched by OpenAlex ID.
        // Unlike keywords/journals/authors, we never auto-create a Topic here: every ID in
        // dto.topicOpenalexIds should already exist from the official taxonomy seed. If OpenAlex
        // returns an ID we don't recognize (taxonomy updated since our last seed), we skip it
        // rather than insert an incomplete ad-hoc row missing hierarchy data.
        Set<Topic> topicsSet = new HashSet<>(paper.getTopics() != null ? paper.getTopics() : new HashSet<>());
        if (topic != null) topicsSet.add(topic);
        for (String openalexTopicId : dto.topicOpenalexIds) {
            Integer tId = findCachedId(topicIdCache, openalexTopicId,
                    () -> topicRepository.findByOpenalexId(openalexTopicId).map(Topic::getTopicId));
            if (tId != null) {
                topicsSet.add(entityManager.getReference(Topic.class, tId));
            }
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
                .totalTopicsCount(job.getTotalTopicsCount())
                .processedTopicsCount(job.getProcessedTopicsCount())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }
}
