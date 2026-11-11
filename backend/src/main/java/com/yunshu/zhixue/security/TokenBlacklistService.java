package com.yunshu.zhixue.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 令牌黑名单：登出、禁用、改密后立即失效（Redis，TTL 至令牌过期）。
 */
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String tokenId, Duration ttl) {
        if (tokenId == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", ttl);
    }

    public void blacklist(String tokenId, Instant expiresAt) {
        if (expiresAt == null) {
            return;
        }
        blacklist(tokenId, Duration.between(Instant.now(), expiresAt));
    }

    public boolean isBlacklisted(String tokenId) {
        return tokenId != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
    }
}
