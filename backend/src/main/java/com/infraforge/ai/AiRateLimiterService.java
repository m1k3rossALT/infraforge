package com.infraforge.ai;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limiter for AI suggest requests using Bucket4j token buckets.
 *
 * Each authenticated user gets their own bucket keyed by userId.
 * Buckets are created lazily on first request and live in-memory.
 *
 * Default: 10 requests per user per minute (configurable via application.yml).
 *
 * Scalability note: in-memory buckets work correctly for a single backend instance.
 * If horizontal scaling is added (Phase 5d Kubernetes), replace the ConcurrentHashMap
 * with a distributed store (Redis via bucket4j-redis) to enforce limits across instances.
 */
@Service
public class AiRateLimiterService {

    @Value("${infraforge.ai.rate-limit-per-minute:10}")
    private int rateLimit;

    private final ConcurrentHashMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempt to consume one token from the user's bucket.
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean tryConsume(UUID userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, this::newBucket);
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(UUID userId) {
        Bandwidth limit = Bandwidth.classic(
            rateLimit,
            Refill.greedy(rateLimit, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}