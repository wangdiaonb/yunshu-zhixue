package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * Excel 导入校验失败的行。
 */
public record InterviewQuestionImportErrorRow(
        int rowNumber,
        String content,
        List<String> messages
) {
}
