package com.yunshu.zhixue.support.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志埋点：标注在管理端写操作、登录/登出与导出接口上（B5）。
 *
 * <p>切面在方法成功返回后异步写入；登录接口无登录态时由业务代码显式记录。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作类型。 */
    OperationAction action();

    /** 操作对象类型，如 question / course / student。 */
    String targetType();

    /** 操作对象描述，支持 SpEL，例如 {@code "#id"}；留空时取方法名。 */
    String targetName() default "";
}
