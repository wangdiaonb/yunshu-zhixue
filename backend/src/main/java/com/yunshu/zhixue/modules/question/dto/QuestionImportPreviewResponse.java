package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * 题库 Excel 导入预览（A-Q12）。
 */
public record QuestionImportPreviewResponse(
        String token,
        int total,
        int validCount,
        int invalidCount,
        Long subjectId,
        String subjectName,
        List<String> stagesToCreate,
        List<QuestionImportRow> rows,
        List<QuestionImportErrorRow> errors
) {
}
