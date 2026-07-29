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
        public Boolean isOpenAccess;
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
    private final com.publication_trend_tracking_system.sever_web_app.service.DashboardService dashboardService;
    private final com.publication_trend_tracking_system.sever_web_app.config.OpenAlexKeyRotator openAlexKeyRotator;
    private final com.publication_trend_tracking_system.sever_web_app.repository.SchedulerSettingsRepository schedulerSettingsRepository;
    private final com.publication_trend_tracking_system.sever_web_app.config.SyncJobState syncJobState;

    private static final Integer SCHEDULER_SETTINGS_ID = 1;

    // How many papers to pull per topic/query. Small by default: the demo environment cares more
    // about a sync finishing quickly than about exhaustive coverage.
    @org.springframework.beans.factory.annotation.Value("${app.sync.max-papers-per-topic:20}")
    private int maxPapersPerTopic;

    // Ceiling on how many official topics a single manual "Sync All" sweeps. The full taxonomy is
    // ~4,500 topics, which at the API's polite rate limit takes far too long to sit through during
    // a demo. Set to 0 or below for no limit. Scheduled runs deliberately ignore this — see
    // doExecuteSyncJob().
    @org.springframework.beans.factory.annotation.Value("${app.sync.max-topics-per-job:200}")
    private int maxTopicsPerJob;

    @org.springframework.beans.factory.annotation.Value("${app.sync.scheduler-enabled-by-default:false}")
    private boolean schedulerEnabledByDefault;

    @org.springframework.beans.factory.annotation.Value("${app.sync.job-timeout-minutes:60}")
    private int jobTimeoutMinutes;

    @org.springframework.beans.factory.annotation.Value("${app.sync.worker-threads:2}")
    private int syncWorkerThreads;

    @org.springframework.beans.factory.annotation.Value("${app.sync.topics-per-request:25}")
    private int topicsPerRequest;

    @org.springframework.beans.factory.annotation.Value("${app.sync.papers-per-request:50}")
    private int papersPerRequest;

    // Must stay wide enough to cover the interval between two app.sync.cron firings.
    @org.springframework.beans.factory.annotation.Value("${app.sync.scheduled-time-range:WEEK}")
    private String scheduledTimeRangeName;

    // Guards "is a job already RUNNING for this source?" together with the insert that follows.
    // Apart they are a check-then-act race: two admins clicking Sync at the same moment both saw
    // no running job and both started one.
    private final Object jobCreationLock = new Object();

    @Override
    @Transactional(readOnly = true)
    public boolean isSchedulerEnabled() {
        // Once an admin uses the toggle a row exists and it decides. Before that the configured
        // default applies, and that default is off: an install nobody has configured should not
        // start syncing on its own in the middle of the night.
        return schedulerSettingsRepository.findById(SCHEDULER_SETTINGS_ID)
                .map(com.publication_trend_tracking_system.sever_web_app.entity.SchedulerSettings::isEnabled)
                .orElse(schedulerEnabledByDefault);
    }

    @Override
    @Transactional
    public int failStaleRunningJobs() {
        if (jobTimeoutMinutes <= 0) {
            return 0;
        }
        List<SyncJob> stale = syncJobRepository.findStaleRunningJobs(
                LocalDateTime.now().minusMinutes(jobTimeoutMinutes));
        if (stale.isEmpty()) {
            return 0;
        }
        for (SyncJob staleJob : stale) {
            // Raise the cancel flag too: if the thread is somehow still alive, this stops it at the
            // next page instead of leaving it writing to a job already marked FAILED.
            syncJobState.requestCancel(staleJob.getSyncJobId());
            staleJob.setStatus("FAILED");
            staleJob.setErrorMessage("No longer running after " + jobTimeoutMinutes
                    + " minutes; released so new syncs are not blocked");
            staleJob.setFinishedAt(LocalDateTime.now());
        }
        syncJobRepository.saveAll(stale);
        log.warn("Released {} stalled sync job(s) that had been RUNNING for over {} minutes.",
                stale.size(), jobTimeoutMinutes);
        return stale.size();
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
            // Raise the live signal first: worker threads poll this, so they stop at their next
            // page boundary regardless of when the status write below lands.
            syncJobState.requestCancel(jobId);
            job.setStatus("CANCELED");
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorMessage("Manually stopped by Admin");
            syncJobRepository.save(job);
        }
    }

    @Override
    @Transactional
    public void deleteSyncJob(Long jobId) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.SYNC_JOB_NOT_FOUND));

        if ("RUNNING".equalsIgnoreCase(job.getStatus())) {
            throw new AppException(ErrorCode.SYNC_JOB_STILL_RUNNING);
        }

        // Papers keep their own rows — they are real synced data that other jobs may also have
        // touched, so removing a job's history must not delete them. Only the back-reference is
        // cleared, otherwise papers would point at a job id that no longer exists.
        paperRepository.clearLastSyncJobId(jobId);
        syncJobRepository.delete(job);
        syncJobState.clear(jobId);
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
        SyncJob job;
        synchronized (jobCreationLock) {
            if (syncJobRepository.existsByApiSource_SourceIdAndStatus(sourceId, "RUNNING")) {
                throw new AppException(ErrorCode.SYNC_JOB_ALREADY_RUNNING);
            }

            job = syncJobRepository.save(SyncJob.builder()
                    .apiSource(source)
                    .triggeredBy(null)
                    .status("RUNNING")
                    .startedAt(LocalDateTime.now())
                    // Recording the window keeps a later Retry from turning into an unbounded
                    // full sweep.
                    .timeRange(scheduledTimeRange().name())
                    .build());
        }

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

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        SyncJob job;
        synchronized (jobCreationLock) {
            if (syncJobRepository.existsByApiSource_SourceIdAndStatus(sourceId, "RUNNING")) {
                throw new AppException(ErrorCode.SYNC_JOB_ALREADY_RUNNING);
            }

            job = syncJobRepository.save(SyncJob.builder()
                    .apiSource(source)
                    .triggeredBy(user)
                    .status("RUNNING")
                    .startedAt(LocalDateTime.now())
                    // Recorded so retrySyncJob() can re-run this exact job rather than guessing.
                    .customQuery(customQuery)
                    .timeRange(timeRange != null ? timeRange.name() : null)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .build());
        }

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
        doExecuteSyncJob(jobId, sourceId, null, scheduledTimeRange(), null, null, true);
    }

    /**
     * Look-back window for scheduled runs. This must cover the gap between two firings of
     * {@code app.sync.cron}, otherwise the run silently misses papers: the default cron is weekly
     * but this was hardcoded to DAY, so six days out of every seven were never picked up.
     */
    private com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange scheduledTimeRange() {
        try {
            return com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange
                    .valueOf(scheduledTimeRangeName.trim().toUpperCase());
        } catch (RuntimeException ex) {
            log.warn("Invalid app.sync.scheduled-time-range '{}'; falling back to WEEK.", scheduledTimeRangeName);
            return com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange.WEEK;
        }
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
                if (onlyExistingTopics) {
                    // Scheduled runs stay whole: they are incremental over topics that already have
                    // papers, and capping them would mean the topics past the cut silently stopped
                    // receiving updates.
                    officialTopics = topicRepository.findOfficialTopicsWithExistingPapersAndHierarchy();
                } else if (maxTopicsPerJob > 0) {
                    // Take the slice the taxonomy is most overdue for, so consecutive runs walk
                    // through every topic instead of re-syncing the same first N forever. Each run
                    // stays short; coverage accumulates across runs.
                    officialTopics = topicRepository.findOfficialTopicsLeastRecentlySyncedFirst(
                            org.springframework.data.domain.PageRequest.of(0, maxTopicsPerJob));
                    long totalOfficial = topicRepository.countByOpenalexIdIsNotNull();
                    long neverSynced = totalOfficial - topicRepository.countByOpenalexIdIsNotNullAndLastSyncedAtIsNotNull();
                    log.info("Syncing the {} least-recently-updated of {} official topics"
                                    + " ({} never synced yet).",
                            officialTopics.size(), totalOfficial, neverSynced);
                } else {
                    officialTopics = topicRepository.findAllOfficialTopicsWithHierarchy();
                }

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
            // Circuit breaker. When the upstream API is down or out of quota, every remaining topic
            // would still burn its full retry budget before failing — 200 topics each retrying with
            // backoff turns a dead API into a job that runs for a very long time and achieves
            // nothing. Enough consecutive failures and the rest of the job is abandoned instead.
            java.util.concurrent.atomic.AtomicInteger consecutiveApiFailures = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicBoolean circuitOpen = new java.util.concurrent.atomic.AtomicBoolean(false);
            List<Paper> newPapers = java.util.Collections.synchronizedList(new ArrayList<>());
            // Accumulated independently of newPapers so trend alerts still cover every topic the
            // run touched even if paper retention hits its ceiling. A set of ids stays small.
            Set<Integer> touchedTopicIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
            Set<String> processedDois = java.util.concurrent.ConcurrentHashMap.newKeySet();
            
            final LocalDate finalCutoffDate = cutoffDate;
            final LocalDate finalEndDate = endDate;
            // Papers to pull per topic/query. This used to be hardcoded to 20 while the request
            // asked the API for per-page=200 and the loop counted the *page size* against the
            // budget — so the cap exited after one page and every topic actually pulled 200
            // papers, ten times the documented figure. The page size now follows the budget, so
            // the number here is the number that gets fetched.
            final int maxPapersPerQuery = Math.max(1, maxPapersPerTopic);
            
            // Concurrency is bounded low on purpose. Every worker inserts into the same shared
            // authors/keywords/journals rows, so past a couple of threads they spend more time
            // deadlocking against each other — and redoing the rolled-back work — than they gain
            // from running in parallel. Measured on this dataset, five workers produced deadlocks
            // on more than half the batches; the wall-clock time got worse, not better.
            java.util.concurrent.ExecutorService executor =
                    java.util.concurrent.Executors.newFixedThreadPool(Math.max(1, syncWorkerThreads));
            List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

            for (String query : queries) {
                java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    log.info("Starting sync from {} for query: {}", source.getSourceName(), query);
                    int page = 1;
                    int fetchedForQuery = 0;
                    // Never request more than the budget, and stay within each API's own ceiling
                    // (OpenAlex 200/page, Semantic Scholar 100/page).
                    int apiMaxPageSize = "OpenAlex".equalsIgnoreCase(source.getSourceName()) ? 200 : 100;
                    int pageSize = Math.min(maxPapersPerQuery, apiMaxPageSize);

                    while (fetchedForQuery < maxPapersPerQuery) {
                        // In-memory flag, not a re-read of the job row: this runs before every page
                        // on every worker thread, and the old query per page competed with the
                        // batch saves for connections.
                        if (syncJobState.isCancelRequested(jobId)) {
                            log.warn("Sync job {} was manually stopped.", jobId);
                            return;
                        }
                        if (circuitOpen.get()) {
                            return;
                        }

                        String url = null;
                        String responseBody = null;
                        try {
                            url = buildApiUrl(source, query, page, pageSize, finalCutoffDate, finalEndDate);
                            responseBody = fetchFromApi(url);
                        } catch (Exception e) {
                            log.error("Error calling API for query {}", query, e);
                            failedQueryCount.incrementAndGet();
                            tripCircuitIfUpstreamIsDown(jobId, consecutiveApiFailures, circuitOpen);
                            break;
                        }

                        // A reply means the upstream is answering again, so the failure streak that
                        // feeds the circuit breaker starts over.
                        consecutiveApiFailures.set(0);

                        if (responseBody == null || responseBody.isBlank()) {
                            break;
                        }

                        int[] counts = new int[2];
                        List<Paper> batchNewPapers = new ArrayList<>();
                        try {
                            final String rawResponse = responseBody;
                            boolean continuePagination = saveBatchWithLockRetry(
                                    () -> applicationContext
                                            .getBean(SyncServiceImpl.class)
                                            .saveResultsInTransaction(
                                                    rawResponse,
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
                                                    jobId),
                                    query);

                            collectNewPapers(newPapers, touchedTopicIds, batchNewPapers);
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
                        // Only pause when another page is actually coming. This used to run at the
                        // end of every iteration including the last one, so each query paid a full
                        // second of sleep purely to exit its loop — with one page per topic that
                        // was a wasted second per topic, and it dominated the job's runtime.
                        if (fetchedForQuery < maxPapersPerQuery) {
                            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }
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

            // OpenAlex accepts an OR of topic ids in a single filter, so a batch of N topics costs
            // one request instead of N — the difference between minutes and hours across the full
            // 4,500-topic taxonomy. Batches are built within a single research field: papers still
            // attach to their own topics from their own metadata, and keeping one field per batch
            // means the field stamped on a new paper stays correct.
            List<List<Topic>> topicBatches = batchTopicsByField(officialTopics, topicsPerRequest);
            log.info("Sweeping {} topics in {} batched request(s).", officialTopics.size(), topicBatches.size());

            for (List<Topic> topicBatch : topicBatches) {
                final List<Integer> batchTopicIds = topicBatch.stream().map(Topic::getTopicId).toList();
                final Integer representativeTopicId = batchTopicIds.get(0);
                final String batchFilter = topicBatch.stream()
                        .map(Topic::getOpenalexId)
                        .collect(java.util.stream.Collectors.joining("|"));
                final String officialTopicLabel = topicBatch.size() + " topics starting with "
                        + topicBatch.get(0).getTopicName();
                // Papers pulled per batched request. This is the knob that decides how heavy a run
                // is: the number of topics only costs requests, whereas every paper returned costs
                // parsing and database writes. Letting a batch return a full page of 200 made a
                // 500-topic sweep take over ten minutes even though the requests themselves were
                // fast, because the run then had thousands of papers to write.
                final int batchBudget = Math.min(200, Math.max(1, papersPerRequest));

                java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    log.info("Starting structured sync from {} for {}", source.getSourceName(), officialTopicLabel);
                    int page = 1;
                    int fetchedForQuery = 0;
                    int pageSize = Math.min(batchBudget, 200); // OpenAlex only in this branch

                    while (fetchedForQuery < batchBudget) {
                        // In-memory flag, not a re-read of the job row: this runs before every page
                        // on every worker thread, and the old query per page competed with the
                        // batch saves for connections.
                        if (syncJobState.isCancelRequested(jobId)) {
                            log.warn("Sync job {} was manually stopped.", jobId);
                            return;
                        }
                        if (circuitOpen.get()) {
                            return;
                        }

                        String url;
                        String responseBody;
                        try {
                            url = buildStructuredOpenAlexUrl(source, batchFilter, page, pageSize, finalCutoffDate, finalEndDate);
                            responseBody = fetchFromApi(url);
                        } catch (Exception e) {
                            log.error("Error calling API for topic {}", officialTopicLabel, e);
                            failedQueryCount.incrementAndGet();
                            tripCircuitIfUpstreamIsDown(jobId, consecutiveApiFailures, circuitOpen);
                            break;
                        }

                        // A reply means the upstream is answering again, so the failure streak that
                        // feeds the circuit breaker starts over.
                        consecutiveApiFailures.set(0);

                        if (responseBody == null || responseBody.isBlank()) {
                            break;
                        }

                        int[] counts = new int[2];
                        List<Paper> batchNewPapers = new ArrayList<>();
                        try {
                            final String rawResponse = responseBody;
                            boolean continuePagination = saveBatchWithLockRetry(
                                    () -> applicationContext
                                            .getBean(SyncServiceImpl.class)
                                            .saveResultsInTransaction(
                                                    rawResponse,
                                                    source,
                                                    representativeTopicId,
                                                    counts,
                                                    batchNewPapers,
                                                    finalCutoffDate,
                                                    processedDois,
                                                    authorIdCache,
                                                    topicIdCache,
                                                    keywordIdCache,
                                                    journalIdCache,
                                                    fieldIdCache,
                                                    jobId),
                                    officialTopicLabel);

                            collectNewPapers(newPapers, touchedTopicIds, batchNewPapers);
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
                        // Same as the query branch above: only wait when another page follows.
                        // Sync All runs through this branch, so the wasted second was being paid
                        // once per topic across the whole taxonomy sweep.
                        if (fetchedForQuery < batchBudget) {
                            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }
                    }

                    // Every topic in the batch was covered by the request, so all of them advance
                    // the rotation cursor together.
                    try {
                        topicRepository.markSyncedAll(batchTopicIds, LocalDateTime.now());
                    } catch (Exception e) {
                        log.warn("Failed to update lastSyncedAt for {}", officialTopicLabel, e);
                    }

                    // Atomic UPDATE, not read-modify-write: many batch futures update this
                    // concurrently for the same job.
                    try {
                        syncJobRepository.addProcessedTopicsCount(jobId, batchTopicIds.size());
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

            if (syncJobState.isCancelRequested(jobId)) {
                // stopSyncJob already wrote CANCELED and finishedAt. Falling through to the verdict
                // below would relabel the job SUCCESS — failedQueryCount is zero when the workers
                // simply returned early — and erase the fact that an admin stopped it. Papers saved
                // before the stop are real data and stay; the notification pass is skipped on
                // purpose, since blasting out alerts is exactly the work Stop was pressed to halt.
                log.info("Sync job {} was canceled; keeping CANCELED status and skipping notifications.", jobId);
                syncJobState.clear(jobId);
                return;
            }

            // FIX #4: PARTIAL_SUCCESS when some queries failed but data was still saved
            int totalQueries = structuredOpenAlex ? officialTopics.size() : queries.size();
            int failed = failedQueryCount.get();
            if (circuitOpen.get()) {
                // Distinct from a plain failure count so the log tells an admin the run was cut
                // short deliberately rather than that every single topic happened to fail.
                job.setStatus(addedCount.get() > 0 || updatedCount.get() > 0 ? "PARTIAL_SUCCESS" : "FAILED");
                job.setErrorMessage("Stopped early: the data source stopped responding after "
                        + API_FAILURE_CIRCUIT_THRESHOLD + " consecutive failures. "
                        + "Anything fetched before that was saved.");
            } else if (failed > 0 && (addedCount.get() > 0 || updatedCount.get() > 0)) {
                job.setStatus("PARTIAL_SUCCESS");
                // Deliberately not blaming the API: this counter also covers failures while saving
                // a batch, and saying "rate limit or API error" for a database problem sent
                // diagnosis in the wrong direction. The log has the actual cause per query.
                job.setErrorMessage(failed + "/" + totalQueries
                        + " queries did not complete (see server log for the cause of each)."
                        + " Data from the successful queries was saved.");
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
            // Driven by the ids gathered during the run, not by the retained paper list, so trend
            // alerts stay complete even when paper retention was capped.
            dashboardService.checkAndNotifyTrendingTopics(touchedTopicIds);

            syncJobState.clear(jobId);

        // Throwable, not Exception: an Error such as OutOfMemoryError would otherwise escape without
        // ever writing a final status, leaving the row at RUNNING and blocking every later sync for
        // this source. The row is finalised first, then Errors are rethrown to the thread's handler.
        } catch (Throwable ex) {
            syncJobState.clear(jobId);
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

            // The job row is safely closed out now. An Error means the JVM is in a state this
            // method has no business papering over, so let it continue up the stack.
            if (ex instanceof Error) {
                throw (Error) ex;
            }
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

        // Re-run what the original job actually ran. This previously passed a hardcoded
        // (null query, TimeRange.ALL), so retrying any job — however small — kicked off a full
        // unbounded sweep of the whole taxonomy instead.
        com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange = null;
        if (job.getTimeRange() != null) {
            try {
                timeRange = com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange
                        .valueOf(job.getTimeRange());
            } catch (IllegalArgumentException ex) {
                log.warn("Sync job {} has an unrecognised timeRange '{}'; retrying without one.",
                        jobId, job.getTimeRange());
            }
        }

        return syncFromSource(
                job.getApiSource().getSourceId(),
                userId,
                job.getCustomQuery(),
                timeRange,
                job.getFromDate(),
                job.getToDate());
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

    private String buildApiUrl(ApiSource source, String query, int page, int pageSize, LocalDate cutoffDate, LocalDate endDate) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if ("OpenAlex".equalsIgnoreCase(source.getSourceName())) {
            // FIX #5: Use fixed mailto for OpenAlex polite pool identification
            String url = source.getBaseUrl() + "/works?search=" + encodedQuery + "&per-page=" + pageSize + "&page=" + page + "&mailto=ptt.sync@university.edu";
            
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
            int offset = (page - 1) * pageSize;
            String url = source.getBaseUrl() + "/v1/paper/search?query=" + encodedQuery + "&limit=" + pageSize + "&offset=" + offset + "&fields=title,abstract,authors,journal,year,externalIds,citationCount,fieldsOfStudy,isOpenAccess";
            
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
    private String buildStructuredOpenAlexUrl(ApiSource source, String topicOpenalexId, int page, int pageSize, LocalDate cutoffDate, LocalDate endDate) {
        StringBuilder filter = new StringBuilder("topics.id:").append(topicOpenalexId);
        if (cutoffDate != null) {
            filter.append(",from_publication_date:").append(cutoffDate);
        }
        if (endDate != null) {
            filter.append(",to_publication_date:").append(endDate);
        }
        return source.getBaseUrl() + "/works?filter=" + filter
                + "&sort=publication_date:desc&per-page=" + pageSize + "&page=" + page + "&mailto=ptt.sync@university.edu";
    }

    // Upper bound on how many new Paper entities a single job keeps in memory for the notification
    // pass at the end. Without it the list grew for the entire run — a wide sweep could exhaust the
    // heap before it ever reached the notification step. Well above what a normal run produces, so
    // in practice this only ever engages on a pathological job.
    private static final int MAX_PAPERS_RETAINED_FOR_NOTIFICATION = 20_000;

    private void collectNewPapers(List<Paper> retained, Set<Integer> touchedTopicIds, List<Paper> batch) {
        if (batch.isEmpty()) {
            return;
        }
        for (Paper paper : batch) {
            if (paper.getTopics() != null) {
                for (Topic topic : paper.getTopics()) {
                    touchedTopicIds.add(topic.getTopicId());
                }
            }
        }
        synchronized (retained) {
            int room = MAX_PAPERS_RETAINED_FOR_NOTIFICATION - retained.size();
            if (room <= 0) {
                return;
            }
            if (batch.size() <= room) {
                retained.addAll(batch);
            } else {
                retained.addAll(batch.subList(0, room));
                log.warn("Reached the {}-paper notification cap; later new papers in this job will not"
                        + " generate per-paper notifications.", MAX_PAPERS_RETAINED_FOR_NOTIFICATION);
            }
        }
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

        // Tracks whether this call is the one that inserted the row, so only a genuinely new id
        // gets tied to the outcome of the current transaction.
        boolean[] insertedHere = {false};

        ID id = finder.get().orElseGet(() -> {
            try {
                ID created = creator.get();
                insertedHere[0] = true;
                return created;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Lost a race with another thread inserting the same unique name/value;
                // the row now exists, so re-fetch it instead of failing this paper.
                return finder.get().orElseThrow(() -> e);
            }
        });

        ID existing = cache.putIfAbsent(cacheKey, id);
        if (existing != null) {
            return existing;
        }

        // The row we just inserted only exists for other threads once this transaction commits.
        // A batch that rolls back mid-way used to leave its ids cached for the rest of the job, so
        // every later batch resolved the key to a row that no longer existed and failed on the
        // foreign key. Drop the entry unless the transaction actually committed.
        if (insertedHere[0]
                && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                // Value-matched removal: never evict an entry a different thread
                                // committed for the same key in the meantime.
                                cache.remove(cacheKey, id);
                            }
                        }
                    });
        }
        return id;
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

    /**
     * Waits before the next attempt, doubling each time with random jitter.
     *
     * <p>A flat delay makes every worker that failed at the same moment retry at the same moment,
     * so the API gets hit by the whole pool in lockstep and the retries themselves keep it
     * overloaded. Doubling backs off a struggling upstream instead of hammering it, and the jitter
     * spreads the workers out so they stop moving as a block.
     */
    private void backoffBeforeRetry(int attemptIndex) {
        long base = Math.min(RETRY_BASE_DELAY_MS * (1L << Math.min(attemptIndex, 4)), RETRY_MAX_DELAY_MS);
        long jitter = java.util.concurrent.ThreadLocalRandom.current().nextLong(base / 2 + 1);
        try {
            Thread.sleep(base / 2 + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static final long RETRY_BASE_DELAY_MS = 1000;
    private static final long RETRY_MAX_DELAY_MS = 16_000;

    // Worker threads insert into the same authors/keywords/journals rows concurrently, so SQL
    // Server periodically picks one transaction as a deadlock victim and rolls it back. That is
    // normal under concurrent writes and the victim is meant to be retried — before this, every
    // deadlock was counted as a failed query and its whole page of papers was dropped.
    private static final int SAVE_DEADLOCK_MAX_ATTEMPTS = 4;

    /**
     * Splits topics into request-sized groups that each stay within one research field.
     *
     * <p>Grouping by field is what makes batching safe: the field stamped on a newly created paper
     * is derived from the topic the request was built around, so mixing fields in one request would
     * mislabel papers. Topics with no field resolve into their own group rather than being dropped.
     */
    private List<List<Topic>> batchTopicsByField(List<Topic> topics, int batchSize) {
        int size = Math.max(1, batchSize);
        java.util.Map<String, List<Topic>> byField = new java.util.LinkedHashMap<>();
        for (Topic topic : topics) {
            if (topic.getOpenalexId() == null) {
                continue;
            }
            TopicField field = topic.getSubfield() != null ? topic.getSubfield().getField() : null;
            String key = field != null && field.getOpenalexId() != null
                    ? field.getOpenalexId()
                    : "no-field:" + topic.getTopicId();
            byField.computeIfAbsent(key, k -> new ArrayList<>()).add(topic);
        }

        List<List<Topic>> batches = new ArrayList<>();
        for (List<Topic> group : byField.values()) {
            for (int start = 0; start < group.size(); start += size) {
                batches.add(new ArrayList<>(group.subList(start, Math.min(start + size, group.size()))));
            }
        }
        return batches;
    }

    private static boolean isLockConflict(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.dao.PessimisticLockingFailureException
                    || t instanceof org.hibernate.exception.LockAcquisitionException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /** Runs a batch save, retrying it when the database rolls it back as a deadlock victim. */
    private boolean saveBatchWithLockRetry(java.util.function.BooleanSupplier save, String label) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= SAVE_DEADLOCK_MAX_ATTEMPTS; attempt++) {
            try {
                return save.getAsBoolean();
            } catch (RuntimeException e) {
                if (!isLockConflict(e)) {
                    throw e;
                }
                lastFailure = e;
                log.warn("Deadlock while saving batch for {} (attempt {}/{}); retrying.",
                        label, attempt, SAVE_DEADLOCK_MAX_ATTEMPTS);
                // Short randomised pause so the retrying threads don't collide again immediately.
                try {
                    Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextLong(100, 400) * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastFailure;
    }

    // Consecutive per-query API failures before a job gives up on the rest of its work. High enough
    // that a few unlucky topics don't end an otherwise healthy run, low enough that a dead upstream
    // is noticed within seconds rather than after grinding through every remaining topic.
    private static final int API_FAILURE_CIRCUIT_THRESHOLD = 10;

    private void tripCircuitIfUpstreamIsDown(
            Long jobId,
            java.util.concurrent.atomic.AtomicInteger consecutiveApiFailures,
            java.util.concurrent.atomic.AtomicBoolean circuitOpen) {

        if (consecutiveApiFailures.incrementAndGet() >= API_FAILURE_CIRCUIT_THRESHOLD
                && circuitOpen.compareAndSet(false, true)) {
            log.error("Sync job {}: {} consecutive API failures — abandoning the remaining queries"
                    + " instead of retrying against an upstream that is not answering.",
                    jobId, API_FAILURE_CIRCUIT_THRESHOLD);
        }
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
                backoffBeforeRetry(i);
            } catch (Exception e) {
                log.warn("API request failed (attempt {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i == maxRetries - 1) {
                    throw new RuntimeException("API request failed after " + maxRetries + " attempts: " + e.getMessage(), e);
                }
                backoffBeforeRetry(i);
            }
        }
        return null;
    }

    // Some OpenAlex/Semantic Scholar records carry garbage-length values in fields that are
    // normally short (e.g. a corporate "author" whose display_name is actually a full sentence),
    // which otherwise crashes the whole batch with a SQL Server truncation error on insert.
    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
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
                // OpenAlex wraps species/gene names in italics etc. (<i>, <b>, <scp>...) — we
                // display titles as plain text everywhere, so strip the markup rather than show
                // literal "<i>...</i>" to the user.
                dto.title = dto.title.replaceAll("<[^>]+>", "").trim();
                if (dto.title.length() > 250) dto.title = dto.title.substring(0, 247) + "...";
                
                String doiUrl = work.path("doi").asText(null);
                dto.doi = doiUrl != null && doiUrl.startsWith("https://doi.org/") ? doiUrl.substring(16) : doiUrl;
                // FIX #2: Don't default to current year — null is better for trend accuracy
                int rawYear = work.path("publication_year").asInt(0);
                dto.year = rawYear > 0 ? rawYear : null;
                dto.citations = work.path("cited_by_count").asInt(0);
                dto.isOpenAccess = work.path("open_access").path("is_oa").asBoolean(false);

                JsonNode abstractNode = work.path("abstract_inverted_index");
                dto.paperAbstract = (!abstractNode.isMissingNode() && abstractNode.isObject()) ? reconstructAbstractFromJson(abstractNode) : "";
                dto.sourceUrl = work.path("id").asText("");
                dto.journalName = truncate(work.path("primary_location").path("source").path("display_name").asText(null), 255);

                JsonNode authorships = work.path("authorships");
                if (authorships.isArray()) {
                    for (JsonNode authorship : authorships) {
                        String authorName = authorship.path("author").path("display_name").asText(null);
                        if (authorName != null && !authorName.isBlank()) dto.authorNames.add(truncate(authorName.trim(), 255));
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
                            dto.keywordNames.add(truncate(conceptName.trim(), 255));
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
                            dto.keywordNames.add(truncate(kwName.trim(), 255));
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
                dto.title = dto.title.replaceAll("<[^>]+>", "").trim();
                if (dto.title.length() > 250) dto.title = dto.title.substring(0, 247) + "...";
                dto.paperAbstract = paperNode.path("abstract").asText("");
                // FIX #2: Don't default to current year — null is better for trend accuracy
                dto.year = year > 0 ? year : null;
                dto.citations = paperNode.path("citationCount").asInt(0);
                dto.isOpenAccess = paperNode.path("isOpenAccess").asBoolean(false);
                dto.doi = paperNode.path("externalIds").path("DOI").asText(null);
                if (dto.doi == null || dto.doi.isBlank()) dto.doi = paperNode.path("externalIds").path("doi").asText(null);
                dto.sourceUrl = "https://www.semanticscholar.org/paper/" + paperNode.path("paperId").asText("");
                dto.journalName = truncate(paperNode.path("journal").path("name").asText(null), 255);
                JsonNode authorsNode = paperNode.path("authors");
                if (authorsNode.isArray()) {
                    for (JsonNode author : authorsNode) {
                        String authorName = author.path("name").asText(null);
                        if (authorName != null && !authorName.isBlank()) dto.authorNames.add(truncate(authorName.trim(), 255));
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
            paper.setIsOpenAccess(dto.isOpenAccess);
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
            paper.setIsOpenAccess(dto.isOpenAccess);
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
