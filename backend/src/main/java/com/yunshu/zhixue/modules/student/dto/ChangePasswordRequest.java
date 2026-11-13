package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码入参（P-M03）。
 */
public record ChangePasswordRequest(

        @NotBlank(message = "请输入原密码")
        String oldPassword,

        @NotBlank(message = "请设置新密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String newPassword
) {
}
