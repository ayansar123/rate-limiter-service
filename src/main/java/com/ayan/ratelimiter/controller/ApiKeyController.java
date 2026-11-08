package com.ayan.ratelimiter.controller;

import com.ayan.ratelimiter.model.ApiKey;
import com.ayan.ratelimiter.model.RequestCount;
import com.ayan.ratelimiter.repository.ApiKeyRepository;
import com.ayan.ratelimiter.repository.RequestCountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final RequestCountRepository requestCountRepository;

    public ApiKeyController(ApiKeyRepository apiKeyRepository,
                            RequestCountRepository requestCountRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.requestCountRepository = requestCountRepository;
    }

    public record CreateKeyRequest(String ownerName, Integer requestLimit) {}

    @PostMapping("/keys")
    public ResponseEntity<Map<String, Object>> createKey(@RequestBody CreateKeyRequest request) {
        if (request.ownerName() == null || request.ownerName().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "ownerName is required");
        }
        int limit = request.requestLimit() == null ? 5 : request.requestLimit();
        if (limit < 1) {
            return error(HttpStatus.BAD_REQUEST, "requestLimit must be at least 1");
        }

        ApiKey key = new ApiKey();
        key.setApiKey(UUID.randomUUID().toString());
        key.setOwnerName(request.ownerName());
        key.setRequestLimit(limit);
        key.setActive(true);
        apiKeyRepository.save(key);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", key.getId());
        body.put("apiKey", key.getApiKey());
        body.put("ownerName", key.getOwnerName());
        body.put("requestLimit", key.getRequestLimit());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> usage(
            @RequestHeader(value = "X-API-Key", required = false) String key) {
        if (key == null || key.isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "Missing X-API-Key header");
        }
        Optional<ApiKey> found = apiKeyRepository.findByApiKey(key);
        if (found.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }

        ApiKey apiKey = found.get();
        LocalDateTime windowStart = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        int used = requestCountRepository
                .findByKeyIdAndWindowStart(apiKey.getId(), windowStart)
                .map(RequestCount::getRequestCount)
                .orElse(0);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ownerName", apiKey.getOwnerName());
        body.put("limitPerMinute", apiKey.getRequestLimit());
        body.put("usedThisMinute", used);
        body.put("remaining", Math.max(0, apiKey.getRequestLimit() - used));
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/keys/{id}")
    public ResponseEntity<Map<String, Object>> disableKey(@PathVariable Integer id) {
        Optional<ApiKey> found = apiKeyRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Key not found");
        }
        ApiKey key = found.get();
        key.setActive(false);
        apiKeyRepository.save(key);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Key disabled");
        body.put("id", id);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}