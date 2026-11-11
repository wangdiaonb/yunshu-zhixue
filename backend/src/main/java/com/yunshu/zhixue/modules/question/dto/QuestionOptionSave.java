package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增/编辑题目时的选项。
 */
public record QuestionOptionSave(

        @NotBlank(message = "选项内容不能为空")
        @Size(max = 500, message = "选项内容不能超过 500 个字符")
        String text,

        Boolean correct
) {
}
