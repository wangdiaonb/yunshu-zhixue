package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 题库导入确认（A-Q13）。
 */
public record QuestionImportConfirmRequest(
        @NotBlank(message = "缺少导入令牌，请重新预览")
        String token
) {
}
