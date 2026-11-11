package com.yunshu.zhixue.modules.auth.support;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.security.TokenAudience;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 登录失败锁定（D18）：连续 5 次密码错误锁定 5 分钟。
 *
 * <p>Redis 键按端隔离：{@code login:fail:{aud}/{account}}、{@code login:lock:{aud}/{account}}，
 * 避免同一账号名在学员端与管理端相互影响；账号统一小写并去空格。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptGuard {

    private static final String FAILURE_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redisTemplate;

    /** 是否处于锁定状态。 */
    public boolean isLocked(TokenAudience audience, String account) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(audience, account)));
    }

    /**
     * 记录一次密码错误。
     *
     * @return 当前连续失败次数；达到上限时同时写入锁定键并清空计数
     */
    public int recordFailure(TokenAudience audience, String account) {
        String failureKey = failureKey(audience, account);
        Long count = redisTemplate.opsForValue().increment(failureKey);
        if (count == null) {
            return 0;
        }
        if (count == 1L) {
            redisTemplate.expire(failureKey, AppConstants.LOGIN_LOCK_DURATION);
        }
        if (count >= AppConstants.LOGIN_MAX_FAILURES) {
            redisTemplate.opsForValue().set(lockKey(audience, account), "1", AppConstants.LOGIN_LOCK_DURATION);
            // 锁定到期后重新计数，避免解锁后立即再次被锁
            redisTemplate.delete(failureKey);
        }
        return count.intValue();
    }

    /** 登录成功后清空失败计数与锁定标记。 */
    public void clear(TokenAudience audience, String account) {
        redisTemplate.delete(failureKey(audience, account));
        redisTemplate.delete(lockKey(audience, account));
    }

    private String failureKey(TokenAudience audience, String account) {
        return FAILURE_PREFIX + audience.getValue() + ":" + normalize(account);
    }

    private String lockKey(TokenAudience audience, String account) {
        return LOCK_PREFIX + audience.getValue() + ":" + normalize(account);
    }

    private String normalize(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
    }
}
