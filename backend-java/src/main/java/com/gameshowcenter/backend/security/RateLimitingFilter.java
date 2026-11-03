package com.gameshowcenter.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance Sliding-Window Rate Limiting Filter to mitigate Denial-of-Service (DoS),
 * credential/promo brute force, and API resource exhaustion attacks.
 * Adheres to SOC 2 CC6.6 & ISO/IEC 27001:2022 A.8.20 / A.8.26.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Window size: 1 minute (60,000 ms)
    private static final long WINDOW_MS = 60_000L;

    // Rate limits per route category (requests allowed per IP per window)
    private static final int LIMIT_PAYMENT = 10;   // Prevents brute-forcing promo codes or spamming payment
    private static final int LIMIT_DOWNLOAD = 5;   // Prevents bulk downloading and bandwidth exhaustion
    private static final int LIMIT_MATCH = 15;     // Prevents compiler CPU exhaustion
    private static final int LIMIT_DEFAULT = 60;   // Baseline protection for any other API route

    private static class IpRequestBucket {
        long windowStart;
        AtomicInteger count;

        IpRequestBucket(long now) {
            this.windowStart = now;
            this.count = new AtomicInteger(1);
        }
    }

    // IP + Route Category -> Bucket
    private final Map<String, IpRequestBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Allow CORS pre-flight OPTIONS requests without consuming rate limit
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Only rate-limit API endpoints
        if (path == null || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        int maxAllowed = resolveLimitForPath(path);
        String category = resolveCategoryForPath(path);
        String bucketKey = clientIp + ":" + category;

        long now = System.currentTimeMillis();
        cleanStaleBuckets(now);

        IpRequestBucket bucket = buckets.compute(bucketKey, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > WINDOW_MS) {
                return new IpRequestBucket(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = bucket.count.get();
        if (currentCount > maxAllowed) {
            long remainingSeconds = Math.max(1, (WINDOW_MS - (now - bucket.windowStart)) / 1000);
            log.warn("Rate limit exceeded for client IP [{}] on path [{}] (Category: {}, Request {}/{})",
                    clientIp, path, category, currentCount, maxAllowed);

            response.setStatus(429); // 429 Too Many Requests
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(remainingSeconds));
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxAllowed));
            response.setHeader("X-RateLimit-Remaining", "0");

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", 429);
            errorBody.put("error", "Too Many Requests");
            errorBody.put("message", "Too many requests. You have exceeded the permitted security rate limit for this service.");
            errorBody.put("retryAfterSeconds", remainingSeconds);

            response.getWriter().write(MAPPER.writeValueAsString(errorBody));
            response.getWriter().flush();
            return;
        }

        // Set telemetry headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxAllowed));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxAllowed - currentCount)));

        filterChain.doFilter(request, response);
    }

    private int resolveLimitForPath(String path) {
        if (path.startsWith("/api/payment")) return LIMIT_PAYMENT;
        if (path.startsWith("/api/download")) return LIMIT_DOWNLOAD;
        if (path.startsWith("/api/match")) return LIMIT_MATCH;
        return LIMIT_DEFAULT;
    }

    private String resolveCategoryForPath(String path) {
        if (path.startsWith("/api/payment")) return "PAYMENT";
        if (path.startsWith("/api/download")) return "DOWNLOAD";
        if (path.startsWith("/api/match")) return "MATCH";
        return "GENERAL";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown-ip";
    }

    private void cleanStaleBuckets(long now) {
        // Evict buckets whose window expired more than 2 windows ago
        if (buckets.size() > 500) {
            buckets.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > (WINDOW_MS * 2));
        }
    }
}
