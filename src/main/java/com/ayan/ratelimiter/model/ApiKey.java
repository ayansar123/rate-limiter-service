package com.ayan.ratelimiter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "request_limit", nullable = false)
    private Integer requestLimit;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    public ApiKey() {}

    public Integer getId() { return id; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Integer getRequestLimit() { return requestLimit; }
    public void setRequestLimit(Integer requestLimit) { this.requestLimit = requestLimit; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}