package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * Excel 导入的合法题目行；同时用于 Redis 预览暂存。
 */
public record QuestionImportRow(
        int rowNumber,
        String content,
        String stageName,
        int type,
        int difficulty,
        List<QuestionImportOption> options,
        String answer,
        String analysis,
        String tags
) {
}
