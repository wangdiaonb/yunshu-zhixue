package com.yunshu.zhixue.modules.auth.support;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 短信验证码存储与频控（P-A01 / P-A02 / P-A04 / P-A05）。
 *
 * <p>验证码、冷却与每日次数均存 Redis；验证码 5 分钟有效，同一手机号 60 秒内仅可获取一次，
 * 每日最多 10 次；连续校验失败 5 次后验证码立即失效。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsCodeStore {

    private static final String CODE_PREFIX = "sms:code:";
    private static final String COOLDOWN_PREFIX = "sms:cooldown:";
    private static final String FAILURE_PREFIX = "sms:fail:";
    private static final String DAILY_PREFIX = "sms:daily:";
    private static final Duration CODE_TTL = AppConstants.SMS_CODE_TTL;
    private static final Duration COOLDOWN = AppConstants.SMS_SEND_COOLDOWN;
    private static final int DAILY_LIMIT = AppConstants.SMS_DAILY_LIMIT;
    private static final int MAX_VERIFY_FAILURES = 5;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.sms.log-code:false}")
    private boolean logCode;

    /**
     * 生成并发送验证码。
     *
     * @return 验证码有效期与本次验证码（后者仅在开发环境用于回显）
     */
    public SmsCodeDelivery send(String type, String mobile) {
        String normalizedMobile = normalize(mobile);
        String cooldownKey = COOLDOWN_PREFIX + normalizedMobile;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw BizException.of(ErrorCode.RATE_LIMITED, "验证码发送过于频繁，请稍后再试");
        }

        String dailyKey = dailyKey(normalizedMobile);
        String dailyValue = redisTemplate.opsForValue().get(dailyKey);
        int sentToday = dailyValue == null ? 0 : Integer.parseInt(dailyValue);
        if (sentToday >= DAILY_LIMIT) {
            throw BizException.of(ErrorCode.SMS_DAILY_LIMIT);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(codeKey(type, normalizedMobile), code, CODE_TTL);
        redisTemplate.delete(failureKey(type, normalizedMobile));
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN);

        Long count = redisTemplate.opsForValue().increment(dailyKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(dailyKey, ttlUntilTomorrow());
        }

        if (logCode) {
            log.warn("【开发环境】短信验证码 type={} mobile={} code={}", type, normalizedMobile, code);
        } else {
            log.info("短信验证码已生成 type={} mobile={}", type, maskMobile(normalizedMobile));
        }
        return new SmsCodeDelivery((int) CODE_TTL.toSeconds(), code);
    }

    /** 校验并消费验证码；失败累计 5 次后验证码失效。 */
    public void verify(String type, String mobile, String code) {
        String normalizedMobile = normalize(mobile);
        String key = codeKey(type, normalizedMobile);
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(code)) {
            String failureKey = failureKey(type, normalizedMobile);
            Long failures = redisTemplate.opsForValue().increment(failureKey);
            if (failures != null && failures == 1L) {
                redisTemplate.expire(failureKey, CODE_TTL);
            }
            if (failures != null && failures >= MAX_VERIFY_FAILURES) {
                redisTemplate.delete(key);
                redisTemplate.delete(failureKey);
            }
            throw BizException.of(ErrorCode.CAPTCHA_INVALID);
        }
        redisTemplate.delete(key);
        redisTemplate.delete(failureKey(type, normalizedMobile));
    }

    private String codeKey(String type, String mobile) {
        return CODE_PREFIX + type + ":" + mobile;
    }

    private String failureKey(String type, String mobile) {
        return FAILURE_PREFIX + type + ":" + mobile;
    }

    private String dailyKey(String mobile) {
        return DAILY_PREFIX + mobile + ":" + LocalDate.now(AppConstants.BIZ_ZONE);
    }

    private Duration ttlUntilTomorrow() {
        LocalDateTime nextDay = LocalDate.now(AppConstants.BIZ_ZONE).plusDays(1).atStartOfDay();
        Duration ttl = Duration.between(LocalDateTime.now(AppConstants.BIZ_ZONE), nextDay);
        return ttl.isNegative() || ttl.isZero() ? Duration.ofDays(1) : ttl;
    }

    private String normalize(String mobile) {
        return mobile == null ? "" : mobile.trim().toLowerCase(Locale.ROOT);
    }

    private String maskMobile(String mobile) {
        return mobile.length() == 11 ? mobile.substring(0, 3) + "****" + mobile.substring(7) : mobile;
    }
}
