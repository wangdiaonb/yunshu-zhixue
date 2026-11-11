package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 编辑面试题（A-I04）：题干与参考答案只读，仅主题与难度可维护。
 */
public record InterviewQuestionUpdateRequest(

        @Size(max = 50, message = "主题不能超过 50 个字符")
        String topic,

        @NotNull(message = "请选择难度")
        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty
) {
}
