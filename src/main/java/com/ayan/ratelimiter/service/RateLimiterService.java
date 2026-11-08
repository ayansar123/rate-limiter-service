package com.ayan.ratelimiter.service;

import com.ayan.ratelimiter.model.ApiKey;
import com.ayan.ratelimiter.model.RequestCount;
import com.ayan.ratelimiter.repository.ApiKeyRepository;
import com.ayan.ratelimiter.repository.RequestCountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RateLimiterService {

    private final ApiKeyRepository apiKeyRepository;
    private final RequestCountRepository requestCountRepository;

    public RateLimiterService(ApiKeyRepository apiKeyRepository,
                              RequestCountRepository requestCountRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.requestCountRepository = requestCountRepository;
    }

    @Transactional
    public RateLimitResult check(String key) {
        Optional<ApiKey> found = apiKeyRepository.findByApiKey(key);
        if (found.isEmpty() || !Boolean.TRUE.equals(found.get().getActive())) {
            return new RateLimitResult(RateLimitResult.Status.INVALID_KEY, 0, 0, 0);
        }

        ApiKey apiKey = found.get();
        int limit = apiKey.getRequestLimit();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.truncatedTo(ChronoUnit.MINUTES);
        long retryAfter = Math.max(1, ChronoUnit.SECONDS.between(now, windowStart.plusMinutes(1)));

        RequestCount counter = requestCountRepository
                .findByKeyIdAndWindowStart(apiKey.getId(), windowStart)
                .orElseGet(() -> {
                    RequestCount c = new RequestCount();
                    c.setKeyId(apiKey.getId());
                    c.setWindowStart(windowStart);
                    c.setRequestCount(0);
                    return c;
                });

        if (counter.getRequestCount() >= limit) {
            return new RateLimitResult(RateLimitResult.Status.LIMIT_EXCEEDED, limit, 0, retryAfter);
        }

        counter.setRequestCount(counter.getRequestCount() + 1);
        requestCountRepository.save(counter);

        return new RateLimitResult(RateLimitResult.Status.ALLOWED, limit,
                limit - counter.getRequestCount(), retryAfter);
    }
}