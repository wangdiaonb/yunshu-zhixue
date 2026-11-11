package com.yunshu.zhixue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 契约配置（D21）。
 *
 * <p>两个分组分别对应学员端与管理端，`/v3/api-docs/portal`、`/v3/api-docs/admin`
 * 供两端用 openapi-typescript 生成类型；后端永不手工维护第二份类型定义。</p>
 */
@Configuration
public class OpenApiConfig {

    public static final String PORTAL_SCHEME = "portalToken";
    public static final String ADMIN_SCHEME = "adminToken";

    @Bean
    public OpenAPI yunshuOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("云枢智学 API")
                        .description("AI 赋能的智能面试训练平台 · 学员端与管理端接口契约")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(PORTAL_SCHEME, bearerScheme("学员端访问令牌（aud=portal）"))
                        .addSecuritySchemes(ADMIN_SCHEME, bearerScheme("管理端访问令牌（aud=admin）")));
    }

    @Bean
    public GroupedOpenApi portalApiGroup() {
        return GroupedOpenApi.builder()
                .group("portal")
                .pathsToMatch("/api/portal/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    private SecurityScheme bearerScheme(String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(description);
    }
}
