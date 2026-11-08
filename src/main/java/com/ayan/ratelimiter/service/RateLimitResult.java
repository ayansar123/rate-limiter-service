package com.ayan.ratelimiter.service;

public record RateLimitResult(Status status, int limit, int remaining, long retryAfterSeconds) {

    public enum Status { ALLOWED, LIMIT_EXCEEDED, INVALID_KEY }
}