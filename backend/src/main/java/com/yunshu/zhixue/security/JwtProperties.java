package com.yunshu.zhixue.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置：两端各自一套签名密钥与有效期（accessToken 2h / refreshToken 14d）。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private Token portal = new Token();

    private Token admin = new Token();

    public Token forAudience(TokenAudience audience) {
        return audience == TokenAudience.ADMIN ? admin : portal;
    }

    @Getter
    @Setter
    public static class Token {

        /** HMAC 签名密钥，至少 32 字节；只允许来自环境变量/配置文件。 */
        private String secret;

        /** 访问令牌有效期。 */
        private Duration accessTtl = Duration.ofHours(2);

        /** 刷新令牌有效期（滚动续期）。 */
        private Duration refreshTtl = Duration.ofDays(14);
    }
}
