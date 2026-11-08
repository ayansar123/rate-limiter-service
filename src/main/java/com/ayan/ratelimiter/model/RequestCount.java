package com.ayan.ratelimiter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_counts")
public class RequestCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "key_id", nullable = false)
    private Integer keyId;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount;

    public RequestCount() {}

    public Integer getId() { return id; }
    public Integer getKeyId() { return keyId; }
    public void setKeyId(Integer keyId) { this.keyId = keyId; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }
    public Integer getRequestCount() { return requestCount; }
    public void setRequestCount(Integer requestCount) { this.requestCount = requestCount; }
}