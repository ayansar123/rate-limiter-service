package com.ayan.ratelimiter.repository;

import com.ayan.ratelimiter.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {
    Optional<ApiKey> findByApiKey(String apiKey);
}