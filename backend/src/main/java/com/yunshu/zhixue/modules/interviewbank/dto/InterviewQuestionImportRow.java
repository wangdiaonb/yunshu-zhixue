package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * Excel 导入校验通过的行。
 */
public record InterviewQuestionImportRow(
        int rowNumber,
        Long subjectId,
        String subjectName,
        String content,
        String topic,
        Integer difficulty,
        String tags,
        String referenceAnswer
) {
}
