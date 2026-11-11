package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 题目上架/下架（A-Q08）。
 */
public record QuestionStatusRequest(

        @NotBlank(message = "请选择操作")
        @Pattern(regexp = "on|off", message = "操作参数不合法")
        String action
) {
}
