package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.constant.AppConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置：两端独立令牌域 + 无状态 + 注解式 RBAC。
 *
 * <p>D20 同域部署、不开 CORS 白名单；接口路径按 {@code /api/portal/**} 与
 * {@code /api/admin/**} 分区拦截，未归入任何分区的 {@code /api/**} 一律拒绝。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 无需认证的基础设施端点。 */
    private static final String[] PUBLIC_INFRA_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/error",
            "/files/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                        JwtTokenService jwtTokenService,
                                                        TokenBlacklistService tokenBlacklistService,
                                                        RestAuthenticationEntryPoint entryPoint,
                                                        RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.securityMatcher(AppConstants.ADMIN_API_PREFIX + "/**");
        applyCommon(http, jwtTokenService, tokenBlacklistService, entryPoint, accessDeniedHandler);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(AppConstants.ADMIN_API_PREFIX + "/auth/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain portalSecurityFilterChain(HttpSecurity http,
                                                          JwtTokenService jwtTokenService,
                                                          TokenBlacklistService tokenBlacklistService,
                                                          RestAuthenticationEntryPoint entryPoint,
                                                          RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.securityMatcher(AppConstants.PORTAL_API_PREFIX + "/**");
        applyCommon(http, jwtTokenService, tokenBlacklistService, entryPoint, accessDeniedHandler);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 登录/注册/找回密码/第三方登录为公开接口，其余学员端接口需登录
                .requestMatchers(AppConstants.PORTAL_API_PREFIX + "/auth/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http,
                                                         RestAuthenticationEntryPoint entryPoint,
                                                         RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_INFRA_PATHS).permitAll()
                        // 未经两端前缀分区的 /api 接口默认拒绝，避免新增路径漏鉴权
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void applyCommon(HttpSecurity http,
                             JwtTokenService jwtTokenService,
                             TokenBlacklistService tokenBlacklistService,
                             RestAuthenticationEntryPoint entryPoint,
                             RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenService, tokenBlacklistService),
                        UsernamePasswordAuthenticationFilter.class);
    }
}
