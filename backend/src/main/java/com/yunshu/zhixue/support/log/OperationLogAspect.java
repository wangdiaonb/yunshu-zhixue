package com.yunshu.zhixue.support.log;

import com.yunshu.zhixue.common.util.IpUtils;
import com.yunshu.zhixue.security.LoginUser;
import com.yunshu.zhixue.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面：方法成功返回后异步记录，不改变业务返回值。
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final ObjectProvider<OperationLogRecorder> recorderProvider;

    public OperationLogAspect(ObjectProvider<OperationLogRecorder> recorderProvider) {
        this.recorderProvider = recorderProvider;
    }

    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Object result = joinPoint.proceed();

        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            // 登录等无登录态接口由业务代码显式记录
            return result;
        }

        try {
            OperationLogRecorder recorder = recorderProvider.getIfAvailable();
            if (recorder != null) {
                String targetName = resolveTargetName(operationLog.targetName(), joinPoint);
                recorder.record(new OperationLogCommand(
                        loginUser.id(),
                        loginUser.account(),
                        operationLog.action().getLabel(),
                        operationLog.targetType(),
                        targetName,
                        currentIp()));
            }
        } catch (Exception ex) {
            log.warn("操作日志记录失败（不影响业务）：{}", ex.getMessage());
        }
        return result;
    }

    /**
     * 支持两种写法：静态文本，或以 {@code #} 开头的 SpEL（如 {@code #request.username}）。
     */
    private String resolveTargetName(String expression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!StringUtils.hasText(expression)) {
            return signature.getMethod().getName();
        }
        if (!expression.startsWith("#")) {
            return expression;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            String value = EXPRESSION_PARSER.parseExpression(expression).getValue(context, String.class);
            return StringUtils.hasText(value) ? value : expression;
        } catch (Exception ex) {
            log.warn("操作日志表达式解析失败：{}", expression);
            return expression;
        }
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return IpUtils.clientIp(request);
        }
        return "unknown";
    }
}
