package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicSeedStatusResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicDomain;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicField;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicSubfield;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicDomainRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicFieldRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicSubfieldRepository;
import com.publication_trend_tracking_system.sever_web_app.service.TopicSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Seeds the official OpenAlex 4-tier topic taxonomy (Domain -> Field -> Subfield -> Topic,
 * 4,516 topics total) so the sync engine can filter papers by canonical topic ID instead of
 * free-text search, covering every scientific discipline rather than only what ad-hoc
 * concept-derived topics happened to capture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicSeedServiceImpl implements TopicSeedService {

    private static final String TOPICS_ENDPOINT = "https://api.openalex.org/topics";
    private static final int PAGE_SIZE = 200;
    private static final long EXPECTED_TOPIC_COUNT = 4516;

    private final TopicRepository topicRepository;
    private final TopicSubfieldRepository topicSubfieldRepository;
    private final TopicFieldRepository topicFieldRepository;
    private final TopicDomainRepository topicDomainRepository;
    private final ApplicationContext applicationContext;
    private final com.publication_trend_tracking_system.sever_web_app.config.OpenAlexKeyRotator openAlexKeyRotator;
    private final com.publication_trend_tracking_system.sever_web_app.config.TopicSeedState seedState;

    private final RestTemplate restTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();

    private long lastApiCallTime = 0;

    @Override
    @Async
    public void seedOfficialTaxonomy() {
        if (!seedState.tryStart()) {
            log.warn("Seed already running — ignoring duplicate start request.");
            return;
        }
        int page = 1;
        int totalUpserted = 0;
        log.info("Starting OpenAlex official topic taxonomy seed...");
        try {
            while (true) {
                if (seedState.isCancelRequested()) {
                    log.warn("Topic taxonomy seed canceled by admin after upserting {} topics.", totalUpserted);
                    break;
                }

                String url = TOPICS_ENDPOINT + "?per-page=" + PAGE_SIZE + "&page=" + page + "&mailto=ptt.sync@university.edu";
                String responseBody = fetchFromApi(url);
                if (responseBody == null || responseBody.isBlank()) {
                    break;
                }

                int[] pageStats = applicationContext.getBean(TopicSeedServiceImpl.class).saveTopicsPage(responseBody);
                totalUpserted += pageStats[1];

                // Pagination must stop based on how many raw results OpenAlex returned for this
                // page, not how many were successfully upserted: a single malformed topic
                // (missing id/name) on a non-final page would otherwise make pageStats[1] < 200
                // and silently truncate every page after it.
                if (pageStats[0] < PAGE_SIZE) {
                    break; // last page reached
                }
                page++;
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            log.info("OpenAlex topic taxonomy seed completed. Upserted {} topics across {} pages.", totalUpserted, page);
        } catch (Exception ex) {
            log.error("Topic taxonomy seed failed after upserting {} topics", totalUpserted, ex);
        } finally {
            seedState.finish();
        }
    }

    @Override
    public void cancelSeed() {
        seedState.requestCancel();
    }

    @Override
    public TopicSeedStatusResponse getSeedStatus() {
        return TopicSeedStatusResponse.builder()
                .officialTopicCount(topicRepository.countByOpenalexIdIsNotNull())
                .expectedTopicCount(EXPECTED_TOPIC_COUNT)
                .domainCount(topicDomainRepository.count())
                .fieldCount(topicFieldRepository.count())
                .subfieldCount(topicSubfieldRepository.count())
                .running(seedState.isRunning())
                .build();
    }

    // [0] = raw result count returned by OpenAlex for this page (pagination-continue signal),
    // [1] = count actually upserted (for the completion log only).
    @Transactional
    public int[] saveTopicsPage(String responseBody) {
        ObjectMapper mapper = new ObjectMapper();
        int[] stats = new int[2];
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode results = root.path("results");
            if (results.isArray()) {
                stats[0] = results.size();
                for (JsonNode topicNode : results) {
                    if (upsertTopic(topicNode)) {
                        stats[1]++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse/save a page of OpenAlex topics", e);
            throw new RuntimeException("Failed to save topic taxonomy page", e);
        }
        return stats;
    }

    private boolean upsertTopic(JsonNode topicNode) {
        String topicOpenalexId = extractShortId(topicNode.path("id").asText(null));
        String displayName = topicNode.path("display_name").asText(null);
        if (topicOpenalexId == null || displayName == null || displayName.isBlank()) {
            return false;
        }

        TopicDomain domain = upsertDomain(topicNode.path("domain"));
        TopicField field = upsertField(topicNode.path("field"), domain);
        TopicSubfield subfield = upsertSubfield(topicNode.path("subfield"), field);

        String description = topicNode.path("description").asText(null);

        // Match by openalexId first (idempotent re-seed), then fall back to a name match
        // restricted to still-unlinked (openalexId IS NULL) rows only, so we upgrade a
        // pre-existing legacy topic in place instead of hitting the topic_name unique
        // constraint — without letting two distinct official topics whose names differ only by
        // case steal each other's row (see repository method javadoc).
        Topic topic = topicRepository.findByOpenalexId(topicOpenalexId)
                .or(() -> topicRepository.findByTopicNameIgnoreCaseAndOpenalexIdIsNull(displayName))
                .orElseGet(Topic::new);

        topic.setOpenalexId(topicOpenalexId);
        topic.setTopicName(displayName);
        topic.setDescription(description);
        topic.setSubfield(subfield);
        topicRepository.save(topic);
        return true;
    }

    private TopicDomain upsertDomain(JsonNode node) {
        String id = extractShortId(node.path("id").asText(null));
        if (id == null) return null;
        return topicDomainRepository.findByOpenalexId(id)
                .orElseGet(() -> topicDomainRepository.save(TopicDomain.builder()
                        .openalexId(id)
                        .displayName(node.path("display_name").asText(id))
                        .build()));
    }

    private TopicField upsertField(JsonNode node, TopicDomain domain) {
        String id = extractShortId(node.path("id").asText(null));
        if (id == null || domain == null) return null;
        return topicFieldRepository.findByOpenalexId(id)
                .orElseGet(() -> topicFieldRepository.save(TopicField.builder()
                        .openalexId(id)
                        .displayName(node.path("display_name").asText(id))
                        .domain(domain)
                        .build()));
    }

    private TopicSubfield upsertSubfield(JsonNode node, TopicField field) {
        String id = extractShortId(node.path("id").asText(null));
        if (id == null || field == null) return null;
        return topicSubfieldRepository.findByOpenalexId(id)
                .orElseGet(() -> topicSubfieldRepository.save(TopicSubfield.builder()
                        .openalexId(id)
                        .displayName(node.path("display_name").asText(id))
                        .field(field)
                        .build()));
    }

    private String extractShortId(String fullUri) {
        if (fullUri == null || fullUri.isBlank()) return null;
        int idx = fullUri.lastIndexOf('/');
        return idx >= 0 ? fullUri.substring(idx + 1) : fullUri;
    }

    private synchronized void rateLimitApi() {
        long now = System.currentTimeMillis();
        long diff = now - lastApiCallTime;
        if (diff < 200) { // Max 5 requests per second, matches SyncServiceImpl's OpenAlex pacing
            try {
                Thread.sleep(200 - diff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastApiCallTime = System.currentTimeMillis();
    }

    private String fetchFromApi(String url) {
        // Extra headroom so there's an attempt left after rotating through every configured key
        int maxRetries = Math.max(3, 1 + openAlexKeyRotator.keyCount());
        for (int i = 0; i < maxRetries; i++) {
            String requestUrl = url;
            String key = openAlexKeyRotator.getCurrentKey();
            if (key != null) {
                requestUrl += "&api_key=" + key;
            }
            try {
                rateLimitApi();
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);
                return response.getBody();
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                String responseText = e.getResponseBodyAsString();
                log.error("OpenAlex rate limit exceeded (429) on attempt {}: {}", i + 1, responseText != null && !responseText.isBlank() ? responseText : e.getMessage());
                if (responseText != null && responseText.contains("Insufficient budget")) {
                    if (openAlexKeyRotator.hasMoreKeys()) {
                        openAlexKeyRotator.rotateToNextKey();
                        continue;
                    }
                    throw new RuntimeException("OpenAlex daily API quota/budget exceeded on all configured keys. Resets at midnight UTC.", e);
                }
                if (i == maxRetries - 1) {
                    throw new RuntimeException("API Rate Limit Exceeded (429) after " + maxRetries + " attempts.", e);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                log.warn("OpenAlex topics request failed (attempt {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i == maxRetries - 1) {
                    throw new RuntimeException("OpenAlex topics request failed after " + maxRetries + " attempts: " + e.getMessage(), e);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        return null;
    }
}
