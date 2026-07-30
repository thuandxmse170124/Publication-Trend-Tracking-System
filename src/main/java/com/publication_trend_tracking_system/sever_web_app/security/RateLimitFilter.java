package com.publication_trend_tracking_system.sever_web_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window request cap, applied per caller and per class of endpoint.
 *
 * <p>Three tiers, because the endpoints differ by orders of magnitude in what a request costs:
 * <ul>
 *   <li><b>auth</b> — login, register and password reset are the brute-force surface. The cap here
 *       is what stops an attacker working through a password list; it is deliberately the tightest.
 *   <li><b>sync</b> — one trigger starts a job that sweeps 4,500 topics. Even an admin holding the
 *       button should not be able to queue these back to back.
 *   <li><b>default</b> — ordinary reads. Loose enough that no honest user, and no page that fires
 *       ten parallel calls on load, will ever reach it.
 * </ul>
 *
 * <p>Counters live in memory, which is the right trade for a single-instance deployment: no extra
 * dependency, no network hop on the hot path. Running more than one instance would need a shared
 * store (Redis) — until then each instance enforces its own quota.
 *
 * <p>Keyed by authenticated user when a JWT is present and by client IP otherwise. That matters:
 * keying only by IP would let one shared campus address exhaust everyone's quota, and keying only
 * by user would leave the login endpoint — where there is no user yet — unprotected.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    /** Above this many tracked keys the map is cleared wholesale rather than allowed to grow. */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.auth-per-minute:10}")
    private int authPerMinute;

    @Value("${app.rate-limit.sync-per-minute:5}")
    private int syncPerMinute;

    @Value("${app.rate-limit.default-per-minute:120}")
    private int defaultPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        int limit = limitFor(path);
        String key = tier(path) + "|" + callerId(request);

        Window window = counters.compute(key, (k, existing) ->
                existing == null || existing.isExpired() ? new Window() : existing);

        int used = window.hits.incrementAndGet();
        if (used > limit) {
            long retryAfter = Math.max(1, window.secondsRemaining());
            log.warn("Rate limit hit: key={} path={} used={}/{}", key, path, used, limit);
            reject(response, retryAfter, limit);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));

        if (counters.size() > MAX_TRACKED_KEYS) {
            // Unbounded growth would be a memory leak an attacker could drive by rotating IPs.
            // Dropping every counter is safe: the worst case is that some callers get a fresh
            // window early, which is far better than the process running out of heap.
            log.warn("Rate limiter tracking {} keys — resetting all windows", counters.size());
            counters.clear();
        }

        chain.doFilter(request, response);
    }

    /** Swagger and the payment webhook are excluded: the first is documentation, the second is
     *  called by PayOS on their schedule, and throttling it would drop payment confirmations. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/api/payment/webhook")
                || path.startsWith("/api/payos");
    }

    private String tier(String path) {
        if (path.startsWith("/auth/")) return "auth";
        if (path.startsWith("/api/admin/sync/trigger")
                || path.startsWith("/api/admin/sync/seed-topics")) return "sync";
        return "default";
    }

    private int limitFor(String path) {
        return switch (tier(path)) {
            case "auth" -> authPerMinute;
            case "sync" -> syncPerMinute;
            default -> defaultPerMinute;
        };
    }

    /** Authenticated caller if we can see one, otherwise the client address. */
    private String callerId(HttpServletRequest request) {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Left-most entry is the original client. Only trust this behind a proxy that sets it;
            // exposed directly to the internet it is caller-controlled and trivially spoofed.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, long retryAfter, int limit) throws IOException {
        response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.getWriter().write(
                "{\"code\":429,\"message\":\"Too many requests. Try again in "
                        + retryAfter + " seconds.\",\"result\":null}");
    }

    /** One caller's counter for one window. */
    private static final class Window {
        private final Instant startedAt = Instant.now();
        private final AtomicInteger hits = new AtomicInteger();

        boolean isExpired() {
            return Instant.now().isAfter(startedAt.plus(WINDOW));
        }

        long secondsRemaining() {
            return Duration.between(Instant.now(), startedAt.plus(WINDOW)).toSeconds();
        }
    }
}
