package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增角色（A-L02）。
 */
public record RoleCreateRequest(

        @NotBlank(message = "请输入角色名称")
        @Size(max = 50, message = "角色名称不能超过 50 个字符")
        String name,

        @NotBlank(message = "请输入角色编码")
        @Size(max = 50, message = "角色编码不能超过 50 个字符")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$",
                message = "角色编码需以字母开头，仅包含字母、数字和下划线")
        String code,

        @Size(max = 255, message = "角色描述不能超过 255 个字符")
        String description
) {
}
