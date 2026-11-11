package com.yunshu.zhixue;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 云枢智学后端启动类。
 *
 * <p>架构形态为模块化单体：业务代码统一落在 {@code modules/<领域>} 下，
 * 模块间单向依赖、不跨模块直连数据库（见 docs/design/02.整体架构设计.md §3.2）。</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.yunshu.zhixue.modules", annotationClass = Mapper.class)
public class YunshuZhixueApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunshuZhixueApplication.class, args);
    }
}
