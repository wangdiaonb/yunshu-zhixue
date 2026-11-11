package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量操作 ID 入参（批量删除）。
 */
public record BatchIdsRequest(

        @NotEmpty(message = "请选择记录")
        @Size(max = 100, message = "单次最多操作 100 条记录")
        List<Long> ids
) {
}
