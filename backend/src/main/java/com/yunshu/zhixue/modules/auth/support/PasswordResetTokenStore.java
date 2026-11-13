package com.yunshu.zhixue.modules.auth.support;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * 找回密码一次性令牌（P-A05 / P-A06）：Redis TTL 10 分钟，使用后立即失效。
 */
@Component
@RequiredArgsConstructor
public class PasswordResetTokenStore {

    private static final String PREFIX = "reset:token:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public String issue(Long studentId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(PREFIX + token, String.valueOf(studentId), TTL);
        return token;
    }

    /** 消费一次性令牌并返回学员主键；无效或已使用时返回 10004。 */
    public Long consume(String token) {
        if (!StringUtils.hasText(token)) {
            throw BizException.of(ErrorCode.CONFLICT, "重置令牌无效或已过期，请重新验证");
        }
        String key = PREFIX + token.trim();
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw BizException.of(ErrorCode.CONFLICT, "重置令牌无效或已过期，请重新验证");
        }
        redisTemplate.delete(key);
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw BizException.of(ErrorCode.CONFLICT, "重置令牌无效或已过期，请重新验证");
        }
    }
}
