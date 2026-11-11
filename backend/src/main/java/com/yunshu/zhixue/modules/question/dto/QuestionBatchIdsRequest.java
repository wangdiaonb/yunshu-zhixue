package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量题目 ID 入参（A-Q11）。
 */
public record QuestionBatchIdsRequest(

        @NotEmpty(message = "请选择题目")
        @Size(max = 200, message = "单次最多操作 200 道题")
        List<Long> ids
) {
}
