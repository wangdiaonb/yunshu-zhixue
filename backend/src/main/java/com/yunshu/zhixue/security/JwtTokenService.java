package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与校验：access / refresh 两类令牌，按端区分密钥与 audience。
 *
 * <p>令牌内只放身份与端标识，不放权限清单（权限由 {@link PermissionEvaluator} 查询）。</p>
 */
@Component
public class JwtTokenService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_ACCOUNT = "account";
    private static final String CLAIM_TYPE = "typ";

    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    /** 签发访问令牌。 */
    public String createAccessToken(Long userId, String account, TokenAudience audience) {
        JwtProperties.Token policy = properties.forAudience(audience);
        return createToken(userId, account, audience, policy.getAccessTtl(), TYPE_ACCESS);
    }

    /** 签发刷新令牌。 */
    public String createRefreshToken(Long userId, String account, TokenAudience audience) {
        JwtProperties.Token policy = properties.forAudience(audience);
        return createToken(userId, account, audience, policy.getRefreshTtl(), TYPE_REFRESH);
    }

    /**
     * 校验并解析访问令牌。
     *
     * @throws BizException 令牌无效/过期/端不匹配（业务码 10001）
     */
    public TokenPayload parseAccessToken(String token, TokenAudience audience) {
        return parse(token, audience, TYPE_ACCESS);
    }

    /**
     * 校验并解析刷新令牌。
     *
     * @throws BizException 令牌无效/过期/端不匹配（业务码 10001）
     */
    public TokenPayload parseRefreshToken(String token, TokenAudience audience) {
        return parse(token, audience, TYPE_REFRESH);
    }

    private String createToken(Long userId, String account, TokenAudience audience,
                               Duration ttl, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_ACCOUNT, account)
                .claim(CLAIM_TYPE, type)
                .audience().add(audience.getValue()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(keyOf(audience), Jwts.SIG.HS256)
                .compact();
    }

    private TokenPayload parse(String token, TokenAudience audience, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyOf(audience))
                    .requireAudience(audience.getValue())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw BizException.of(ErrorCode.UNAUTHORIZED);
            }
            return new TokenPayload(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_ACCOUNT, String.class),
                    audience,
                    claims.getId(),
                    claims.getExpiration().toInstant(),
                    expectedType);
        } catch (BizException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
    }

    private SecretKey keyOf(TokenAudience audience) {
        JwtProperties.Token policy = properties.forAudience(audience);
        if (policy.getSecret() == null || policy.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥未配置或长度不足 32 字节：" + audience.getValue());
        }
        return Keys.hmacShaKeyFor(policy.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 令牌解析结果。
     *
     * @param userId    账号主键
     * @param account   登录账号（学员手机号 / 管理员用户名）
     * @param audience  端标识
     * @param tokenId   jti，用于登出黑名单
     * @param expiresAt 过期时间
     * @param type      access / refresh
     */
    public record TokenPayload(Long userId, String account, TokenAudience audience,
                               String tokenId, Instant expiresAt, String type) {

        public boolean isAccessToken() {
            return TYPE_ACCESS.equals(type);
        }
    }
}
