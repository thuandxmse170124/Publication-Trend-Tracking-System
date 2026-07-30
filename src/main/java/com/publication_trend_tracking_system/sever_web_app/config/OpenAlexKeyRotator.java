package com.publication_trend_tracking_system.sever_web_app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rotates across multiple free OpenAlex API keys so a single key's $1/day budget doesn't block
 * sync jobs. When a key's budget is exhausted mid-run, callers advance to the next key and retry
 * — see SyncServiceImpl/TopicSeedServiceImpl's fetchFromApi. Index only ever moves forward; it
 * wraps back to the first key on the next app restart (daily budgets reset at midnight UTC
 * anyway, so this is close enough without tracking per-key reset timestamps).
 */
@Component
@Slf4j
public class OpenAlexKeyRotator {

    private final List<String> keys;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public OpenAlexKeyRotator(@Value("${openalex.api-keys:}") String rawKeys) {
        List<String> parsed = new ArrayList<>();
        if (rawKeys != null && !rawKeys.isBlank()) {
            for (String key : rawKeys.split(",")) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    parsed.add(trimmed);
                }
            }
        }
        this.keys = parsed;
        log.info("OpenAlex key rotator configured with {} key(s)", keys.size());
    }

    public int keyCount() {
        return keys.size();
    }

    /** Null when no keys are configured — callers should fall back to unauthenticated requests. */
    public String getCurrentKey() {
        if (keys.isEmpty()) return null;
        return keys.get(currentIndex.get() % keys.size());
    }

    public boolean hasMoreKeys() {
        return currentIndex.get() + 1 < keys.size();
    }

    public void rotateToNextKey() {
        int next = currentIndex.incrementAndGet();
        if (next < keys.size()) {
            log.warn("OpenAlex API key exhausted its daily budget — rotating to key {}/{}", next + 1, keys.size());
        }
    }

    /**
     * Strips the value of any {@code api_key} parameter out of a string before it is logged or
     * stored. OpenAlex only accepts its key as a query parameter, so the key is unavoidably part of
     * every request URL; the job here is to make sure that URL never reaches a log file, an error
     * message, or the sync_jobs.error_message column an admin can read in the browser.
     *
     * <p>Callers should route anything that might embed a request URL through this — most obviously
     * exception messages, which today happen to omit the query string but are not contractually
     * required to.
     */
    public static String redactApiKey(String text) {
        return text == null ? null : text.replaceAll("(?i)(api_key=)[^&\\s\"]+", "$1***");
    }
}
