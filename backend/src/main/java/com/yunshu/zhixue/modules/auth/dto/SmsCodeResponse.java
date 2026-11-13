package com.yunshu.zhixue.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 发送短信验证码响应（P-A01）。
 *
 * @param expireSeconds 验证码有效期（秒）
 * @param debugCode     开发环境回显用验证码；生产环境（app.sms.log-code=false）不返回
 */
public record SmsCodeResponse(
        int expireSeconds,
        @JsonInclude(JsonInclude.Include.NON_NULL) String debugCode
) {
}
