package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 阶段启用/停用（A-S10）。
 */
public record StageStatusRequest(

        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态不合法")
        @Max(value = 1, message = "状态不合法")
        Integer status
) {
}
