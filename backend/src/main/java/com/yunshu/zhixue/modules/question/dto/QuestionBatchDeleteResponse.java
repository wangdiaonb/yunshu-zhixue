package com.yunshu.zhixue.modules.question.dto;

/**
 * 批量删除结果。
 */
public record QuestionBatchDeleteResponse(int deletedCount, long refCount) {
}
