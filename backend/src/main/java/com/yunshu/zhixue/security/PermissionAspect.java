package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 权限注解切面：校验当前管理端令牌是否拥有所需权限码。
 *
 * <p>未提供 {@link PermissionEvaluator} 实现时一律拒绝，避免出现「鉴权未就绪即放行」。</p>
 */
@Aspect
@Component
public class PermissionAspect {

    private final ObjectProvider<PermissionEvaluator> evaluatorProvider;

    public PermissionAspect(ObjectProvider<PermissionEvaluator> evaluatorProvider) {
        this.evaluatorProvider = evaluatorProvider;
    }

    @Around("@annotation(com.yunshu.zhixue.security.RequirePermission)"
            + " || @within(com.yunshu.zhixue.security.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequirePermission annotation = resolveAnnotation(joinPoint);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        LoginUser loginUser = UserContext.getRequired();
        if (loginUser.audience() != TokenAudience.ADMIN) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }

        PermissionEvaluator evaluator = evaluatorProvider.getIfAvailable();
        if (evaluator == null) {
            throw BizException.of(ErrorCode.FORBIDDEN, "权限服务未就绪");
        }

        String[] codes = annotation.value();
        boolean allowed = codes.length == 0
                || Arrays.stream(codes).anyMatch(code -> evaluator.hasPermission(loginUser.id(), code));
        if (!allowed) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }

    private RequirePermission resolveAnnotation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequirePermission annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequirePermission.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(),
                    RequirePermission.class);
        }
        return annotation;
    }
}
