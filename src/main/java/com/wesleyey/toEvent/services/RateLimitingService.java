package com.wesleyey.toEvent.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Max cache entries before simple clearing to avoid memory bloat
    private static final int MAX_CACHE_ENTRIES = 10_000;

    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> eventCreateBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> eventGeneralBuckets = new ConcurrentHashMap<>();

    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(5))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(15)
                .refillGreedy(15, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createEventCreateBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(20)
                .refillGreedy(20, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createEventGeneralBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(60)
                .refillGreedy(60, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket getBucket(Map<String, Bucket> map, String key, java.util.function.Supplier<Bucket> supplier) {
        if (map.size() > MAX_CACHE_ENTRIES) {
            map.clear();
        }
        return map.computeIfAbsent(key, k -> supplier.get());
    }

    public ConsumptionProbe tryConsumeRegister(String clientIp) {
        return getBucket(registerBuckets, clientIp, this::createRegisterBucket).tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeLogin(String clientIp) {
        return getBucket(loginBuckets, clientIp, this::createLoginBucket).tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeEventCreate(String userOrIp) {
        return getBucket(eventCreateBuckets, userOrIp, this::createEventCreateBucket).tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeEventGeneral(String userOrIp) {
        return getBucket(eventGeneralBuckets, userOrIp, this::createEventGeneralBucket).tryConsumeAndReturnRemaining(1);
    }

    // For tests or administration
    public void resetAll() {
        registerBuckets.clear();
        loginBuckets.clear();
        eventCreateBuckets.clear();
        eventGeneralBuckets.clear();
    }
}
