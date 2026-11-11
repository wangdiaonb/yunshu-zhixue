package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * Redis 中的题库导入预览载荷（绑定目标学科）。
 */
public record QuestionImportPayload(Long subjectId, List<QuestionImportRow> rows) {
}
