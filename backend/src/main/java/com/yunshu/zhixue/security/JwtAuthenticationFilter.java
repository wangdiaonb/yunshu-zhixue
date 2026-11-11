package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：按请求路径判定端，校验访问令牌并写入 SecurityContext。
 *
 * <p>令牌缺失或无效时不做短路，交由 Spring Security 的入口点决定是否返回 401，
 * 以兼容登录、公开题库等匿名接口。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   TokenBlacklistService tokenBlacklistService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        TokenAudience audience = TokenAudience.ofPath(request.getRequestURI());
        String token = resolveToken(request);

        if (audience != null && token != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtTokenService.TokenPayload payload = jwtTokenService.parseAccessToken(token, audience);
                if (!tokenBlacklistService.isBlacklisted(payload.tokenId())) {
                    LoginUser loginUser = new LoginUser(payload.userId(), payload.account(),
                            audience, payload.tokenId(), payload.expiresAt());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (BizException ex) {
                // 令牌无效/过期：保持匿名，由入口点对受保护接口返回 401
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            String token = header.substring(AppConstants.BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
