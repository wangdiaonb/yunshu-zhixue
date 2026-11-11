package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 路线发布/下架（A-R04）。
 */
public record RouteStatusRequest(

        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态参数不合法")
        @Max(value = 1, message = "状态参数不合法")
        Integer status
) {
}
