package com.yunshu.zhixue.modules.question.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情（A-Q03）。
 */
public record QuestionDetail(
        Long id,
        String content,
        Integer type,
        Integer difficulty,
        Integer status,
        Long stageId,
        String stageName,
        Long subjectId,
        String subjectName,
        String tags,
        String answer,
        String analysis,
        String rejectReason,
        List<QuestionOptionItem> options,
        long doneCount,
        long correctCount,
        double accuracyRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
