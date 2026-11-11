package com.yunshu.zhixue.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端接口的 RBAC 权限点校验（API 设计 §2.8）。
 *
 * <p>权限码与数据库 permission 表一一对应（9 组 34 项）；
 * 传入多个码时按「任一满足」放行。角色管理接口不设权限点，仅超管可访问。</p>
 *
 * <pre>{@code @RequirePermission("question:audit")}</pre>
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 权限码，如 {@code subject:edit}。 */
    String[] value();
}
