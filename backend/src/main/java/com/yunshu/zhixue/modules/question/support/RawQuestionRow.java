package com.yunshu.zhixue.modules.question.support;

import java.util.Map;

/**
 * Excel 原始题目行。
 */
public record RawQuestionRow(
        int rowNumber,
        String content,
        String category,
        String type,
        String difficulty,
        Map<String, String> options,
        String correctAnswer,
        String analysis,
        String tags
) {
}
