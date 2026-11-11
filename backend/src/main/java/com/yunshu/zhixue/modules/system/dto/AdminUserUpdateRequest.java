package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 编辑后台用户（A-M03）。
 *
 * @param password 新密码；留空表示不修改
 */
public record AdminUserUpdateRequest(

        @NotBlank(message = "请输入用户名")
        @Size(max = 50, message = "用户名长度不能超过 50 个字符")
        String username,

        /** 内置超管允许为空，常规用户由服务端强制校验 */
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
        String email,

        @NotNull(message = "请选择角色")
        Long roleId,

        /** 留空或 null 表示不修改密码，非空时由服务端校验 6~20 位 */
        String password,

        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态不合法")
        @Max(value = 1, message = "状态不合法")
        Integer status
) {
}
