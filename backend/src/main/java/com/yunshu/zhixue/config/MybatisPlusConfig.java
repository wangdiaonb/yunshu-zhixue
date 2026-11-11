package com.yunshu.zhixue.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MyBatis-Plus 配置：分页、乐观锁、防全表更新、审计时间自动填充。
 *
 * <p>逻辑删除在 {@code application.yml} 的 global-config 中统一开启（D14），
 * 数据库时间统一 UTC（database-rules §3.5）。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        // 错题更新、面试结束落库使用乐观锁（评审 R-01）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防御误写的全表 update/delete
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * created_at / updated_at 自动填充（UTC）。
     *
     * <p>实体字段需标注 {@code @TableField(fill = FieldFill.INSERT)} /
     * {@code FieldFill.INSERT_UPDATE} 才会被填充；无审计字段的表（如 role_permission）不受影响。</p>
     */
    @Bean
    public MetaObjectHandler utcMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now(ZoneOffset.UTC));
            }
        };
    }
}
