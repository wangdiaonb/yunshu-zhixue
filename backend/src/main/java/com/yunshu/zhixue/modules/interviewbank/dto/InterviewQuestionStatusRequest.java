package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 面试题上下架（A-I15）。
 */
public record InterviewQuestionStatusRequest(

        @NotNull(message = "请选择状态")
        @Min(value = 1, message = "状态不合法")
        @Max(value = 2, message = "状态不合法")
        Integer status
) {
}
