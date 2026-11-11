package com.yunshu.zhixue.modules.question.dto;

import java.time.LocalDateTime;

/**
 * 题库列表项（A-Q02）。
 */
public record QuestionItem(
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
        String rejectReason,
        LocalDateTime createdAt,
        long doneCount,
        double accuracyRate
) {
}
