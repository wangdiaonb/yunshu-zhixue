package com.yunshu.zhixue.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务错误码（API 设计附录 B）。
 *
 * <p>5 位数字，首位标识归属：1 通用、2 学员端、3 管理端；HTTP 状态码与业务码一一对应，
 * 不出现「HTTP 200 + 业务失败」。</p>
 */
@Getter
public enum ErrorCode {

    // ===== 通用 1xxxx =====
    PARAM_INVALID(10000, HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(10001, HttpStatus.UNAUTHORIZED, "未认证或凭证失效"),
    FORBIDDEN(10002, HttpStatus.FORBIDDEN, "无权限"),
    NOT_FOUND(10003, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(10004, HttpStatus.CONFLICT, "资源冲突或已被修改，请刷新后重试"),
    RATE_LIMITED(10005, HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),
    INTERNAL_ERROR(10006, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),
    MALFORMED_BODY(10007, HttpStatus.BAD_REQUEST, "请求体格式错误"),
    FILE_INVALID(10008, HttpStatus.BAD_REQUEST, "文件类型或大小不符"),

    // ===== 学员端 2xxxx =====
    CONTACT_OCCUPIED(20001, HttpStatus.CONFLICT, "联系方式已被占用"),
    CAPTCHA_INVALID(20002, HttpStatus.BAD_REQUEST, "验证码错误或已失效"),
    BAD_CREDENTIALS(20003, HttpStatus.BAD_REQUEST, "账号或密码错误"),
    ACCOUNT_LOCKED(20004, HttpStatus.LOCKED, "账号因连续错误被临时锁定"),
    ACCOUNT_DISABLED(20005, HttpStatus.FORBIDDEN, "账号已禁用"),
    SMS_DAILY_LIMIT(20006, HttpStatus.TOO_MANY_REQUESTS, "验证码每日发送已达上限"),
    INTERVIEW_QUOTA_EXHAUSTED(20007, HttpStatus.TOO_MANY_REQUESTS, "今日面试次数已用尽"),
    INTERVIEW_SESSION_NOT_FOUND(20008, HttpStatus.NOT_FOUND, "面试会话不存在或已过期"),
    INSUFFICIENT_QUESTIONS(20009, HttpStatus.CONFLICT, "可出题数量不足"),
    AI_UNAVAILABLE(20010, HttpStatus.SERVICE_UNAVAILABLE, "AI 批改服务暂不可用"),
    WRONG_QUESTION_NOT_FOUND(20011, HttpStatus.NOT_FOUND, "错题目标不存在"),
    OAUTH_UNAVAILABLE(20012, HttpStatus.SERVICE_UNAVAILABLE, "第三方平台未配置或授权服务不可用"),
    OAUTH_ALREADY_BOUND(20013, HttpStatus.CONFLICT, "该第三方账号已绑定其他学员"),
    OAUTH_STATE_INVALID(20014, HttpStatus.BAD_REQUEST, "第三方授权 state 或绑定票据无效"),

    // ===== 管理端 3xxxx =====
    DUPLICATE_NAME(30001, HttpStatus.CONFLICT, "名称、用户名或编码已存在"),
    MOBILE_EXISTS(30002, HttpStatus.CONFLICT, "手机号已存在"),
    ROLE_IN_USE(30003, HttpStatus.CONFLICT, "角色下有用户，不可删除"),
    BUILTIN_IMMUTABLE(30004, HttpStatus.CONFLICT, "内置账号或超管角色不可操作"),
    IMPORT_VALIDATION_FAILED(30005, HttpStatus.BAD_REQUEST, "导入校验失败"),
    INVALID_STATE_TRANSITION(30006, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    CONTENT_REFERENCED(30007, HttpStatus.CONFLICT, "存在关联内容，不可删除"),
    PERMISSION_SAVE_FAILED(30008, HttpStatus.BAD_REQUEST, "权限保存失败"),
    ADMIN_BAD_CREDENTIALS(30009, HttpStatus.BAD_REQUEST, "后台账号或密码错误"),
    ADMIN_ACCOUNT_LOCKED(30010, HttpStatus.LOCKED, "后台账号临时锁定"),
    ADMIN_ACCOUNT_DISABLED(30011, HttpStatus.FORBIDDEN, "后台账号已禁用");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
