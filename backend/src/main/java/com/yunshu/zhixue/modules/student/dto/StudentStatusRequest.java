package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 启用 / 禁用学员（A-U02）。
 */
public record StudentStatusRequest(

        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态不合法")
        @Max(value = 1, message = "状态不合法")
        Integer status
) {
}
