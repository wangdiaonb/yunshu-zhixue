package com.yunshu.zhixue.modules.question.dto;

/**
 * 批量状态操作结果。
 */
public record QuestionBatchStatusResponse(int updatedCount, int skippedCount) {
}
