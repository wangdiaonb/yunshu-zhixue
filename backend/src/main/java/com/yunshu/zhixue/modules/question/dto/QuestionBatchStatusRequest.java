package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量上架/下架（A-Q10）。
 */
public record QuestionBatchStatusRequest(

        @NotEmpty(message = "请选择题目")
        @Size(max = 200, message = "单次最多操作 200 道题")
        List<Long> ids,

        @NotBlank(message = "请选择操作")
        @Pattern(regexp = "on|off", message = "操作参数不合法")
        String action
) {
}
