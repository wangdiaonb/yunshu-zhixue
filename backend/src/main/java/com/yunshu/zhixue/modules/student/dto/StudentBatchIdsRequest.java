package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量删除学员（A-U05）。
 */
public record StudentBatchIdsRequest(

        @NotEmpty(message = "请选择学员")
        @Size(max = 100, message = "单次最多操作 100 个学员")
        List<Long> ids
) {
}
