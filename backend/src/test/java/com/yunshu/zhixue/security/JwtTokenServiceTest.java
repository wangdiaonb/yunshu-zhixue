package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 双端隔离与令牌类型校验。
 */
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.getPortal().setSecret("unit-test-portal-secret-key-0123456789");
        properties.getPortal().setAccessTtl(Duration.ofHours(2));
        properties.getPortal().setRefreshTtl(Duration.ofDays(14));
        properties.getAdmin().setSecret("unit-test-admin-secret-key-0123456789");
        properties.getAdmin().setAccessTtl(Duration.ofHours(2));
        properties.getAdmin().setRefreshTtl(Duration.ofDays(14));
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    void shouldCreateAndParsePortalAccessToken() {
        String token = jwtTokenService.createAccessToken(10L, "13800000000", TokenAudience.PORTAL);

        JwtTokenService.TokenPayload payload = jwtTokenService.parseAccessToken(token, TokenAudience.PORTAL);

        assertThat(payload.userId()).isEqualTo(10L);
        assertThat(payload.account()).isEqualTo("13800000000");
        assertThat(payload.audience()).isEqualTo(TokenAudience.PORTAL);
        assertThat(payload.tokenId()).isNotBlank();
        assertThat(payload.isAccessToken()).isTrue();
    }

    @Test
    void shouldRejectTokenAcrossAudience() {
        String adminToken = jwtTokenService.createAccessToken(1L, "admin", TokenAudience.ADMIN);

        assertThatThrownBy(() -> jwtTokenService.parseAccessToken(adminToken, TokenAudience.PORTAL))
                .isInstanceOf(BizException.class);
    }

    @Test
    void shouldRejectRefreshTokenAsAccessToken() {
        String refreshToken = jwtTokenService.createRefreshToken(10L, "13800000000", TokenAudience.PORTAL);

        assertThatThrownBy(() -> jwtTokenService.parseAccessToken(refreshToken, TokenAudience.PORTAL))
                .isInstanceOf(BizException.class);
    }
}
