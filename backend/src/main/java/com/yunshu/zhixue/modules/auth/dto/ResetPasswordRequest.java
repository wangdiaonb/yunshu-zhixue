package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 找回密码·重置密码入参（P-A06）。
 */
public record ResetPasswordRequest(

        @NotBlank(message = "重置令牌不能为空")
        String resetToken,

        @NotBlank(message = "请设置新密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String newPassword
) {
}
