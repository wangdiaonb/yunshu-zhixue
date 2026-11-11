package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量启用/禁用后台用户（A-M04）。
 */
public record BatchUserStatusRequest(

        @NotEmpty(message = "请选择用户")
        @Size(max = 100, message = "单次最多操作 100 个用户")
        List<Long> ids,

        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态不合法")
        @Max(value = 1, message = "状态不合法")
        Integer status
) {
}
