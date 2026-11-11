package com.yunshu.zhixue.modules.auth.support;

import com.yunshu.zhixue.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 刷新令牌登记表：记录有效 refreshToken 的 jti，支持轮换与登出即时失效（API §2.2）。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String PREFIX = "token:refresh:";

    private final StringRedisTemplate redisTemplate;

    /** 登录 / 刷新成功后登记新的 refreshToken。 */
    public void store(JwtTokenService.TokenPayload payload) {
        Duration ttl = Duration.between(Instant.now(), payload.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(key(payload), "1", ttl);
    }

    /**
     * 原子消费：删除成功表示令牌首次使用，可用于轮换；重复使用返回 false。
     */
    public boolean consume(JwtTokenService.TokenPayload payload) {
        return Boolean.TRUE.equals(redisTemplate.delete(key(payload)));
    }

    /** 登出时撤销刷新令牌；令牌不存在时幂等返回。 */
    public void revoke(JwtTokenService.TokenPayload payload) {
        redisTemplate.delete(key(payload));
    }

    private String key(JwtTokenService.TokenPayload payload) {
        return PREFIX + payload.audience().getValue() + ":" + payload.userId() + ":" + payload.tokenId();
    }
}
