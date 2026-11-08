package com.ayan.ratelimiter.repository;

import com.ayan.ratelimiter.model.RequestCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RequestCountRepository extends JpaRepository<RequestCount, Integer> {
    Optional<RequestCount> findByKeyIdAndWindowStart(Integer keyId, LocalDateTime windowStart);
}