package com.ayan.ratelimiter.service;

import com.ayan.ratelimiter.model.ApiKey;
import com.ayan.ratelimiter.model.RequestCount;
import com.ayan.ratelimiter.repository.ApiKeyRepository;
import com.ayan.ratelimiter.repository.RequestCountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private RequestCountRepository requestCountRepository;

    @InjectMocks
    private RateLimiterService service;

    private ApiKey makeKey(int limit, boolean active) {
        ApiKey key = new ApiKey();
        key.setApiKey("abc123");
        key.setOwnerName("Test");
        key.setRequestLimit(limit);
        key.setActive(active);
        return key;
    }

    @Test
    void unknownKeyIsInvalid() {
        when(apiKeyRepository.findByApiKey("nope")).thenReturn(Optional.empty());

        RateLimitResult result = service.check("nope");

        assertEquals(RateLimitResult.Status.INVALID_KEY, result.status());
    }

    @Test
    void inactiveKeyIsInvalid() {
        when(apiKeyRepository.findByApiKey("abc123")).thenReturn(Optional.of(makeKey(3, false)));

        RateLimitResult result = service.check("abc123");

        assertEquals(RateLimitResult.Status.INVALID_KEY, result.status());
    }

    @Test
    void firstRequestIsAllowedAndSaved() {
        when(apiKeyRepository.findByApiKey("abc123")).thenReturn(Optional.of(makeKey(3, true)));
        when(requestCountRepository.findByKeyIdAndWindowStart(any(), any())).thenReturn(Optional.empty());

        RateLimitResult result = service.check("abc123");

        assertEquals(RateLimitResult.Status.ALLOWED, result.status());
        assertEquals(3, result.limit());
        assertEquals(2, result.remaining());
        verify(requestCountRepository).save(any(RequestCount.class));
    }

    @Test
    void remainingGoesDownWithEachRequest() {
        RequestCount existing = new RequestCount();
        existing.setRequestCount(1);
        when(apiKeyRepository.findByApiKey("abc123")).thenReturn(Optional.of(makeKey(3, true)));
        when(requestCountRepository.findByKeyIdAndWindowStart(any(), any())).thenReturn(Optional.of(existing));

        RateLimitResult result = service.check("abc123");

        assertEquals(RateLimitResult.Status.ALLOWED, result.status());
        assertEquals(1, result.remaining());
    }

    @Test
    void requestOverTheLimitIsBlocked() {
        RequestCount existing = new RequestCount();
        existing.setRequestCount(3);
        when(apiKeyRepository.findByApiKey("abc123")).thenReturn(Optional.of(makeKey(3, true)));
        when(requestCountRepository.findByKeyIdAndWindowStart(any(), any())).thenReturn(Optional.of(existing));

        RateLimitResult result = service.check("abc123");

        assertEquals(RateLimitResult.Status.LIMIT_EXCEEDED, result.status());
        assertEquals(0, result.remaining());
        verify(requestCountRepository, never()).save(any());
    }
}