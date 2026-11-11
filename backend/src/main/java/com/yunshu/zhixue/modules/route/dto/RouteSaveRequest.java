package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新建/编辑学习路线（A-R02 / A-R03）。
 */
public record RouteSaveRequest(

        @NotBlank(message = "请输入路线名称")
        @Size(max = 100, message = "路线名称不能超过 100 个字符")
        String name,

        @NotNull(message = "请选择关联学科")
        @Min(value = 1, message = "学科参数不合法")
        Long subjectId,

        @Size(max = 100, message = "适合学历不能超过 100 个字符")
        String targetEducation,

        @Size(max = 50, message = "路线图标不能超过 50 个字符")
        String icon,

        @Size(max = 2000, message = "路线描述不能超过 2000 个字符")
        String description,

        @Min(value = 0, message = "状态参数不合法")
        @Max(value = 1, message = "状态参数不合法")
        Integer status
) {
}
