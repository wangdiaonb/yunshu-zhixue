package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 编辑主题词表（A-I11 / A-I12）。
 */
public record InterviewTopicSaveRequest(

        @NotBlank(message = "请输入主题名称")
        @Size(max = 50, message = "主题名称不能超过 50 个字符")
        String name,

        /** 为空表示全局主题。 */
        Long subjectId,

        Integer sortOrder
) {
}
