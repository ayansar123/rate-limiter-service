package com.ayan.ratelimiter.filter;

import com.ayan.ratelimiter.service.RateLimitResult;
import com.ayan.ratelimiter.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/keys") || path.equals("/api/usage");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String key = request.getHeader("X-API-Key");
        if (key == null || key.isBlank()) {
            writeJson(response, 401, "{\"error\":\"Missing X-API-Key header\"}");
            return;
        }

        RateLimitResult result = rateLimiterService.check(key);

        if (result.status() == RateLimitResult.Status.INVALID_KEY) {
            writeJson(response, 401, "{\"error\":\"Invalid or inactive API key\"}");
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (result.status() == RateLimitResult.Status.LIMIT_EXCEEDED) {
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            writeJson(response, 429, "{\"error\":\"Rate limit exceeded. Try again in "
                    + result.retryAfterSeconds() + " seconds.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(body);
    }
}