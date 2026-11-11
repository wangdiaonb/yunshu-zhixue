package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 确认导入（A-I08 两段式②）。
 */
public record InterviewQuestionImportConfirmRequest(

        @NotBlank(message = "导入令牌不能为空")
        String token
) {
}
